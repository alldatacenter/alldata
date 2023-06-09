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

import com.mongodb.client.MongoCollection;
import org.apache.drill.metastore.mongo.exception.MongoMetastoreException;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 * Mongo delete operation: deletes documents based on given row filter.
 */
public class MongoDelete implements MongoOperation {

  private final Bson filter;

  public MongoDelete(Bson filter) {
    this.filter = filter;
  }

  public Bson filter() {
    return filter;
  }

  @Override
  public void execute(MongoCollection<Document> collection) {
    try {
      collection.deleteMany(filter);
    } catch (Exception e) {
      throw new MongoMetastoreException(String.format("failed to delete by %s" +
        " from %s", filter.toString(), collection.getNamespace()), e);
    }
  }
}
