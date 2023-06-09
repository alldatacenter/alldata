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
package org.apache.drill.common.logical;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Interface for defining a Drill format plugin.
 *
 * Format plugins are contained within storage plugins to describe particular
 * data formats available in a given storage system. These formats are not
 * necessarily tied to how the data is stored. One current use of this
 * abstraction is describing different file formats like CSV and JSON that
 * can be stored in a filesystem. Some storage systems like databases may only
 * have a single format in which the data is actually stored. This interface
 * enables flexibility for configuring the different data formats that will
 * live under one storage system. The storage systems themselves are described
 * in {@see StoragePluginConfig}s.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface FormatPluginConfig {
}
