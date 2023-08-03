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

package org.apache.uniffle.common.rpc;

import org.apache.uniffle.proto.RssProtos;

public enum StatusCode {
  SUCCESS(0),
  DOUBLE_REGISTER(1),
  NO_BUFFER(2),
  INVALID_STORAGE(3),
  NO_REGISTER(4),
  NO_PARTITION(5),
  INTERNAL_ERROR(6),
  TIMEOUT(7),
  ACCESS_DENIED(8);

  private final int statusCode;

  StatusCode(int code) {
    this.statusCode = code;
  }

  public int statusCode() {
    return statusCode;
  }

  public RssProtos.StatusCode toProto() {
    RssProtos.StatusCode code = RssProtos.StatusCode.forNumber(this.statusCode());
    return code == null ? RssProtos.StatusCode.INTERNAL_ERROR : code;
  }
}
