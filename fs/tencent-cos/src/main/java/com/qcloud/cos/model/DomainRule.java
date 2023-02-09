package com.qcloud.cos.model;

public class DomainRule {
 public static String ENABLED = "ENABLED"; // domain status
 public static String DISABLED = "DISABLED"; // domain status
 public static String REST = "REST"; // domain source type
 public static String WEBSITE = "WEBSITE"; // domain source type
 public static String CNAME = "CNAME"; // domain replace type
 public static String TXT = "TXT"; // domain replace type

 private String status;
 private String type;
 private String name;
 private String forcedReplacement;

 public DomainRule() {}

 public String getStatus() {
  return status;
 }

 public void setStatus(String status) {
  this.status = status;
 }

 public String getType() {
  return type;
 }

 public void setType(String type) {
  this.type = type;
 }

 public String getName() {
  return name;
 }

 public void setName(String name) {
  this.name = name;
 }

 public String getForcedReplacement() {
  return forcedReplacement;
 }

 public void setForcedReplacement(String forcedReplacement) {
  this.forcedReplacement = forcedReplacement;
 }
}
