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

package com.netease.arctic.flink.util;

import org.apache.flink.api.connector.source.ReaderOutput;
import org.apache.flink.streaming.api.operators.source.ProgressiveTimestampsAndWatermarks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.netease.arctic.flink.util.ReflectionUtil.getField;

/**
 * A util class to handle the reflection operation of Flink class.
 */
public class FlinkClassReflectionUtil {

  public static final Logger LOG = LoggerFactory.getLogger(FlinkClassReflectionUtil.class);

  public static Object getSplitLocalOutput(ReaderOutput readerOutput) {
    if (readerOutput == null) {
      return null;
    }
    try {
      return getField(
          (Class<ReaderOutput>) ProgressiveTimestampsAndWatermarks.class.getDeclaredClasses()[1],
          readerOutput, "splitLocalOutputs");
    } catch (Exception e) {
      LOG.warn("extract internal watermark error", e);
    }
    return null;
  }

  public static void emitPeriodWatermark(@Nullable Object splitLocalOutput) {
    if (splitLocalOutput == null) {
      return;
    }
    try {
      Method method = ProgressiveTimestampsAndWatermarks.class.getDeclaredClasses()[0]
          .getDeclaredMethod("emitPeriodicWatermark");
      method.setAccessible(true);
      method.invoke(splitLocalOutput);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      LOG.warn("no method found", e);
    }
  }

}
