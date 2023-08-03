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
package org.apache.seatunnel.app.bean.env;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.api.env.EnvOptionRule;
import org.apache.seatunnel.app.dynamicforms.AbstractFormOption;
import org.apache.seatunnel.app.dynamicforms.FormStructure;
import org.apache.seatunnel.app.thirdparty.framework.SeaTunnelOptionRuleWrapper;

import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Data
public class JobEnvCache {

    @Getter private final FormStructure envFormStructure;

    public JobEnvCache() {
        OptionRule envOptionRules = EnvOptionRule.getEnvOptionRules();
        envFormStructure =
                SeaTunnelOptionRuleWrapper.wrapper(
                        envOptionRules.getOptionalOptions(),
                        envOptionRules.getRequiredOptions(),
                        "Env");
        List<AbstractFormOption> collect =
                envFormStructure.getForms().stream()
                        .filter(form -> !"parallelism".equalsIgnoreCase(form.getField()))
                        .collect(Collectors.toList());
        envFormStructure.setForms(collect);
    }
}
