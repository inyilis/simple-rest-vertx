package com.example.my_api_vertx.controller;

import com.example.my_api_vertx.config.Authorization;
import com.example.my_api_vertx.service.UserService;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.authorization.JWTAuthorization;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.jdbcclient.JDBCPool;

public class UserController {

  public Router run(
    Vertx vertx,
    JDBCPool client,
    JWTAuth jwt,
    JWTAuthorization authzProvider,
    PermissionBasedAuthorization adminAuth,
    PermissionBasedAuthorization userAuth
  ) {

    UserService userService = new UserService();
    Authorization authorization = new Authorization();

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    // Login
    router.post("/login").handler(routingContext -> {
      userService.handleLoginUser(routingContext, client, jwt);
    });

    // Sign Up User
    router.post("/signup").handler(routingContext -> {
      userService.handleAddUser(routingContext, client);
    });

    // Protect the API
    router.route("/*").handler(JWTAuthHandler.create(jwt));

    // List Users
    router.get("/users")
      .handler(routingContext -> {
        authorization.validation(routingContext, authzProvider, adminAuth);
      })
      .handler(routingContext -> {
        userService.handleListUsers(routingContext, client);
      });

    // Get User by Id
    router.get("/users/:userID").handler(routingContext -> {
      userService.handleGetUserById(routingContext, client);
    });

    // Edit User
    router.put("/users").handler(routingContext -> {
      userService.handleEditUserById(routingContext, client);
    });

    // Del User by Id
    router.delete("/users/:userID")
      .handler(routingContext -> {
        authorization.validation(routingContext, authzProvider, userAuth);
      })
      .handler(routingContext -> {
        userService.handleDelUser(routingContext, client);
      });

    return router;
  }

}
