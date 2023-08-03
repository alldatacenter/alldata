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

package org.apache.seatunnel.app.dal.entity;

import org.apache.seatunnel.app.parameters.DependentParameters;
import org.apache.seatunnel.app.parameters.SubProcessParameters;
import org.apache.seatunnel.app.utils.JSONUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.apache.seatunnel.app.common.Constants.CMD_PARAM_SUB_PROCESS_DEFINE_CODE;

public class TaskDefinitionExpand extends TaskDefinition {

    public SubProcessParameters getSubProcessParameters() {
        String parameter = super.getTaskParams();
        ObjectNode parameterJson = JSONUtils.parseObject(parameter);
        if (parameterJson.get(CMD_PARAM_SUB_PROCESS_DEFINE_CODE) != null) {
            return JSONUtils.parseObject(parameter, SubProcessParameters.class);
        }
        return null;
    }

    public DependentParameters getDependentParameters() {
        return JSONUtils.parseObject(super.getDependence(), DependentParameters.class);
    }
}
