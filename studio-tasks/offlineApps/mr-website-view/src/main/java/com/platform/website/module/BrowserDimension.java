package com.platform.website.module;

import lombok.Data;

@Data
public class BrowserDimension {

  private int id;
  private String browser;
  private String browser_version;

  public String getBrowser_version() {
    return browser_version;
  }

  public void setBrowser_version(String browser_version) {
    this.browser_version = browser_version;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getBrowser() {
    return browser;
  }

  public void setBrowser(String browser) {
    this.browser = browser;
  }

  public BrowserDimension() {
    super();
  }

  public BrowserDimension(int id) {
    super();
    this.id = id;
  }

  public BrowserDimension(String browser) {
    super();
    this.browser = browser;
  }

  public BrowserDimension(String browser, String broswer_verison) {
    super();
    this.browser = browser;
    this.browser_version = broswer_verison;
  }

  @Override
  public String toString() {
    return "BrowserDimension{" +
        "id=" + id +
        ", browser='" + browser + '\'' +
        ", browser_version='" + browser_version + '\'' +
        '}';
  }
}
