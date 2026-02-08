package com.example.configservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Non-merged view: each file is returned as its own source.
 * properties are always FLAT keys (eg: db.host, server.port).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigSource {
  private String name;
  private String path;
  private String type;                  // "yaml" or "properties"
  private Map<String, Object> properties; // flat keys
}
