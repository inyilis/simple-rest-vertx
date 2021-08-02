package com.example.my_api_vertx.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class Query {
  public String execute(String query) {
    Yaml yaml = new Yaml();
    InputStream inputStream = this.getClass()
      .getClassLoader()
      .getResourceAsStream("query.yaml");
    Map<String, Object> obj = yaml.load(inputStream);
    return obj.get(query).toString();
  }
}
