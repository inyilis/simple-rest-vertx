package com.example.my_api_vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.jwt.authorization.JWTAuthorization;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.templates.SqlTemplate;
import io.vertx.sqlclient.templates.TupleMapper;

import java.util.*;

public class MainVerticle extends AbstractVerticle {

  private JWTAuth jwt;
  private JWTAuthorization authzProvider;
  private PermissionBasedAuthorization adminAuth, userAuth;

  private JDBCPool client;
  private SqlTemplate<Map<String, Object>, RowSet<JsonObject>> loginUserTmpl;
  private SqlTemplate<Map<String, Object>, RowSet<JsonObject>> getUserTmpl;
  private SqlTemplate<Map<String, Object>, RowSet<JsonObject>> delUserTmpl;
  private SqlTemplate<JsonObject, SqlResult<Void>> addUserTmpl;
  private SqlTemplate<JsonObject, SqlResult<Void>> editUserTmpl;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    setUpInitialData();

    // Create a JWT Auth Provider
    jwt = JWTAuth.create(vertx, new JWTAuthOptions()
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("HS256")
        .setBuffer("keyboard cat")));
    authzProvider = JWTAuthorization.create("permissions");
    adminAuth = PermissionBasedAuthorization.create("admin");
    userAuth = PermissionBasedAuthorization.create("user");

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    router.post("/api/login").handler(this::handleLoginUser);
    router.post("/api/users").handler(this::handleAddUser);
    // protect the API
    router.route("/api/*").handler(JWTAuthHandler.create(jwt));
    router.get("/api/users/:userID").handler(this::handleGetUser);
    router.delete("/api/users/:userID").handler(this::handleDelUser);
    router.put("/api/users").handler(this::handleEditUser);
    router.get("/api/users").handler(this::handleListUsers);

    vertx.createHttpServer().requestHandler(router).listen(8888, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8888");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }

  private void setUpInitialData() {
    // Create a JDBC client with a test database
    client = JDBCPool.pool(vertx, new JsonObject()
      .put("url", "jdbc:hsqldb:mem:test?shutdown=true")
      .put("driver_class", "org.hsqldb.jdbcDriver"));
    getUserTmpl = SqlTemplate
      .forQuery(client, "SELECT * FROM users WHERE id = #{id}")
      .mapTo(Row::toJson);
    loginUserTmpl = SqlTemplate
      .forQuery(client, "SELECT * FROM users where username = #{username} and password = #{password}")
      .mapTo(Row::toJson);
    addUserTmpl = SqlTemplate
      .forUpdate(client, "INSERT INTO users (username, email, password, role) VALUES (#{username}, #{email}, #{password}, #{role})")
      .mapFrom(TupleMapper.jsonObject());
    editUserTmpl = SqlTemplate
      .forUpdate(client, "UPDATE users SET username = #{username}, email = #{email}, password = #{password}, role = #{role} WHERE id = #{id}")
      .mapFrom(TupleMapper.jsonObject());
    delUserTmpl = SqlTemplate
      .forQuery(client, "DELETE FROM users WHERE id = #{id}")
      .mapTo(Row::toJson);

    client.query("CREATE TABLE IF NOT EXISTS users(id INT IDENTITY, username VARCHAR(255), email VARCHAR(255), password VARCHAR(255), role VARCHAR(255))")
      .execute()
      .compose(res -> addUserTmpl.executeBatch(Arrays.asList(
        new JsonObject().put("username", "admin").put("email", "admin@admin.com").put("password", "admin").put("role", "admin"),
        new JsonObject().put("username", "febby").put("email", "febby@gmail.com").put("password", "password").put("role", "user"),
        new JsonObject().put("username", "inyilis").put("email", "inyilis@gmail.com").put("password", "password").put("role", "user")
        ))
      );
  }

  private void handleLoginUser(RoutingContext routingContext) {
    JsonObject reqBody = routingContext.getBodyAsJson();
    Map<String, Object> userLogin = new HashMap<>();
    userLogin.put("username", reqBody.getValue("username"));
    userLogin.put("password", reqBody.getValue("password"));
    loginUserTmpl.execute(userLogin)
        .onSuccess(result -> {
          if (result.size() == 0) {
            routingContext.fail(404);
          } else {
            Object role = result.iterator().next().getValue("ROLE");
            List<String> authorities = new ArrayList<>();
            authorities.add(role.toString());

            routingContext.response().putHeader("Content-Type", "text/plain");
            routingContext.response()
              .end(jwt.generateToken(
                new JsonObject()
                  .put("username", reqBody.getValue("username")),
                new JWTOptions().setExpiresInMinutes(60).setPermissions(authorities)
              ));
          }
        }).onFailure(err -> {
        routingContext.fail(500);
      });
  }

  private void handleListUsers(RoutingContext routingContext) {
    User user = routingContext.user();
    authzProvider.getAuthorizations(user).onComplete(ar -> {
      if (ar.succeeded()) {
        // protect the API
        if (adminAuth.match(user)) {
          client.query("SELECT * FROM users").execute(query -> {
            if (query.failed()) {
              routingContext.fail(500);
            } else {
              JsonArray arr = new JsonArray();
              query.result().forEach(row -> {
                arr.add(row.toJson());
              });
              routingContext.response().putHeader("content-type", "application/json").end(arr.encode());
            }
          });
        } else {
          routingContext.response().setStatusCode(403).end();
        }
      } else {
        routingContext.fail(ar.cause());
      }
    });
  }

  private void handleGetUser(RoutingContext routingContext) {
    String userID = routingContext.request().getParam("userID");
    HttpServerResponse response = routingContext.response();
    User user = routingContext.user();
    authzProvider.getAuthorizations(user).onComplete(ar -> {
      if (ar.succeeded()) {
        // protect the API
        if (adminAuth.match(user)) {
          getUserTmpl.execute(Collections.singletonMap("id", userID))
            .onSuccess(result -> {
              if (result.size() == 0) {
                routingContext.fail(404);
              } else {
                response
                  .putHeader("content-type", "application/json")
                  .end(result.iterator().next().encode());
              }
            }).onFailure(err -> {
            routingContext.fail(500);
          });
        } else {
          routingContext.response().setStatusCode(403).end();
        }
      } else {
        routingContext.fail(ar.cause());
      }
    });
  }

  private void handleAddUser(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject reqBody = routingContext.getBodyAsJson();
    addUserTmpl.execute(reqBody)
      .onSuccess(res -> response.end(reqBody.encodePrettily()))
      .onFailure(err -> routingContext.fail(500));
  }

  private void handleEditUser(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject user = routingContext.getBodyAsJson();
    editUserTmpl.execute(user)
      .onSuccess(res -> response.end(user.encodePrettily()))
      .onFailure(err -> routingContext.fail(500));
  }

  private void handleDelUser(RoutingContext routingContext) {
    String userID = routingContext.request().getParam("userID");
    HttpServerResponse response = routingContext.response();
    User user = routingContext.user();
    authzProvider.getAuthorizations(user).onComplete(ar -> {
      if (ar.succeeded()) {
        // protect the API
        if (userAuth.match(user)) {
          delUserTmpl.execute(Collections.singletonMap("id", userID))
            .onSuccess(result -> {
              if (result.size() == 0) {
                routingContext.fail(404);
              } else {
                response
                  .putHeader("content-type", "application/json")
                  .end();
              }
            }).onFailure(err -> {
            routingContext.fail(500);
          });
        } else {
          routingContext.response().setStatusCode(403).end();
        }
      } else {
        routingContext.fail(ar.cause());
      }
    });
  }
}
