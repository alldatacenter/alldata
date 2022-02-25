/**
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
package org.apache.ambari.spi.exceptions;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.spi.upgrade.UpgradeAction;

/**
 * An exception thrown by {@link UpgradeAction}s when they are unable to
 * execute.
 */
public class UpgradeActionException extends AmbariException {

  /**
   * The failed class.
   */
  private final Class<? extends UpgradeAction> m_failedClass;

  /**
   * Constructor.
   *
   * @param failedClass
   *          the failed {@link UpgradeAction}.
   * @param message
   *          the failure message.
   */
  public UpgradeActionException(Class<? extends UpgradeAction> failedClass, String message) {
    super(message);
    m_failedClass = failedClass;
  }

  /**
   * Constructor.
   *
   * @param failedClass
   *          the failed {@link UpgradeAction}.
   * @param message
   *          the failure message.
   * @param cause
   *          the underlying exception
   */
  public UpgradeActionException(Class<? extends UpgradeAction> failedClass, String message, Throwable cause) {
    super(message, cause);
    m_failedClass = failedClass;
  }

  /**
   * Gets the class of the failed upgrade action.
   *
   * @return the failed {@link UpgradeAction}.
   */
  public Class<? extends UpgradeAction> getFailedClass() {
    return m_failedClass;
  }
}
