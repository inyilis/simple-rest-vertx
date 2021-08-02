package com.example.my_api_vertx.service;

import com.example.my_api_vertx.config.Query;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.templates.SqlTemplate;
import io.vertx.sqlclient.templates.TupleMapper;

public class PutTaskService {
  public static void execute(RoutingContext routingContext, JDBCPool client) {
    HttpServerResponse response = routingContext.response();
    JsonObject updateUser = routingContext.getBodyAsJson();
    Query getQuery = new Query();

    SqlTemplate
      .forUpdate(client, getQuery.execute("update-task"))
      .mapFrom(TupleMapper.jsonObject())
      .execute(updateUser)
      .onSuccess(res -> response.end(updateUser.encodePrettily()))
      .onFailure(err -> routingContext.fail(500));
  }
}
