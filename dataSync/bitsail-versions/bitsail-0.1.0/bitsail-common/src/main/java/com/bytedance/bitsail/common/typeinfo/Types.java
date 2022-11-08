/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.bytedance.bitsail.common.typeinfo;

public enum Types {
  VOID,
  SHORT,
  INT,
  LONG,
  /**
   * Same with {@link  Types#LONG}, for history compatible.
   */
  BIGINT,
  FLOAT,
  DOUBLE,
  BIGDECIMAL,
  BIGINTEGER,
  BYTE,
  BINARY,
  DATE,
  TIME,
  TIMESTAMP,
  BOOLEAN,
  STRING,
  LIST,
  MAP,
  /**
   * Same with {@link Types#BINARY}, for history compatible.
   */
  @Deprecated
  BYTES,
  /**
   * DATE_TIME & DATE_DATE & DATE_DATE_TIME will be deprecated in the future.
   */
  @Deprecated
  DATE_DATE {
    @Override
    public String getTypeStringNickName() {
      return "date.date";
    }
  },

  @Deprecated
  DATE_TIME {
    @Override
    public String getTypeStringNickName() {
      return "date.time";
    }
  },

  @Deprecated
  DATE_DATE_TIME {
    @Override
    public String getTypeStringNickName() {
      return "date.datetime";
    }
  };

  public String getTypeStringNickName() {
    return null;
  }
}
