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

package com.bytedance.bitsail.shaded.hive.shim;

import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.IMetaStoreClient;
import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.Writable;

import javax.annotation.Nullable;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * A shim layer to support different versions of Hive.
 */
public interface HiveShim extends Serializable {
  /**
   * Create a Hive Metastore client based on the given HiveConf object.
   *
   * @param hiveConf HiveConf instance
   * @return an IMetaStoreClient instance
   */
  IMetaStoreClient getHiveMetastoreClient(HiveConf hiveConf);

  /**
   * Converts a Hive primitive java object to corresponding Writable object.
   */
  @Nullable
  Writable hivePrimitiveToWritable(@Nullable Object value);

  /**
   * Hive Date data type class was changed in Hive 3.1.0.
   *
   * @return Hive's Date class
   */
  Class<?> getDateDataTypeClass();

  /**
   * Hive Timestamp data type class was changed in Hive 3.1.0.
   *
   * @return Hive's Timestamp class
   */
  Class<?> getTimestampDataTypeClass();

  /**
   * Get the class of Hive's MetaStoreUtils because its package name was changed in Hive 3.1.0.
   *
   * @return MetaStoreUtils class
   */
  Class<?> getMetaStoreUtilsClass();

  /**
   * Converts a hive date instance to  which is expected by .
   */
  Date toDate(Object hiveDate);

  /**
   * Converts a date instance to what's expected by Hive.
   */
  Object toHiveDate(Object value);

  /**
   * Converts a date instance to writable what's expected by Hive.
   */
  Writable toHiveDateWritable(Date value);

  /**
   * Converts a hive timestamp instance to milliseconds timestamp.
   */
  Timestamp toTimestamp(Object hiveTimestamp);

  /**
   * Converts a timestamp instance to what's expected by Hive.
   */
  Object toHiveTimestamp(Object time);

  /**
   * Converts a timestamp instance to writable what's expected by Hive.
   */
  Writable toHiveTimestampWritable(Timestamp time);

  /**
   * get writable of list type
   *
   * @param writables element writables
   * @return arrayWritable
   */
  ArrayWritable getArrayWritable(Writable[] writables);

  /**
   * get complex type from ArrayWritable
   *
   * @param writables element writables
   * @return arrayWritable
   */
  ArrayWritable getComplexValueFromArrayWritable(ArrayWritable writables);

  /**
   * Method for get hive.
   */
  String getVersion();

}
