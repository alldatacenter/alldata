/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.netease.arctic.flink.read.hybrid.reader;

import com.netease.arctic.flink.read.hybrid.split.ArcticSplit;
import org.apache.flink.connector.base.source.reader.RecordsWithSplitIds;
import org.apache.iceberg.io.CloseableIterator;

import java.io.Serializable;
import java.util.function.Function;

/**
 * This function that accepts one {@link ArcticSplit} and produces an iterator of {@link ArcticRecordWithOffset<T>}.
 */
@FunctionalInterface
public interface ReaderFunction<T> extends Serializable,
    Function<ArcticSplit, CloseableIterator<RecordsWithSplitIds<ArcticRecordWithOffset<T>>>> {
}
