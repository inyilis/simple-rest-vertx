package com.example.my_api_vertx.config;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.templates.SqlTemplate;
import io.vertx.sqlclient.templates.TupleMapper;

import java.util.Arrays;

public class ConfigDB {

  public static JDBCPool setUp(Vertx vertx) {

    // Create a JDBC client with a test database
    JDBCPool client = JDBCPool.pool(vertx, new JsonObject()
      .put("url", "jdbc:hsqldb:mem:test?shutdown=true")
      .put("driver_class", "org.hsqldb.jdbcDriver"));

    client.query("CREATE TABLE IF NOT EXISTS users(id INT IDENTITY, username VARCHAR(255), email VARCHAR(255), password VARCHAR(255), role VARCHAR(255))")
      .execute()
      .compose(res ->
        SqlTemplate
          .forUpdate(client, "INSERT INTO users (username, email, password, role) VALUES (#{username}, #{email}, #{password}, #{role})")
          .mapFrom(TupleMapper.jsonObject())
          .executeBatch(Arrays.asList(
            new JsonObject().put("username", "admin").put("email", "admin@admin.com").put("password", "admin").put("role", "admin"),
            new JsonObject().put("username", "febby").put("email", "febby@gmail.com").put("password", "password").put("role", "user"),
            new JsonObject().put("username", "inyilis").put("email", "inyilis@gmail.com").put("password", "password").put("role", "user")
          ))
      );

    client.query("CREATE TABLE IF NOT EXISTS tasks(id INT IDENTITY, name VARCHAR(255), user_id INT)")
      .execute()
      .compose(res ->
        SqlTemplate
          .forUpdate(client, "INSERT INTO tasks (name, user_id) VALUES (#{name}, #{user_id})")
          .mapFrom(TupleMapper.jsonObject())
          .executeBatch(Arrays.asList(
            new JsonObject().put("name", "Admin").put("user_id", 0),
            new JsonObject().put("name", "FE").put("user_id", 1),
            new JsonObject().put("name", "BE").put("user_id", 2)
          ))
      );

    return client;
  }

}
