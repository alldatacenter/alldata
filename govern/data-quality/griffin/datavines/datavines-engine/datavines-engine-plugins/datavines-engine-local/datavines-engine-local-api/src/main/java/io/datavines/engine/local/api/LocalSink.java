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
package io.datavines.engine.local.api;

import io.datavines.common.config.Config;
import io.datavines.common.utils.StringUtils;
import io.datavines.engine.api.component.Component;
import io.datavines.engine.local.api.entity.ResultList;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static io.datavines.common.ConfigConstants.*;

public interface LocalSink extends Component {

    Logger log = LoggerFactory.getLogger(LocalSink.class);

    void output(List<ResultList> resultList, LocalRuntimeEnvironment env) throws Exception;

    default void setExceptedValue(Config config, List<ResultList> resultList, Map<String, String> inputParameter) {
        if (CollectionUtils.isNotEmpty(resultList)) {
            resultList.forEach(item -> {
                if(item != null) {
                    item.getResultList().forEach(x -> {
                        x.forEach((k,v) -> {
                            String expectedValue = config.getString(EXPECTED_VALUE);
                            if (StringUtils.isNotEmpty(expectedValue)) {
                                if (expectedValue.equals(k)) {
                                    inputParameter.put(EXPECTED_VALUE, String.valueOf(v));
                                }
                            }

                            inputParameter.put(k, String.valueOf(v));
                        });
                    });
                }
            });
        }
    }

}
