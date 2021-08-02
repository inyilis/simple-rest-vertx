package com.example.my_api_vertx.service;

import com.example.my_api_vertx.config.Query;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import io.vertx.jdbcclient.JDBCPool;

public class GetAllTaskService {
  public static void execute(RoutingContext routingContext, JDBCPool client) {
    Query getQuery = new Query();
    client.query(getQuery.execute("select-all-task")).execute(query -> {
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
}
