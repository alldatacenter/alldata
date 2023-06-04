/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.service.cluster;

import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Factory for {@link InlongClusterOperator}.
 */
@Service
public class InlongClusterOperatorFactory {

    @Autowired
    private List<InlongClusterOperator> clusterOperatorList;

    /**
     * Get a cluster operator instance via the given type
     */
    public InlongClusterOperator getInstance(String type) {
        return clusterOperatorList.stream()
                .filter(inst -> inst.accept(type))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCodeEnum.CLUSTER_TYPE_NOT_SUPPORTED,
                        String.format(ErrorCodeEnum.CLUSTER_TYPE_NOT_SUPPORTED.getMessage(), type)));
    }

}
