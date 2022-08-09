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

package org.apache.ambari.server.api.services.serializers;


import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;

/**
 * Format internal result to format expected by client.
 */
public interface ResultSerializer {
  /**
   * Serialize the given result to a format expected by client.
   *
   * @param result  internal result
   *
   * @return the serialized result
   */
  Object serialize(Result result);

  /**
   * Serialize an error result to the format expected by the client.
   *
   * @param error  the error result
   *
   * @return the serialized error result
   */
  Object serializeError(ResultStatus error);
}
