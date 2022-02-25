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
package org.apache.ambari.server.controller.spi;

/**
 * Encapsulates a property in a Sort Request.
 */
public class SortRequestProperty {
  private final String propertyId;
  private final SortRequest.Order order;

  public SortRequestProperty(String propertyId, SortRequest.Order order) {
    this.propertyId = propertyId;
    if (order == null) {
      this.order = SortRequest.Order.ASC;
    } else {
      this.order = order;
    }
  }

  public String getPropertyId() {
    return propertyId;
  }

  public  SortRequest.Order getOrder() {
    return order;
  }

  @Override
  public String toString() {
    return "SortRequestProperty{" +
      "propertyId='" + propertyId + '\'' +
      ", order=" + order +
      '}';
  }
}
