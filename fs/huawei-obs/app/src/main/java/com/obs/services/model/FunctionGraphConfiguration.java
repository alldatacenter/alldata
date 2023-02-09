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

import java.util.List;

/**
 * FunctionGraph event notification configuration
 *
 */
public class FunctionGraphConfiguration extends AbstractNotification {

    private String functionGraph;

    public FunctionGraphConfiguration() {

    }

    /**
     * Constructor
     * 
     * @param id
     *            Event notification configuration ID
     * @param filter
     *            Filtering rules
     * @param functionGraph
     *            FunctionGraph URN
     * @param events
     *            List of event types that need to be notified
     */
    public FunctionGraphConfiguration(String id, Filter filter, String functionGraph, List<EventTypeEnum> events) {
        super(id, filter, events);
        this.functionGraph = functionGraph;

    }

    /**
     * Obtain the FunctionGraph URN.
     * 
     * @return FunctionGraph URN
     */
    public String getFunctionGraph() {
        return functionGraph;
    }

    /**
     * Set the FunctionGraph URN.
     * 
     * @param functionGraph
     *            FunctionGraph URN
     */
    public void setFunctionGraph(String functionGraph) {
        this.functionGraph = functionGraph;
    }

    @Override
    public String toString() {
        return "FunctionGraphConfiguration [id=" + id + ", functionGraph=" + functionGraph + ", events=" + events
                + ", filter=" + filter + "]";
    }

}
