package com.platform.backend.model;

import com.platform.backend.datasource.Transaction;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Alert {
  private Integer ruleId;
  private String rulePayload;

  Transaction triggeringEvent;
  BigDecimal triggeringValue;
}
