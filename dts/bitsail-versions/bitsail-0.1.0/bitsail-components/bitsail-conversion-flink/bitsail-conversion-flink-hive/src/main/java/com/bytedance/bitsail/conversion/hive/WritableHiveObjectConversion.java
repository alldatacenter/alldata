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
 *
 * Original Files: apache/flink(https://github.com/apache/flink)
 * Copyright: Copyright 2014-2022 The Apache Software Foundation
 * SPDX-License-Identifier: Apache License 2.0
 *
 * This file may have been modified by ByteDance Ltd. and/or its affiliates.
 */

package com.bytedance.bitsail.conversion.hive;

import com.bytedance.bitsail.shaded.hive.shim.HiveShim;

/**
 * A HiveObjectConversion that converts Flink objects to Hive Writable objects.
 */
public class WritableHiveObjectConversion implements HiveObjectConversion {

  private static final long serialVersionUID = 1L;

  private final HiveObjectConversion flinkToJavaConversion;
  private final HiveShim hiveShim;

  WritableHiveObjectConversion(HiveObjectConversion flinkToJavaConversion, HiveShim hiveShim) {
    this.flinkToJavaConversion = flinkToJavaConversion;
    this.hiveShim = hiveShim;
  }

  @Override
  public Object toHiveObject(Object o) {
    return hiveShim.hivePrimitiveToWritable(flinkToJavaConversion.toHiveObject(o));
  }
}
