/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.common.config;

public interface Reconfigurable {

  /**
   * The method use new configuration to reconfigure the component
   * @param conf means that new configuration after modification
   **/
  void reconfigure(RssConf conf);

  // todo: Mark the properties is reloadable or not in the ConfigOptionBuilder
  /**
   * This method judge whether the property could be reconfigurable or not.
   * @param property means that property name.
   * @return True means that the property could be reconfigurable.
   *         False means that the property couldn't be reconfigurable.
   **/
  boolean isPropertyReconfigurable(String property);

}
