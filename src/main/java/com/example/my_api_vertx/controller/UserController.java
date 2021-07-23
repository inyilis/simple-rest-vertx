package com.example.my_api_vertx.controller;

import com.example.my_api_vertx.service.UserService;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.jwt.authorization.JWTAuthorization;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;

public class UserController extends AbstractVerticle {

  private JWTAuth jwt;
  private JWTAuthorization authzProvider;
  private PermissionBasedAuthorization adminAuth, userAuth;

  @Override
  public void start() {
    vertx.deployVerticle(new UserService());

    // Create a JWT Auth Provider
    jwt = JWTAuth.create(vertx, new JWTAuthOptions()
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("HS256")
        .setBuffer("keyboard cat")));
    authzProvider = JWTAuthorization.create("permissions");
    adminAuth = PermissionBasedAuthorization.create("admin");
    userAuth = PermissionBasedAuthorization.create("user");

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    // Login
    router.post("/api/login").handler(routingContext -> {
      vertx.eventBus().request("login.addr", routingContext.getBodyAsJson(), reply -> {
        routingContext.response().putHeader("Content-Type", "text/plain");
        if (reply.succeeded()){
          routingContext.request().response().end(reply.result().body().toString());
        } else {
          routingContext.request().response().end(reply.cause().getMessage());
        }
      });
    });

    // Add User
    router.post("/api/users").handler(routingContext -> {
      vertx.eventBus().request("add.user.addr", routingContext.getBodyAsJson(), reply -> {
        routingContext.response().putHeader("content-type", "application/json");
        if (reply.succeeded()){
          routingContext.request().response().end(reply.result().body().toString());
        } else {
          routingContext.request().response().end(reply.cause().getMessage());
        }
      });
    });

    // Protect the API
    router.route("/api/*").handler(JWTAuthHandler.create(jwt));

    // List Users
    router.get("/api/users").handler(routingContext -> {
      User user = routingContext.user();
      authzProvider.getAuthorizations(user).onComplete(ar -> {
        if (ar.succeeded()) {
          // protect the API
          if (adminAuth.match(user)) {
            vertx.eventBus().request("list.user.addr", "", reply -> {
              routingContext.response().putHeader("content-type", "application/json");
              if (reply.succeeded()){
                routingContext.request().response().end(reply.result().body().toString());
              } else {
                routingContext.request().response().end(reply.cause().getMessage());
              }
            });
          } else {
            routingContext.fail(403);
          }
        } else {
          routingContext.fail(ar.cause());
        }
      });
    });

    // Get User by Id
    router.get("/api/users/:userID").handler(routingContext -> {
      String userID = routingContext.request().getParam("userID");
      User user = routingContext.user();
      authzProvider.getAuthorizations(user).onComplete(ar -> {
        if (ar.succeeded()) {
          // protect the API
          if (adminAuth.match(user)) {
            vertx.eventBus().request("get.user.addr", userID, reply -> {
              routingContext.response().putHeader("content-type", "application/json");
              if (reply.succeeded()){
                routingContext.request().response().end(reply.result().body().toString());
              } else {
                routingContext.request().response().end(reply.cause().getMessage());
              }
            });
          } else {
            routingContext.response().setStatusCode(403).end();
          }
        } else {
          routingContext.fail(ar.cause());
        }
      });
    });

    // Edit User
    router.put("/api/users").handler(routingContext -> {
      vertx.eventBus().request("edit.user.addr", routingContext.getBodyAsJson(), reply -> {
        routingContext.response().putHeader("content-type", "application/json");
        if (reply.succeeded()){
          routingContext.request().response().end(reply.result().body().toString());
        } else {
          routingContext.request().response().end(reply.cause().getMessage());
        }
      });
    });

    // Del User by Id
    router.delete("/api/users/:userID").handler(routingContext -> {
      String userID = routingContext.request().getParam("userID");
      User user = routingContext.user();
      authzProvider.getAuthorizations(user).onComplete(ar -> {
        if (ar.succeeded()) {
          // protect the API
          if (userAuth.match(user)) {
            vertx.eventBus().request("del.user.addr", userID, reply -> {
              routingContext.response().putHeader("content-type", "text/plain");
              if (reply.succeeded()){
                routingContext.request().response().end(reply.result().body().toString());
              } else {
                routingContext.request().response().end(reply.cause().getMessage());
              }
            });
          } else {
            routingContext.fail(403);
          }
        } else {
          routingContext.fail(ar.cause());
        }
      });
    });

    vertx.createHttpServer().requestHandler(router).listen(8888);
  }
}
