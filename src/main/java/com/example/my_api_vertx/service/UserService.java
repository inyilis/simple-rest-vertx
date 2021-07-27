package com.example.my_api_vertx.service;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.templates.SqlTemplate;
import io.vertx.sqlclient.templates.TupleMapper;

import java.util.*;

public class UserService {

  public void handleLoginUser(RoutingContext routingContext, JDBCPool client, JWTAuth jwt) {
    JsonObject reqBody = routingContext.getBodyAsJson();
    Map<String, Object> userLogin = new HashMap<>();
    userLogin.put("username", reqBody.getValue("username"));
    userLogin.put("password", reqBody.getValue("password"));

    SqlTemplate
      .forQuery(client, "SELECT * FROM users where username = #{username} and password = #{password}")
      .mapTo(Row::toJson)
      .execute(userLogin)
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

  public void handleAddUser(RoutingContext routingContext, JDBCPool client) {
    HttpServerResponse response = routingContext.response();
    JsonObject reqBody = routingContext.getBodyAsJson();

    SqlTemplate
      .forUpdate(client, "INSERT INTO users (username, email, password, role) VALUES (#{username}, #{email}, #{password}, #{role})")
      .mapFrom(TupleMapper.jsonObject())
      .execute(reqBody)
      .onSuccess(res -> response.end(reqBody.encodePrettily()))
      .onFailure(err -> routingContext.fail(500));
  }

  public void handleListUsers(RoutingContext routingContext, JDBCPool client) {
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

  public void handleGetUserById(RoutingContext routingContext, JDBCPool client) {
    String userID = routingContext.request().getParam("userID");
    HttpServerResponse response = routingContext.response();

    SqlTemplate
      .forQuery(client, "SELECT * FROM users WHERE id = #{id}")
      .mapTo(Row::toJson)
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

  public void handleEditUserById(RoutingContext routingContext, JDBCPool client) {
    HttpServerResponse response = routingContext.response();
    JsonObject user = routingContext.getBodyAsJson();

    SqlTemplate
      .forUpdate(client, "UPDATE users SET username = #{username}, email = #{email}, password = #{password}, role = #{role} WHERE id = #{id}")
      .mapFrom(TupleMapper.jsonObject())
      .execute(user)
      .onSuccess(res -> response.end(user.encodePrettily()))
      .onFailure(err -> routingContext.fail(500));
  }

  public void handleDelUser(RoutingContext routingContext, JDBCPool client) {
    String userID = routingContext.request().getParam("userID");
    HttpServerResponse response = routingContext.response();

    SqlTemplate
      .forQuery(client, "DELETE FROM users WHERE id = #{id}")
      .mapTo(Row::toJson)
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
