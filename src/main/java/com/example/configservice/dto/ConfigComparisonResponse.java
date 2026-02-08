package com.example.configservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for config comparison between two profiles.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigComparisonResponse {

  private String application;
  private String profileA;
  private String profileB;
  private String label;
  private List<ConfigDifference> differences;
}
