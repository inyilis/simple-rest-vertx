package com.example.my_api_vertx.repository;

public class UserRepository {

  public static String userLogin() {
    return "SELECT * FROM users where username = #{username} and password = #{password}";
  }

  public static String userSignUp() {
    return "INSERT INTO users (username, email, password, role) VALUES (#{username}, #{email}, #{password}, #{role})";
  }

  public static String getAllUsers() {
    return "SELECT * FROM users";
  }

  public static String getUserById() {
    return "SELECT * FROM users WHERE id = #{id}";
  }

  public static String editUser() {
    return "UPDATE users SET username = #{username}, email = #{email}, password = #{password}, role = #{role} WHERE id = #{id}";
  }

  public static String delUser() {
    return "DELETE FROM users WHERE id = #{id}";
  }

}
