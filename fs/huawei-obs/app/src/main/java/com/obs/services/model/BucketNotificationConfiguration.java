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

import java.util.ArrayList;
import java.util.List;

/**
 * Bucket notification configuration
 *
 */
public class BucketNotificationConfiguration extends HeaderResponse {
    private List<TopicConfiguration> topicConfigurations;
    private List<FunctionGraphConfiguration> functionGraphConfigurations;

    /**
     * Add event notification configuration.
     * 
     * @param topicConfiguration
     *            Event notification configuration
     * @return Bucket notification configuration
     */
    public BucketNotificationConfiguration addTopicConfiguration(TopicConfiguration topicConfiguration) {
        this.getTopicConfigurations().add(topicConfiguration);
        return this;
    }

    /**
     * Add FunctionGraph notification configuration.
     * 
     * @param functionGraphConfiguration
     *            FunctionGraph notification configuration
     * @return Event notification configuration of the bucket
     */
    public BucketNotificationConfiguration addFunctionGraphConfiguration(
            FunctionGraphConfiguration functionGraphConfiguration) {
        this.getFunctionGraphConfigurations().add(functionGraphConfiguration);
        return this;
    }

    /**
     * Obtain the list of event notification configurations
     * 
     * @return List of event notification configurations
     */
    public List<TopicConfiguration> getTopicConfigurations() {
        if (this.topicConfigurations == null) {
            this.topicConfigurations = new ArrayList<TopicConfiguration>();
        }
        return topicConfigurations;
    }

    /**
     * Obtain the list of FunctionGraph notification configurations
     * 
     * @return List of FunctionGraph notification configurations
     */
    public List<FunctionGraphConfiguration> getFunctionGraphConfigurations() {
        if (this.functionGraphConfigurations == null) {
            this.functionGraphConfigurations = new ArrayList<FunctionGraphConfiguration>();
        }
        return functionGraphConfigurations;
    }

    /**
     * Configure event notification.
     * 
     * @param topicConfigurations
     *            Event notification configuration list
     */
    public void setTopicConfigurations(List<TopicConfiguration> topicConfigurations) {
        this.topicConfigurations = topicConfigurations;
    }

    /**
     * Set the list of FunctionGraph notification configurations.
     * 
     * @param functionGraphConfigurations
     *            List of FunctionGraph notification configurations
     */
    public void setFunctionGraphConfigurations(List<FunctionGraphConfiguration> functionGraphConfigurations) {
        this.functionGraphConfigurations = functionGraphConfigurations;
    }

    @Override
    public String toString() {
        return "BucketNotificationConfiguration [topicConfigurations=" + topicConfigurations
                + ", functionGraphConfigurations=" + functionGraphConfigurations + "]";
    }

}
