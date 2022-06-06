/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.state;

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.ambari.server.state.theme.Theme;

/**
 * Wrapper for theme description
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ThemeInfo {

  private String fileName;
  @XmlElement(name = "default")
  private Boolean isDefault = false;
  private Boolean deleted = false;

  @XmlTransient
  private Map<String, Theme> themeMap = null;

  public ThemeInfo() {
  }

  public Map<String, Theme> getThemeMap() {
    return themeMap;
  }

  public void setThemeMap(Map<String, Theme> themeMap) {
    this.themeMap = themeMap;
  }

  @Override
  public String toString() {
    return "ThemeInfo{" +
      "deleted=" + deleted +
      ", fileName='" + fileName + '\'' +
      ", isDefault=" + isDefault +
      ", themeMap=" + themeMap +
      '}';
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public Boolean getIsDefault() {
    return isDefault;
  }

  public void setIsDefault(Boolean isDefault) {
    this.isDefault = isDefault;
  }

  public Boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(Boolean deleted) {
    this.deleted = deleted;
  }
}
