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
package org.apache.drill.exec.server;

import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.glassfish.hk2.api.Factory;

public class DrillClientFactory implements Factory<DrillClient> {
//  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DrillClientFactory.class);

  @Override
  public void dispose(DrillClient arg0) {
  }

  @Override
  public DrillClient provide() {
    try {
      return new DrillClient();
    } catch(OutOfMemoryException e) {
      throw new RuntimeException(e);
    }
  }
}
