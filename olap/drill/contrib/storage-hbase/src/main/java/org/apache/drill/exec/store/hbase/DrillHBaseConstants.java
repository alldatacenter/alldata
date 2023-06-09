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
package org.apache.drill.exec.store.hbase;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;

public interface DrillHBaseConstants {
  String ROW_KEY = "row_key";

  SchemaPath ROW_KEY_PATH = SchemaPath.getSimplePath(ROW_KEY);

  String HBASE_ZOOKEEPER_PORT = "hbase.zookeeper.property.clientPort";

  MajorType ROW_KEY_TYPE = Types.required(MinorType.VARBINARY);

  MajorType COLUMN_FAMILY_TYPE = Types.required(MinorType.MAP);

  MajorType COLUMN_TYPE = Types.optional(MinorType.VARBINARY);

  String SYS_STORE_PROVIDER_HBASE_TABLE = "drill.exec.sys.store.provider.hbase.table";

  String SYS_STORE_PROVIDER_HBASE_CONFIG = "drill.exec.sys.store.provider.hbase.config";
}
