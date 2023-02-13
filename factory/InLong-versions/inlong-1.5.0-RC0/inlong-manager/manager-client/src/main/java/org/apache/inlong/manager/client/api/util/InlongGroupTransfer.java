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

package org.apache.inlong.manager.client.api.util;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.auth.Authentication;
import org.apache.inlong.manager.common.auth.Authentication.AuthType;
import org.apache.inlong.manager.common.auth.SecretTokenAuthentication;
import org.apache.inlong.manager.common.auth.TokenAuthentication;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.group.InlongGroupExtInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.sort.BaseSortConf;
import org.apache.inlong.manager.pojo.sort.BaseSortConf.SortType;
import org.apache.inlong.manager.pojo.sort.FlinkSortConf;
import org.apache.inlong.manager.pojo.sort.UserDefinedSortConf;

import java.util.ArrayList;
import java.util.List;

/**
 * The transfer util for Inlong Group
 */
public class InlongGroupTransfer {

    /**
     * Create inlong group info from group config.
     */
    public static InlongGroupInfo createGroupInfo(InlongGroupInfo groupInfo, BaseSortConf sortConf) {
        Preconditions.checkNotNull(groupInfo, "Inlong group info cannot be null");
        String groupId = groupInfo.getInlongGroupId();
        Preconditions.checkNotEmpty(groupId, "groupId cannot be empty");
        // init extensions
        if (groupInfo.getExtList() == null) {
            groupInfo.setExtList(new ArrayList<>());
        }
        // set authentication into group ext list
        List<InlongGroupExtInfo> extInfos = new ArrayList<>();
        if (groupInfo.getAuthentication() != null) {
            Authentication authentication = groupInfo.getAuthentication();
            AuthType authType = authentication.getAuthType();
            Preconditions.checkTrue(authType == AuthType.TOKEN,
                    String.format("Unsupported authentication %s for Pulsar", authType.name()));
            TokenAuthentication tokenAuthentication = (TokenAuthentication) authentication;
            InlongGroupExtInfo authTypeExt = new InlongGroupExtInfo();
            authTypeExt.setKeyName(InlongConstants.PULSAR_AUTHENTICATION_TYPE);
            authTypeExt.setKeyValue(tokenAuthentication.getAuthType().toString());
            extInfos.add(authTypeExt);

            InlongGroupExtInfo authValue = new InlongGroupExtInfo();
            authValue.setKeyName(InlongConstants.PULSAR_AUTHENTICATION);
            authValue.setKeyValue(tokenAuthentication.getToken());
            extInfos.add(authValue);

            groupInfo.getExtList().addAll(extInfos);
        }

        if (sortConf == null) {
            throw new IllegalArgumentException("sort config cannot be empty for group=" + groupId);
        }
        // set the sort config into ext list
        SortType sortType = sortConf.getType();
        List<InlongGroupExtInfo> sortExtInfos;
        if (sortType == SortType.FLINK) {
            FlinkSortConf flinkSortConf = (FlinkSortConf) sortConf;
            sortExtInfos = createFlinkExtInfo(flinkSortConf);
        } else if (sortType == SortType.USER_DEFINED) {
            UserDefinedSortConf udf = (UserDefinedSortConf) sortConf;
            sortExtInfos = createUserDefinedSortExtInfo(udf);
        } else {
            // todo local
            sortExtInfos = new ArrayList<>();
        }

        groupInfo.getExtList().addAll(sortExtInfos);
        return groupInfo;
    }

    /**
     * Get ext infos from flink config
     */
    public static List<InlongGroupExtInfo> createFlinkExtInfo(FlinkSortConf flinkSortConf) {
        List<InlongGroupExtInfo> extInfos = new ArrayList<>();
        InlongGroupExtInfo sortType = new InlongGroupExtInfo();
        sortType.setKeyName(InlongConstants.SORT_TYPE);
        sortType.setKeyValue(SortType.FLINK.getType());
        extInfos.add(sortType);
        if (flinkSortConf.getAuthentication() != null) {
            Authentication authentication = flinkSortConf.getAuthentication();
            AuthType authType = authentication.getAuthType();
            Preconditions.checkTrue(authType == AuthType.SECRET_AND_TOKEN,
                    String.format("Unsupported authentication %s for Flink", authType.name()));
            final SecretTokenAuthentication secretTokenAuthentication = (SecretTokenAuthentication) authentication;
            InlongGroupExtInfo authTypeExt = new InlongGroupExtInfo();
            authTypeExt.setKeyName(InlongConstants.SORT_AUTHENTICATION_TYPE);
            authTypeExt.setKeyValue(authType.toString());
            extInfos.add(authTypeExt);
            InlongGroupExtInfo authValue = new InlongGroupExtInfo();
            authValue.setKeyName(InlongConstants.SORT_AUTHENTICATION);
            authValue.setKeyValue(secretTokenAuthentication.toString());
            extInfos.add(authValue);
        }
        if (StringUtils.isNotEmpty(flinkSortConf.getServiceUrl())) {
            InlongGroupExtInfo flinkUrl = new InlongGroupExtInfo();
            flinkUrl.setKeyName(InlongConstants.SORT_URL);
            flinkUrl.setKeyValue(flinkSortConf.getServiceUrl());
            extInfos.add(flinkUrl);
        }
        if (MapUtils.isNotEmpty(flinkSortConf.getProperties())) {
            InlongGroupExtInfo flinkProperties = new InlongGroupExtInfo();
            flinkProperties.setKeyName(InlongConstants.SORT_PROPERTIES);
            try {
                flinkProperties.setKeyValue(JsonUtils.toJsonString(flinkSortConf.getProperties()));
            } catch (Exception e) {
                throw new RuntimeException("get json for sort properties error: " + e.getMessage());
            }
            extInfos.add(flinkProperties);
        }
        return extInfos;
    }

    /**
     * Get ext infos from user defined sort config
     */
    public static List<InlongGroupExtInfo> createUserDefinedSortExtInfo(UserDefinedSortConf userDefinedSortConf) {
        List<InlongGroupExtInfo> extInfos = new ArrayList<>();
        InlongGroupExtInfo sortType = new InlongGroupExtInfo();
        sortType.setKeyName(InlongConstants.SORT_TYPE);
        sortType.setKeyValue(SortType.USER_DEFINED.getType());
        extInfos.add(sortType);
        InlongGroupExtInfo sortName = new InlongGroupExtInfo();
        sortName.setKeyName(InlongConstants.SORT_NAME);
        sortName.setKeyValue(userDefinedSortConf.getSortName());
        extInfos.add(sortName);
        if (MapUtils.isNotEmpty(userDefinedSortConf.getProperties())) {
            InlongGroupExtInfo flinkProperties = new InlongGroupExtInfo();
            flinkProperties.setKeyName(InlongConstants.SORT_PROPERTIES);
            try {
                flinkProperties.setKeyValue(JsonUtils.toJsonString(userDefinedSortConf.getProperties()));
            } catch (Exception e) {
                throw new RuntimeException("get json for sort properties error: " + e.getMessage());
            }
            extInfos.add(flinkProperties);
        }
        return extInfos;
    }

}
