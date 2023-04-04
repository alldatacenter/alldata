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

package com.netease.arctic.ams.server.model;

import com.netease.arctic.table.TableIdentifier;

import java.math.BigDecimal;

public class TableQuotaInfo implements Comparable<TableQuotaInfo> {
  private TableIdentifier tableIdentifier;
  private BigDecimal quota;
  private Double targetQuota;

  public TableQuotaInfo(TableIdentifier tableIdentifier, BigDecimal quota, Double targetQuota) {
    this.tableIdentifier = tableIdentifier;
    this.quota = quota;
    this.targetQuota = targetQuota;
  }

  public TableIdentifier getTableIdentifier() {
    return tableIdentifier;
  }

  public void setTableIdentifier(TableIdentifier tableIdentifier) {
    this.tableIdentifier = tableIdentifier;
  }

  public BigDecimal getQuota() {
    return quota;
  }

  public void setQuota(BigDecimal quota) {
    this.quota = quota;
  }

  public Double getTargetQuota() {
    return targetQuota;
  }

  public void setTargetQuota(Double targetQuota) {
    this.targetQuota = targetQuota;
  }

  @Override
  public int compareTo(TableQuotaInfo o) {
    if (quota.compareTo(o.getQuota()) != 0) {
      return quota.compareTo(o.getQuota());
    }

    return o.getTargetQuota().compareTo(targetQuota);
  }

  @Override
  public String toString() {
    return "Q{" + tableIdentifier + ": " + quota + "/" + targetQuota + '}';
  }
}
