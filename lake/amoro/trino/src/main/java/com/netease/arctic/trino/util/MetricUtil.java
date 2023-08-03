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

package com.netease.arctic.trino.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * tools to metric run time
 */
public class MetricUtil {

  private static final Logger LOG = LoggerFactory.getLogger(MetricUtil.class);

  public static <R> R duration(Supplier<R> supplier, String name) {
    long t1 = System.currentTimeMillis();
    R r = supplier.get();
    long t2 = System.currentTimeMillis();
    LOG.info("{} code duration is {}ms", name, t2 - t1);
    return r;
  }

  public static void duration(Runnable runnable, String name) {
    long t1 = System.currentTimeMillis();
    runnable.run();
    ;
    long t2 = System.currentTimeMillis();
    LOG.info("{} code duration is {}ms", name, t2 - t1);
  }
}
