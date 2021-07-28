package com.example.my_api_vertx.repository;

public class TaskRepository {

  public static String addTask() {
    return "INSERT INTO tasks (name, user_id) VALUES (#{name}, #{user_id})";
  }

  public static String getAllTask() {
    return "SELECT * FROM tasks";
  }

  public static String getTaskById() {
    return "SELECT * FROM tasks WHERE id = #{id}";
  }

  public static String editTask() {
    return "UPDATE tasks SET name = #{name}, user_id = #{user_id} WHERE id = #{id}";
  }

  public static String delTask() {
    return "DELETE FROM tasks WHERE id = #{id}";
  }

}
