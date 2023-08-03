/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark;

import com.netease.arctic.spark.util.ExpressionHelper;

/**
 * This interface will provider some util or helper object to shield api differences in different spark versions.
 */
public interface SparkAdapter {

  /**
   * A helper object to help build spark expressions
   * {@link org.apache.spark.sql.connector.expressions.Expression},
   * and provider a covert method to help covert {@link org.apache.spark.sql.connector.expressions.Expression}
   * to {@link org.apache.spark.sql.catalyst.expressions.Expression}
   *
   * @return expression helper object
   */
  ExpressionHelper expressions();
}
