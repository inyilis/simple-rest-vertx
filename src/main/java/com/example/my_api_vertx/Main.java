package com.example.my_api_vertx;

import com.example.my_api_vertx.Verticle.MainVerticle;
import io.vertx.core.Vertx;

public class Main {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
//    vertx.deployVerticle(new VertxVerticle());
    vertx.deployVerticle(new MainVerticle());
  }

}
