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
package org.apache.drill;

import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Type.Repetition;

public class ParquetSchemaMerge {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ParquetSchemaMerge.class);

  public static void main(String[] args) {
    MessageType message1;
    MessageType message2;

    PrimitiveType c = new PrimitiveType(Repetition.OPTIONAL, PrimitiveTypeName.INT32, "c");
    GroupType b = new GroupType(Repetition.REQUIRED, "b");
    GroupType a = new GroupType(Repetition.OPTIONAL, "a", b);
    message1 = new MessageType("root", a);

    PrimitiveType c2 = new PrimitiveType(Repetition.OPTIONAL, PrimitiveTypeName.INT32, "d");
    GroupType b2 = new GroupType(Repetition.OPTIONAL, "b", c2);
    GroupType a2 = new GroupType(Repetition.OPTIONAL, "a", b2);
    message2 = new MessageType("root", a2);

    MessageType message3 = message1.union(message2);

    StringBuilder builder = new StringBuilder();
    message3.writeToStringBuilder(builder, "");
    logger.info(builder.toString());
  }
}
