/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qlangtech.tis.db.parser;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.util.DescribableJSON;
import com.qlangtech.tis.workflow.pojo.DatasourceDb;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class DBConfigSuit {
  private final DatasourceDb db;
  private final boolean supportDataXReader;
  // 数据源对应的导入ReaderDataX配置是否配置了
  private final boolean dataReaderSetted;

  private List<ISelectedTab> tabs = Lists.newArrayList();

  public DBConfigSuit(DatasourceDb db, boolean supportDataXReader, boolean dataReaderSetted) {
    this.dataReaderSetted = dataReaderSetted;
    this.supportDataXReader = supportDataXReader;
    this.db = db;
  }

  /**
   * db 对应已经选择的表列表
   *
   * @return
   */
  public List<String> getSelectedTabs() {
    return this.tabs.stream().map((tab) -> tab.getName()).collect(Collectors.toList());
  }

  public ISelectedTab getTab(String tab) {
    Optional<ISelectedTab> targetTab = this.tabs.stream().filter((t) -> StringUtils.equals(t.getName(), tab)).findFirst();
    if (!targetTab.isPresent()) {
      throw new IllegalStateException("target table:" + tab + " can not be empty");
    }
    return targetTab.get();
  }

  public void addTabs(List<ISelectedTab> tabs) {
    this.tabs.addAll(tabs);
  }

  public boolean isSupportDataXReader() {
    return this.supportDataXReader;
  }

  public boolean isDataReaderSetted() {
    return this.dataReaderSetted;
  }

  public Integer getDbId() {
    return this.db.getId();
  }

  public String getName() {
    return this.db.getName();
  }


  private DescribableJSON detailed;
  private DescribableJSON facade;

  public JSONObject getDetailed() throws Exception {
    return this.detailed.getItemJson();
  }

  public JSONObject getFacade() throws Exception {
    if (this.facade == null) {
      return null;
    }
    return this.facade.getItemJson();
  }

  public void setDetailed(DataSourceFactory detailed) {
    if (detailed == null) {
      throw new IllegalStateException("param detailed can not be null");
    }
    this.detailed = new DescribableJSON(detailed);
  }

  public void setFacade(DataSourceFactory facade) {
    if (facade == null) {
      throw new IllegalStateException("param detailed can not be null");
    }
    this.facade = new DescribableJSON(facade);
  }
}
