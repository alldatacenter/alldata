/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.manage.biz.dal.pojo;

import java.io.Serializable;
import java.util.Date;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class Department implements Serializable {
  private static final long serialVersionUID = 1L;
  private Integer dptId;

  private Integer parentId;

  private String name;

  private Date gmtCreate;

  private Date gmtModified;


  private Integer templateFlag;

  // Indexset 应用模板特性，是否有实时特性，是否有普通模式特性
  public Integer getTemplateFlag() {
    return templateFlag;
  }

  public void setTemplateFlag(Integer templateFlag) {
    this.templateFlag = templateFlag;
  }

  /**
   * prop:full_name
   */
  private String fullName;

  /**
   * prop:leaf
   */
  private Boolean leaf;


  public Integer getDptId() {
    return dptId;
  }

  public void setDptId(Integer dptId) {
    this.dptId = dptId;
  }

  public Integer getParentId() {
    return parentId;
  }

  public void setParentId(Integer parentId) {
    this.parentId = parentId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name == null ? null : name.trim();
  }

  public Date getGmtCreate() {
    return gmtCreate;
  }

  public void setGmtCreate(Date gmtCreate) {
    this.gmtCreate = gmtCreate;
  }

  public Date getGmtModified() {
    return gmtModified;
  }

  public void setGmtModified(Date gmtModified) {
    this.gmtModified = gmtModified;
  }

  /**
   * get:full_name
   */
  public String getFullName() {
    return fullName;
  }

  /**
   * set:full_name
   */
  public void setFullName(String fullName) {
    this.fullName = fullName == null ? null : fullName.trim();
  }

  /**
   * get:leaf
   */
  public Boolean getLeaf() {
    return leaf;
  }

  /**
   * set:leaf
   */
  public void setLeaf(Boolean leaf) {
    this.leaf = leaf;
  }

}
