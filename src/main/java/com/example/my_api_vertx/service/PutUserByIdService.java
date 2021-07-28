package com.example.my_api_vertx.service;

import com.example.my_api_vertx.repository.UserRepository;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.templates.SqlTemplate;
import io.vertx.sqlclient.templates.TupleMapper;

public class PutUserByIdService {
  public static void execute(RoutingContext routingContext, JDBCPool client) {
    HttpServerResponse response = routingContext.response();
    JsonObject user = routingContext.getBodyAsJson();

    SqlTemplate
      .forUpdate(client, UserRepository.editUser())
      .mapFrom(TupleMapper.jsonObject())
      .execute(user)
      .onSuccess(res -> response.end(user.encodePrettily()))
      .onFailure(err -> routingContext.fail(500));
  }
}
