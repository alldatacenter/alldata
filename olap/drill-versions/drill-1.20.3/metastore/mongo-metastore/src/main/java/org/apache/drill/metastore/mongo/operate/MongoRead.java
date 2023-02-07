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
package org.apache.drill.metastore.mongo.operate;

import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.mongo.MongoMetastoreContext;
import org.apache.drill.metastore.mongo.transform.FilterTransformer;
import org.apache.drill.metastore.operate.AbstractRead;
import org.apache.drill.metastore.operate.MetadataTypeValidator;
import org.apache.drill.metastore.operate.Read;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link Read} interface based on {@link AbstractRead} parent class.
 * Reads information from Mongo collection based on given filter expression.
 * Supports reading information for specific columns.
 *
 * @param <T> Metastore component unit type
 */
public class MongoRead<T> extends AbstractRead<T> {

  private final MongoMetastoreContext<T> context;

  public MongoRead(MetadataTypeValidator metadataTypeValidator, MongoMetastoreContext<T> context) {
    super(metadataTypeValidator);
    this.context = context;
  }

  @Override
  protected List<T> internalExecute() {
    FilterTransformer filterTransformer = context.transformer().filter();
    Bson rowFilter = filterTransformer.combine(
      filterTransformer.transform(metadataTypes), filterTransformer.transform(filter));
    List<Document> documents = Lists.newLinkedList();
    context.table().find(rowFilter).forEach(documents::add);
    return context.transformer().outputData()
      .columns(columns.stream().map(MetastoreColumn::columnName).collect(Collectors.toList()))
      .documents(documents)
      .execute();
  }
}
