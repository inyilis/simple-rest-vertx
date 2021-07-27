package com.example.my_api_vertx.service;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.templates.SqlTemplate;
import io.vertx.sqlclient.templates.TupleMapper;

import java.util.*;

public class TaskService {

  public void handleAddTask(RoutingContext routingContext, JDBCPool client) {
    HttpServerResponse response = routingContext.response();
    JsonObject reqBody = routingContext.getBodyAsJson();

    SqlTemplate
      .forUpdate(client, "INSERT INTO tasks (name, user_id) VALUES (#{name}, #{user_id})")
      .mapFrom(TupleMapper.jsonObject())
      .execute(reqBody)
      .onSuccess(res -> response.end(reqBody.encodePrettily()))
      .onFailure(err -> routingContext.fail(500));
  }

  public void handleListTasks(RoutingContext routingContext, JDBCPool client) {
    client.query("SELECT * FROM tasks").execute(query -> {
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

  public void handleGetTaskById(RoutingContext routingContext, JDBCPool client) {
    String tasksId = routingContext.request().getParam("tasksId");
    HttpServerResponse response = routingContext.response();

    SqlTemplate
      .forQuery(client, "SELECT * FROM tasks WHERE id = #{id}")
      .mapTo(Row::toJson)
      .execute(Collections.singletonMap("id",tasksId))
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

  public void handleEditTaskById(RoutingContext routingContext, JDBCPool client) {
    HttpServerResponse response = routingContext.response();
    JsonObject user = routingContext.getBodyAsJson();

    SqlTemplate
      .forUpdate(client, "UPDATE tasks SET name = #{name}, user_id = #{user_id} WHERE id = #{id}")
      .mapFrom(TupleMapper.jsonObject())
      .execute(user)
      .onSuccess(res -> response.end(user.encodePrettily()))
      .onFailure(err -> routingContext.fail(500));
  }

  public void handleDelTask(RoutingContext routingContext, JDBCPool client) {
    String tasksId = routingContext.request().getParam("tasksId");
    HttpServerResponse response = routingContext.response();

    SqlTemplate
      .forQuery(client, "DELETE FROM tasks WHERE id = #{id}")
      .mapTo(Row::toJson)
      .execute(Collections.singletonMap("id", tasksId))
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
