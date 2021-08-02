package com.example.my_api_vertx.service;

import com.example.my_api_vertx.config.Query;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.templates.SqlTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostUserLoginService {
  public static void execute(RoutingContext routingContext, JDBCPool client, JWTAuth jwt) {
    JsonObject reqBody = routingContext.getBodyAsJson();
    Map<String, Object> userLogin = new HashMap<>();
    userLogin.put("username", reqBody.getValue("username"));
    userLogin.put("password", reqBody.getValue("password"));
    Query getQuery = new Query();

    SqlTemplate
      .forQuery(client, getQuery.execute("user-login"))
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
}
