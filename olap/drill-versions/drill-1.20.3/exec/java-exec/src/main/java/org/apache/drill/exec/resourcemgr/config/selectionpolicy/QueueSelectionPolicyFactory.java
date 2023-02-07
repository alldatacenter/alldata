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
package org.apache.drill.exec.resourcemgr.config.selectionpolicy;

/**
 * Factory to return an instance of {@link QueueSelectionPolicy} based on the configured policy name. By default if
 * the configured policy name doesn't matches any supported policies then it returns {@link BestFitQueueSelection}
 */
public class QueueSelectionPolicyFactory {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(QueueSelectionPolicyFactory.class);

  public static QueueSelectionPolicy createSelectionPolicy(QueueSelectionPolicy.SelectionPolicy policy) {
    logger.debug("Creating SelectionPolicy of type {}", policy);
    QueueSelectionPolicy selectionPolicy;
    switch (policy) {
      case DEFAULT:
        selectionPolicy = new DefaultQueueSelection();
        break;
      case BESTFIT:
        selectionPolicy = new BestFitQueueSelection();
        break;
      case RANDOM:
        selectionPolicy = new RandomQueueSelection();
        break;
      default:
        logger.info("QueueSelectionPolicy is not configured so proceeding with the bestfit as default policy");
        selectionPolicy = new BestFitQueueSelection();
        break;
    }

    return selectionPolicy;
  }

  // prevents from instantiation
  private QueueSelectionPolicyFactory() {
    // no-op
  }
}
