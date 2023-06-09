/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.easy.json.extended;

import org.apache.drill.exec.vector.complex.fn.ExtendedTypeName;

/**
 * Names of Mongo extended types. Includes both
 * <a href="https://docs.mongodb.com/manual/reference/mongodb-extended-json-v1/">
 * V1</a> and
 * <a href="https://docs.mongodb.com/manual/reference/mongodb-extended-json/">
 * V2</a> names.
 *
 * @see org.apache.drill.exec.vector.complex.fn.ExtendedTypeName ExtendedTypeName
 * for an older version of these names
 */
public interface ExtendedTypeNames {

  // Supported Mongo types
  String TYPE_PREFIX = "$";
  String LONG = "$numberLong";
  String DECIMAL = "$numberDecimal";
  String DOUBLE = "$numberDouble";
  String INT = "$numberInt";
  String DATE = "$date";
  String BINARY = "$binary";
  String OBJECT_ID = "$oid";

  // Drill extensions
  String DATE_DAY = ExtendedTypeName.DATE;
  String TIME = ExtendedTypeName.TIME;
  String INTERVAL = ExtendedTypeName.INTERVAL;

  // The V1 JSON reader allows binary of format {"$type": 1, $binary: "..."}
  String BINARY_TYPE = ExtendedTypeName.TYPE;
}
