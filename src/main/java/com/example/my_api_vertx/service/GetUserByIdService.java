package com.example.my_api_vertx.service;

import com.example.my_api_vertx.repository.UserRepository;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.templates.SqlTemplate;

import java.util.Collections;

public class GetUserByIdService {
  public static void execute(RoutingContext routingContext, JDBCPool client) {
    String userID = routingContext.request().getParam("userID");
    HttpServerResponse response = routingContext.response();

    SqlTemplate
      .forQuery(client, UserRepository.getUserById())
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
}
