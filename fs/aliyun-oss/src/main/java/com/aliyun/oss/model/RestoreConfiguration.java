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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.model;

public class RestoreConfiguration {
    /**
     * How many days will it stay in retrievable state after restore done.
     */
    private Integer days;

    /**
     * The restore parameters.
     */
    private RestoreJobParameters restoreJobParameters;

    public RestoreConfiguration(Integer days) {
        this.days = days;
    }

    public RestoreConfiguration(Integer days, RestoreJobParameters restoreJobParameters) {
        this.days = days;
        this.restoreJobParameters = restoreJobParameters;
    }


    /**
     * Gets the days that it will stay in retrievable state after restore done.
     * @return  the duration of the restored state
     */
    public Integer getDays() {
        return days;
    }

    /**
     * Sets the days that it will stay in retrievable state after restore done.
     *
     * @param days
     *            days that it will stay in retrievable state after restore done.
     */
    public void setDays(Integer days) {
        this.days = days;
    }

    /**
     * Gets the ColdArchive object restore job parameters.
     *
     * @return  The {@link RestoreJobParameters} instance.
     */
    public RestoreJobParameters getRestoreJobParameters() {
        return restoreJobParameters;
    }

    /**
     * Sets the ColdArchive object restore job parameters.
     *
     * @param restoreJobParameters
     *            The {@link RestoreJobParameters} instance.
     */
    public void setRestoreJobParameters(RestoreJobParameters restoreJobParameters) {
        this.restoreJobParameters = restoreJobParameters;
    }

    /**
     * Sets the restore job parameters and return the {@link RestoreConfiguration} object itself.
     *
     * @param restoreJobParameters
     *            The {@link RestoreJobParameters} instance.
     *
     * @return  The {@link RestoreConfiguration} instance.
     */
    public RestoreConfiguration withRestoreJobParameters(RestoreJobParameters restoreJobParameters) {
        setRestoreJobParameters(restoreJobParameters);
        return this;
    }
}
