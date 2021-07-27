package com.example.my_api_vertx.Verticle;

import com.example.my_api_vertx.config.ConfigDB;
import com.example.my_api_vertx.controller.TaskController;
import com.example.my_api_vertx.controller.UserController;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.jwt.authorization.JWTAuthorization;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.jdbcclient.JDBCPool;

public class MainVerticle extends AbstractVerticle {

  private PermissionBasedAuthorization adminAuth, userAuth;

  @Override
  public void start(Promise<Void> startPromise) {
    ConfigDB configDB = new ConfigDB();
    JDBCPool client = configDB.setUp(vertx);

    // Create a JWT Auth Provider
    JWTAuth jwt = JWTAuth.create(vertx, new JWTAuthOptions()
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("HS256")
        .setBuffer("keyboard cat")));

    JWTAuthorization authzProvider = JWTAuthorization.create("permissions");
    adminAuth = PermissionBasedAuthorization.create("admin");
    userAuth = PermissionBasedAuthorization.create("user");

    Router mainRouter = Router.router(vertx);
//    mainRouter.route("/static/*").handler(StaticHandler.create());
    mainRouter.route("/doc/*").handler(StaticHandler.create().setCachingEnabled(false).setWebRoot("webroot/swagger-ui-dist"));

    UserController userController = new UserController();
    TaskController taskController = new TaskController();

    Router userRouter = userController.run(vertx, client, jwt, authzProvider, adminAuth, userAuth);
    Router taskRouter = taskController.run(vertx, client, jwt, authzProvider, adminAuth, userAuth);

    mainRouter.mountSubRouter("/user", userRouter);
    mainRouter.mountSubRouter("/task", taskRouter);
    vertx.createHttpServer()
      .requestHandler(mainRouter)
      .listen(8888, http -> {
        if (http.succeeded()) {
          startPromise.complete();
          System.out.println("HTTP server started on port 8888");
        } else {
          startPromise.fail(http.cause());
        }
      });

  }

}
