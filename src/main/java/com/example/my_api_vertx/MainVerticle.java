package com.example.my_api_vertx;

import com.example.my_api_vertx.config.ConfigDB;
import com.example.my_api_vertx.service.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.jwt.authorization.JWTAuthorization;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.jdbcclient.JDBCPool;

public class MainVerticle extends AbstractVerticle {

  private JWTAuth jwt;
  private JWTAuthorization authzProvider;
  private PermissionBasedAuthorization adminAuth, userAuth;

  private JDBCPool client;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    client = ConfigDB.setUp(vertx);

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
    routing(router);

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8888, http -> {
        if (http.succeeded()) {
          startPromise.complete();
          System.out.println("HTTP server started on port 8888");
        } else {
          startPromise.fail(http.cause());
        }
      });
  }

  void routing(Router router) {
    // Login
    router.post("/login").handler(routingContext -> {
      PostUserLoginService.execute(routingContext, client, jwt);
    });
    // Sign Up User
    router.post("/signup").handler(routingContext -> {
      PostUserSignUpService.execute(routingContext, client);
    });

    // Protect the API
    router.route("/users").handler(JWTAuthHandler.create(jwt));
    // List Users
    router.get("/users").handler(routingContext -> {
      User user = routingContext.user();
      authzProvider.getAuthorizations(user).onComplete(ar -> {
        if (ar.succeeded()) {
          // protect the API
          if (adminAuth.match(user)) {
            GetAllUsersService.execute(routingContext, client);
          } else {
            routingContext.fail(403);
          }
        } else {
          routingContext.fail(ar.cause());
        }
      });
    });
    // Get User by Id
    router.get("/users/:userID").handler(routingContext -> {
      GetUserByIdService.execute(routingContext, client);
    });
    // Edit User
    router.put("/users").handler(routingContext -> {
      PutUserByIdService.execute(routingContext, client);
    });
    // Del User by Id
    router.delete("/users/:userID").handler(routingContext -> {
      DeleteUserService.execute(routingContext, client);
    });

    // Protect the API
    router.route("/tasks").handler(JWTAuthHandler.create(jwt));
    // Authorization
    router.route("/tasks").handler(routingContext -> {
      User user = routingContext.user();
      authzProvider.getAuthorizations(user).onComplete(ar -> {
        if (ar.succeeded()) {
          // protect the API
          if (adminAuth.match(user)) {
            routingContext.next();
          } else {
            routingContext.fail(403);
          }
        } else {
          routingContext.fail(ar.cause());
        }
      });
    });
    // Add Task
    router.post("/tasks").handler(routingContext -> {
      PostTaskService.execute(routingContext, client);
    });
    // List Tasks
    router.get("/tasks").handler(routingContext -> {
      GetAllTaskService.execute(routingContext, client);
    });
    // Get Task by Id
    router.get("/tasks/:tasksId").handler(routingContext -> {
      GetTaskByidService.execute(routingContext, client);
    });
    // Edit Task
    router.put("/tasks").handler(routingContext -> {
      PutTaskService.execute(routingContext, client);
    });
    // Del Task by Id
    router.delete("/tasks/:tasksId").handler(routingContext -> {
      DeleteTaskService.execute(routingContext, client);
    });

//    router.route("/doc/*").handler(StaticHandler.create().setCachingEnabled(false).setWebRoot("webroot/swagger-ui-dist"));

  }

}
