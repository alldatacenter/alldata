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
package com.qlangtech.tis.openapi.impl;

import com.qlangtech.tis.pubhook.common.RunEnvironment;
import org.apache.commons.lang.StringUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-12-20
 */
public class AppKey {

  public final String appName;

  public final Short groupIndex;

  public final RunEnvironment runtime;

  public final boolean unmergeglobalparams;

  // 目标配置文件版本
  private Long targetSnapshotId;

  // 取的内容是否要用缓存中索取
  private boolean fromCache = true;

  public static AppKey create(String collectionName) {
    final AppKey appKey = new AppKey(collectionName, /* appName ========== */
      (short) 0, /* groupIndex */
      RunEnvironment.getSysRuntime(), false);
    return appKey;
  }

  public AppKey(String appName, Short groupIndex, RunEnvironment runtime, boolean unmergeglobalparams) {
    this.appName = appName;
    this.groupIndex = groupIndex;
    this.runtime = runtime;
    this.unmergeglobalparams = unmergeglobalparams;
  }

  public boolean isFromCache() {
    return fromCache;
  }

  public AppKey setFromCache(boolean fromCache) {
    this.fromCache = fromCache;
    return this;
  }

  public Long getTargetSnapshotId() {
    return targetSnapshotId;
  }

  public AppKey setTargetSnapshotId(Long targetSnapshotId) {
    this.targetSnapshotId = targetSnapshotId;
    return this;
  }

  @Override
  public int hashCode() {
    // 确保 这个key在5秒之内是相同的
    final String stamp = (appName + String.valueOf(groupIndex) + runtime.getKeyName() + String.valueOf(unmergeglobalparams) + (this.getTargetSnapshotId() == null ? StringUtils.EMPTY : this.getTargetSnapshotId()) + (System.currentTimeMillis() / (1000 * 50)));
    return stamp.hashCode();
  }

  @Override
  public String toString() {
    return "AppKey{" + "appName='" + appName + '\'' + ", groupIndex=" + groupIndex + ", runtime=" + runtime + ", unmergeglobalparams=" + unmergeglobalparams + ", targetSnapshotId=" + targetSnapshotId + ", fromCache=" + fromCache + '}';
  }

  public interface IAppKeyProcess {
    void process(AppKey appKey);
  }
}
