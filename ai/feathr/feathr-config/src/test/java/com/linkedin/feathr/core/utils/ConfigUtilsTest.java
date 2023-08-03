package com.linkedin.feathr.core.utils;

import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import org.testng.annotations.Test;


public class ConfigUtilsTest {
  @Test(description = "Tests validating timestamp pattern.")
  public void testTimestampPatternValidCases() {
    ConfigUtils.validateTimestampPatternWithEpoch("Default", "2020/10/01", "yyyy/MM/dd");
    ConfigUtils.validateTimestampPatternWithEpoch("Default", "2020/10/01/00/00/00","yyyy/MM/dd/HH/mm/ss");
    ConfigUtils.validateTimestampPatternWithEpoch("Default", "1601279713", "epoch");
    ConfigUtils.validateTimestampPatternWithEpoch("Default", "1601279713000", "epoch_millis");
  }

  @Test(expectedExceptions = ConfigBuilderException.class, description = "Tests validating timestamp pattern.")
  public void testTimestampPatternInvalidValidCase1() {
    ConfigUtils.validateTimestampPatternWithEpoch("Default", "2020/10/01","yyy/mm/dd");
  }

  @Test(expectedExceptions = ConfigBuilderException.class, description = "Tests validating timestamp pattern.")
  public void testTimestampPatternInvalidValidCase2() {
    ConfigUtils.validateTimestampPatternWithEpoch("Default", "1601279713","epcho");
  }
}
