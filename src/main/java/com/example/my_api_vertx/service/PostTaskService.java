package com.example.my_api_vertx.service;

import com.example.my_api_vertx.repository.TaskRepository;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.templates.SqlTemplate;
import io.vertx.sqlclient.templates.TupleMapper;

public class PostTaskService {
  public static void execute(RoutingContext routingContext, JDBCPool client) {
    HttpServerResponse response = routingContext.response();
    JsonObject reqBody = routingContext.getBodyAsJson();

    SqlTemplate
      .forUpdate(client, TaskRepository.addTask())
      .mapFrom(TupleMapper.jsonObject())
      .execute(reqBody)
      .onSuccess(res -> response.end(reqBody.encodePrettily()))
      .onFailure(err -> routingContext.fail(500));
  }
}
