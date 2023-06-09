package org.apache.drill.exec.planner.cost;

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


import org.apache.drill.exec.physical.base.GroupScan;

/**
 * PluginCost describes the cost factors to be used when costing for the specific storage/format plugin
 */
public interface PluginCost {
  org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PluginCost.class);

  /**
   * An interface to check if a parameter provided by user is valid or not.
   * @param <T> Type of the parameter.
   */
  interface CheckValid<T> {
    boolean isValid(T paramValue);
  }

  /**
   * Class which checks whether the provided parameter value is greater than
   * or equals to a minimum limit.
   */
  class greaterThanEquals implements CheckValid<Integer> {
    private final Integer atleastEqualsTo;
    public greaterThanEquals(Integer atleast) {
      atleastEqualsTo = atleast;
    }

    @Override
    public boolean isValid(Integer paramValue) {
      if (paramValue >= atleastEqualsTo &&
          paramValue <= Integer.MAX_VALUE) {
        return true;
      } else {
        logger.warn("Setting default value as the supplied parameter value is less than {}", paramValue);
        return false;
      }
    }
  }

  /**
   * @return the average column size in bytes
   */
  int getAverageColumnSize(GroupScan scan);

  /**
   * @return the block size in bytes
   */
  int getBlockSize(GroupScan scan);

  /**
   * @return the sequential block read cost
   */
  int getSequentialBlockReadCost(GroupScan scan);

  /**
   * @return the random block read cost
   */
  int getRandomBlockReadCost(GroupScan scan);
}
