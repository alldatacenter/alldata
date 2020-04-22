package com.platform.website.sdk.test;

import com.platform.website.sdk.AnalyticsEngineSDK;

public class Test {

  public static void main(String[] args) {
    AnalyticsEngineSDK.onChargeSuccess("order123", "member123");
    AnalyticsEngineSDK.onChargeRefund("order123", "member123");
  }
}
