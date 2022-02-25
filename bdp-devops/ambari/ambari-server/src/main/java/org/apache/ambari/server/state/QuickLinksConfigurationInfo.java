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

import org.apache.ambari.server.controller.ApiModel;
import org.apache.ambari.server.state.quicklinks.QuickLinks;

import io.swagger.annotations.ApiModelProperty;

/**
 * Wrapper for quickLinksConfiguration description
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class QuickLinksConfigurationInfo implements ApiModel {

  private String fileName;
  @XmlElement(name = "default")
  private Boolean isDefault = false;
  private Boolean deleted = false;

  @XmlTransient
  private Map<String, QuickLinks> quickLinksConfigurationMap = null;

  public QuickLinksConfigurationInfo() {
  }

  public Map<String, QuickLinks> getQuickLinksConfigurationMap() {
    return quickLinksConfigurationMap;
  }

  public void setQuickLinksConfigurationMap(Map<String, QuickLinks> quickLinksConfigurationMap) {
    this.quickLinksConfigurationMap = quickLinksConfigurationMap;
  }

  @Override
  public String toString() {
    return "QuickLinksConfigurationInfo{" +
      "deleted=" + deleted +
      ", fileName='" + fileName + '\'' +
      ", isDefault=" + isDefault +
      ", quickLinksConfigurationMap=" + quickLinksConfigurationMap +
      '}';
  }

  @ApiModelProperty(name = "file_name")
  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  @ApiModelProperty(name = "default")
  public Boolean getIsDefault() {
    return isDefault;
  }

  public void setIsDefault(Boolean isDefault) {
    this.isDefault = isDefault;
  }

  @ApiModelProperty(hidden = true)
  public Boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(Boolean deleted) {
    this.deleted = deleted;
  }
}
