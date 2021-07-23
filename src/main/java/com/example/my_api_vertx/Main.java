package com.example.my_api_vertx;

import com.example.my_api_vertx.controller.UserController;
import io.vertx.core.Vertx;

public class Main {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
//    vertx.deployVerticle(new MainVerticle());
    vertx.deployVerticle(new UserController());
  }

}
