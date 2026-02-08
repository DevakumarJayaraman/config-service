package com.example.configservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single configuration difference between two profiles.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigDifference {

  private String key;

  @JsonProperty("valueA")
  private String valueA;

  @JsonProperty("valueB")
  private String valueB;

  @JsonProperty("sourceA")
  private String sourceA;

  @JsonProperty("sourceB")
  private String sourceB;

  private String status; // SAME, DIFF, {profile}-missing
}
