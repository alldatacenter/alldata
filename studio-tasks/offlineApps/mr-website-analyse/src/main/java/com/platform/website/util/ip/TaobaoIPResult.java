

package com.platform.website.util.ip;

import lombok.Data;

@Data
public class TaobaoIPResult {
  private int code;
  private String country;
  private String area;
  private String region;
  private String city;
  private String county;
  private String isp;
  private String ip;

}
