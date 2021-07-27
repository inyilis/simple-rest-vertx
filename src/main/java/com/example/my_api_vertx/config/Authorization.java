package com.example.my_api_vertx.config;

import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.auth.jwt.authorization.JWTAuthorization;
import io.vertx.ext.web.RoutingContext;

public class Authorization {

  public RoutingContext validation(RoutingContext routingContext, JWTAuthorization authzProvider, PermissionBasedAuthorization authorization) {
    User user = routingContext.user();
    authzProvider.getAuthorizations(user).onComplete(ar -> {
      if (ar.succeeded()) {
        // protect the API
        if (authorization.match(user)) {
          routingContext.next();
        } else {
          routingContext.fail(403);
        }
      } else {
        routingContext.fail(ar.cause());
      }
    });

    return routingContext;
  }
}
