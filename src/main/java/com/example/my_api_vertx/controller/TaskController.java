package com.example.my_api_vertx.controller;

import com.example.my_api_vertx.config.Authorization;
import com.example.my_api_vertx.service.TaskService;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.authorization.JWTAuthorization;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.jdbcclient.JDBCPool;

public class TaskController {

  public Router run(
    Vertx vertx,
    JDBCPool client,
    JWTAuth jwt,
    JWTAuthorization authzProvider,
    PermissionBasedAuthorization adminAuth,
    PermissionBasedAuthorization userAuth
  ) {

    TaskService taskService = new TaskService();
    Authorization authorization = new Authorization();

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    // Protect the API
    router.route("/*")
      .handler(JWTAuthHandler.create(jwt))
      .handler(routingContext -> {
        authorization.validation(routingContext, authzProvider, adminAuth);
      });

    // Add Task
    router.post("/tasks").handler(routingContext -> {
      taskService.handleAddTask(routingContext, client);
    });

    // List Tasks
    router.get("/tasks").handler(routingContext -> {
      taskService.handleListTasks(routingContext, client);
    });

    // Get Task by Id
    router.get("/tasks/:tasksId").handler(routingContext -> {
      taskService.handleGetTaskById(routingContext, client);
    });

    // Edit Task
    router.put("/tasks").handler(routingContext -> {
      taskService.handleEditTaskById(routingContext, client);
    });

    // Del Task by Id
    router.delete("/tasks/:tasksId").handler(routingContext -> {
      taskService.handleDelTask(routingContext, client);
    });

    return router;
  }

}
