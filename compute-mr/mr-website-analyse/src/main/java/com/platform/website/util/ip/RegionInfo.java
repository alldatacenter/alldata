package com.platform.website.util.ip;

import lombok.Data;

@Data
public class RegionInfo {

  public static final String DEFAULT_VALUE = "XX";
  private String country = DEFAULT_VALUE;
  private String province = DEFAULT_VALUE;
  private String city = DEFAULT_VALUE;

  public RegionInfo() {
  }

  public RegionInfo(String country, String province, String city) {
    this.country = country;
    this.province = province;
    this.city = city;
  }
}
