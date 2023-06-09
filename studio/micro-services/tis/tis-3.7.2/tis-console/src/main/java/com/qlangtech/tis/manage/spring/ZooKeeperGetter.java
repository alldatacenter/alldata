/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.manage.spring;

import com.qlangtech.tis.cloud.ITISCoordinator;
import com.qlangtech.tis.pubhook.common.RunEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-8-15
 */
public class ZooKeeperGetter extends EnvironmentBindService<ITISCoordinator> {

  private static final Logger log = LoggerFactory.getLogger(ZooKeeperGetter.class);

  private void validateMultiServerIsReachable(final String zkAddress) {
    Matcher matcher = ZK_ADDRESS.matcher(zkAddress);
    while (matcher.find()) {
      validateServerIsReachable(matcher.group(1));
    }
  }

  @Override
  protected ITISCoordinator createSerivce(final RunEnvironment runtime) {
    return ITISCoordinator.create();

//        final String zkAddress = Config.getZKHost();
//        validateMultiServerIsReachable(zkAddress);
//        try {
//            log.debug("runtime:" + runtime + ", address:" + zkAddress + " rmi server connection has been established");
//            // try {
//            final TisZkClient target = new TisZkClient(zkAddress, 30000);
//            return target;
//        } catch (Exception e) {
//            // }
//            throw new RuntimeException(e.getMessage(), e);
//        }
  }
}
