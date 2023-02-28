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
package io.datavines.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * failure policy when some task node failed.
 */
public enum TimeoutStrategy{

  /**
   * 0 ending process when some tasks failed.
   * 1 continue running when some tasks failed.
   **/
  RETRY(0, "retry"),
  WARN(1, "warn");

  TimeoutStrategy(int code, String description){
    this.code = code;
    this.description = description;
  }

  @EnumValue
  final int code;

  final String description;

  public int getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }
}
