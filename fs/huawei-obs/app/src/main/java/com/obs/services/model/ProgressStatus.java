/**
* Copyright 2019 Huawei Technologies Co.,Ltd.
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use
* this file except in compliance with the License.  You may obtain a copy of the
* License at
* 
* http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software distributed
* under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations under the License.
**/

package com.obs.services.model;

/**
 * Data transfer status
 *
 */
public interface ProgressStatus {

    /**
     * Obtain the instantaneous speed.
     * 
     * @return Instantaneous speed
     */
    public double getInstantaneousSpeed();

    /**
     * Obtain the average speed.
     * 
     * @return Average speed
     */
    public double getAverageSpeed();

    /**
     * Obtain the transfer progress
     * 
     * @return Transfer progress
     */
    public int getTransferPercentage();

    /**
     * Obtain the number of new bytes.
     * 
     * @return Number of bytes transferred since last progress refresh
     */
    public long getNewlyTransferredBytes();

    /**
     * Obtain the number of transferred bytes.
     * 
     * @return Number of bytes that have been transferred
     */
    public long getTransferredBytes();

    /**
     * Obtain the number of bytes to be transferred.
     * 
     * @return Number of the total bytes to be transferred
     */
    public long getTotalBytes();
}
