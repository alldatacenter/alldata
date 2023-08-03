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

package org.apache.seatunnel.app.thirdparty.transfrom;

import org.apache.seatunnel.shade.com.typesafe.config.Config;
import org.apache.seatunnel.shade.com.typesafe.config.ConfigFactory;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.app.domain.request.job.TableSchemaReq;
import org.apache.seatunnel.app.domain.request.job.transform.Transform;
import org.apache.seatunnel.app.domain.request.job.transform.TransformOptions;
import org.apache.seatunnel.app.dynamicforms.FormStructure;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

public class TransformConfigSwitcherUtils {

    public static FormStructure getFormStructure(
            Transform transform, OptionRule transformOptionRule) {

        return TransformConfigSwitcherProvider.INSTANCE
                .getTransformConfigSwitcher(transform)
                .getFormStructure(transformOptionRule);
    }

    public static Config mergeTransformConfig(
            Transform transform,
            List<TableSchemaReq> inputSchemas,
            Config TransformConfig,
            TransformOptions transformOption) {

        checkArgument(inputSchemas.size() == 1, "transformSwitcher only support one input table");

        return TransformConfigSwitcherProvider.INSTANCE
                .getTransformConfigSwitcher(transform)
                .mergeTransformConfig(TransformConfig, transformOption, inputSchemas.get(0));
    }

    public static Config getOrderedConfigForLinkedHashMap(LinkedHashMap<String, String> map) {
        StringBuilder sb = new StringBuilder();
        sb.append(" {\n");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append("    ")
                    .append("\"")
                    .append(entry.getKey())
                    .append("\"")
                    .append("=")
                    .append("\"")
                    .append(entry.getValue())
                    .append("\"")
                    .append("\n");
        }
        sb.append("}");

        return ConfigFactory.parseString(sb.toString());
    }
}
