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

package org.apache.inlong.manager.pojo.group;

import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;

import java.util.List;

/**
 * String utils for inlong.
 */
public class InlongGroupUtils {

    /**
     * Check if group is batch task.
     *
     * @param groupInfo
     * @return
     */
    public static boolean isBatchTask(InlongGroupInfo groupInfo) {
        if (groupInfo == null || CollectionUtils.isEmpty(groupInfo.getExtList())) {
            return false;
        }
        List<InlongGroupExtInfo> extInfos = groupInfo.getExtList();
        for (InlongGroupExtInfo extInfo : extInfos) {
            if (InlongConstants.BATCH_TASK.equals(extInfo.getKeyName())) {
                return true;
            }
        }
        return false;
    }

}
