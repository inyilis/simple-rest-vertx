package com.example.my_api_vertx.service;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.templates.SqlTemplate;
import io.vertx.sqlclient.templates.TupleMapper;

import java.util.*;

public class UserService extends AbstractVerticle {

  private JWTAuth jwt;

  private JDBCPool client;
  private SqlTemplate<Map<String, Object>, RowSet<JsonObject>> loginUserTmpl;
  private SqlTemplate<Map<String, Object>, RowSet<JsonObject>> getUserTmpl;
  private SqlTemplate<Map<String, Object>, RowSet<JsonObject>> delUserTmpl;
  private SqlTemplate<JsonObject, SqlResult<Void>> addUserTmpl;
  private SqlTemplate<JsonObject, SqlResult<Void>> editUserTmpl;

  @Override
  public void start() {
    setUpInitialData();

    // Create a JWT Auth Provider
    jwt = JWTAuth.create(vertx, new JWTAuthOptions()
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("HS256")
        .setBuffer("keyboard cat")));

    vertx.eventBus().consumer("login.addr", this::handleLoginUser);
    vertx.eventBus().consumer("list.user.addr", this::handleListUsers);
    vertx.eventBus().consumer("get.user.addr", this::handleGetUser);
    vertx.eventBus().consumer("add.user.addr", this::handleAddUser);
    vertx.eventBus().consumer("edit.user.addr", this::handleEditUser);
    vertx.eventBus().consumer("del.user.addr", this::handleDelUser);
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

  public void handleLoginUser(Message<JsonObject> message) {
    JsonObject reqBody = message.body();
    Map<String, Object> userLogin = new HashMap<>();
    userLogin.put("username", reqBody.getValue("username"));
    userLogin.put("password", reqBody.getValue("password"));
    loginUserTmpl.execute(userLogin)
      .onSuccess(result -> {
        if (result.size() == 0) {
          message.fail(404, "Not Found");
        } else {
          Object role = result.iterator().next().getValue("ROLE");
          List<String> authorities = new ArrayList<>();
          authorities.add(role.toString());
          message.reply(jwt.generateToken(
            new JsonObject()
              .put("username", reqBody.getValue("username")),
            new JWTOptions().setExpiresInMinutes(60).setPermissions(authorities)));
        }
      }).onFailure(err -> {
        message.fail(500, "Internal Server Error");
      });
  }

  public void handleListUsers(Message message) {
    client.query("SELECT * FROM users").execute(query -> {
      if (query.failed()) {
        message.fail(500, "Internal Server Error");
      } else {
        JsonArray arr = new JsonArray();
        query.result().forEach(row -> {
          arr.add(row.toJson());
        });
        message.reply(arr.encode());
      }
    });
  }

  public void handleGetUser(Message message) {
    String userID = message.body().toString();
    getUserTmpl.execute(Collections.singletonMap("id", userID))
      .onSuccess(result -> {
        if (result.size() == 0) {
          message.fail(404, "Not Found");
        } else {
          message.reply(result.iterator().next().encode());
        }
      }).onFailure(err -> {
      message.fail(500, "Internal Server Error");
    });
  }

  public void handleAddUser(Message<JsonObject> message) {
    JsonObject reqBody = message.body();
    addUserTmpl.execute(reqBody)
      .onSuccess(res -> message.reply(reqBody.encodePrettily()))
      .onFailure(err -> message.fail(500, "Internal Server Error"));
  }

  public void handleEditUser(Message<JsonObject> message) {
    JsonObject reqBody = message.body();
    editUserTmpl.execute(reqBody)
      .onSuccess(res -> message.reply(reqBody.encodePrettily()))
      .onFailure(err -> message.fail(500, "Internal Server Error"));
  }

  public void handleDelUser(Message message) {
    String userID = message.body().toString();

    delUserTmpl.execute(Collections.singletonMap("id", userID))
      .onSuccess(result -> {
        System.out.println(result.iterator());
        if (result.size() == 0) {
          message.fail(404, "Not Found");
        } else {
          message.reply(String.format("Delete User with ID:%s Success!", userID));
        }
      }).onFailure(err -> message.fail(500, "Internal Server Error"));
  }

}
