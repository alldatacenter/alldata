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
package org.apache.ambari.server.stack.upgrade;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

/**
 * Operations valid for a property transfer.
 */
@XmlEnum
public enum TransferOperation {
  /**
   * The property should be removed.  Special case "*" to delete all
   * properties that have NOT been overridden by a user.
   */
  @XmlEnumValue("delete")
  DELETE,
  /**
   * The property value is moved.  A property may only be moved within the
   * same config type.  To move across types, perform a {@link #COPY} to the target then
   * a {@link #DELETE} from the source config.
   */
  @XmlEnumValue("move")
  MOVE,
  /**
   * The property value is copied from another property.  A property may be copied from
   * another config type.
   */
  @XmlEnumValue("copy")
  COPY
}
