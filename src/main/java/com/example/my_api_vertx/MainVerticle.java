package com.example.my_api_vertx;

import com.example.my_api_vertx.util.Runner;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MainVerticle extends AbstractVerticle {

  // Convenience method so you can run it in your IDE
  public static void main(String[] args) {
    Runner.runExample(MainVerticle.class);
  }

  private JWTAuth jwt;
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
      .setKeyStore(new KeyStoreOptions()
        .setType("jceks")
        .setPath("keystore.jceks")
        .setPassword("secret")));

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    router.post("/api/login").handler(this::handleLoginUser);
    // protect the API
    router.route("/api/*").handler(JWTAuthHandler.create(jwt));
    router.get("/api/users/:userID").handler(this::handleGetUser);
    router.delete("/api/users/:userID").handler(this::handleDelUser);
    router.post("/api/users").handler(this::handleAddUser);
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
      .forUpdate(client, "INSERT INTO users (username, email, password) VALUES (#{username}, #{email}, #{password})")
      .mapFrom(TupleMapper.jsonObject());
    editUserTmpl = SqlTemplate
      .forUpdate(client, "UPDATE users SET username = #{username}, email = #{email}, password = #{password} WHERE id = #{id}")
      .mapFrom(TupleMapper.jsonObject());
    delUserTmpl = SqlTemplate
      .forQuery(client, "DELETE FROM users WHERE id = #{id}")
      .mapTo(Row::toJson);

    client.query("CREATE TABLE IF NOT EXISTS users(id INT IDENTITY, username VARCHAR(255), email VARCHAR(255), password VARCHAR(255))")
      .execute()
      .compose(res -> addUserTmpl.executeBatch(Arrays.asList(
        new JsonObject().put("username", "admin").put("email", "admin@admin.com").put("password", "admin"),
        new JsonObject().put("username", "febby").put("email", "febby@gmail.com").put("password", "password"),
        new JsonObject().put("username", "inyilis").put("email", "inyilis@gmail.com").put("password", "password")
        ))
      );
  }

  private void handleLoginUser(RoutingContext routingContext) {
    MultiMap params = routingContext.request().params();
    String username = params.get("username");
    String password = params.get("password");
    Map<String, Object> userLogin = new HashMap<>();
    userLogin.put("username", username);
    userLogin.put("password", password);
    if (username == null && password == null) {
      routingContext.fail(400);
    } else {
      loginUserTmpl
        .execute(userLogin)
        .onSuccess(result -> {
          if (result.size() == 0) {
            routingContext.fail(404);
          } else {
            routingContext.response().putHeader("Content-Type", "text/plain");
            routingContext.response().end(jwt.generateToken(new JsonObject(), new JWTOptions().setExpiresInMinutes(3)));
          }
        }).onFailure(err -> {
        routingContext.fail(500);
      });
    }
  }

  private void handleListUsers(RoutingContext routingContext) {
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
  }

  private void handleGetUser(RoutingContext routingContext) {
    String userID = routingContext.request().getParam("userID");
    HttpServerResponse response = routingContext.response();
    if (userID == null) {
      routingContext.fail(400);
    } else {
      getUserTmpl
        .execute(Collections.singletonMap("id", userID))
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
    }
  }

  private void handleAddUser(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject user = routingContext.getBodyAsJson();
    addUserTmpl.execute(user)
      .onSuccess(res -> response.end(user.encodePrettily()))
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
    if (userID == null) {
      routingContext.fail(400);
    } else {
      delUserTmpl
        .execute(Collections.singletonMap("id", userID))
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
    }
  }
}
