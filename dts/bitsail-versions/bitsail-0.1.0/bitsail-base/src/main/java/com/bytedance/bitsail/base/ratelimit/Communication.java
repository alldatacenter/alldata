/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.base.ratelimit;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created at 2018/6/25.
 */
public class Communication implements Serializable {

  private static final long serialVersionUID = 1L;

  private Map<String, Long> counterMap;

  public Communication() {
    init();
  }

  private void init() {
    this.counterMap = new ConcurrentHashMap<>();
  }

  public void setCounterVal(String key, long val) {
    counterMap.put(key, val);
  }

  public long getCounterVal(String key) {
    Long val = counterMap.get(key);
    if (null == val) {
      return 0;
    }
    return val;
  }
}
