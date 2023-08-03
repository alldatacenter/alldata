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
package io.datavines.notification.plugin.lark;

import com.google.common.collect.Lists;
import io.datavines.common.utils.HttpUtils;
import io.datavines.common.utils.JSONUtils;
import io.datavines.notification.api.entity.SlaNotificationResultRecord;
import io.datavines.notification.api.entity.SlaSenderMessage;
import io.datavines.notification.plugin.lark.entity.GroupCardDTO;
import io.datavines.notification.plugin.lark.entity.MessageDTO;
import io.datavines.notification.plugin.lark.entity.ReceiverConfig;
import io.datavines.notification.plugin.lark.util.ContentUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Slf4j
@EqualsAndHashCode
@Data
public class LarkSender {

    private String appId;

    private String appSecret;

    private String mustNotNull = " must not be null";

    private String alarmTitle = "【数据质量告警】";
    private String alarmColor = "green";
    private String subjectKey = "摘要";
    private String messageKey = "详情";
    private String atAllKey = "请留意";

    public LarkSender(SlaSenderMessage senderMessage) {

        String configString = senderMessage.getConfig();
        Map<String, String> config = JSONUtils.toMap(configString);

        appId = config.get("appId");
        requireNonNull(appId, "lark appId" + mustNotNull);

        appSecret = config.get("appSecret");
        requireNonNull(appSecret, "lark appSecret" + mustNotNull);

    }

    public SlaNotificationResultRecord sendCardMsg(Set<ReceiverConfig> receiverSet, String subject, String message){
        SlaNotificationResultRecord result = new SlaNotificationResultRecord();
        // if there is no receivers , no need to process
        if (CollectionUtils.isEmpty(receiverSet)) {
            return result;
        }
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        // send lark
        Set<ReceiverConfig> failToReceivers = new HashSet<>();
        for(ReceiverConfig receiverConfig : receiverSet){
            try {
                List<MessageDTO> messageList = Lists.newArrayList(MessageDTO.routineBuilder(subjectKey,subject),MessageDTO.routineBuilder(messageKey,message));
                if(receiverConfig.getAtAll()){
                    messageList.add(MessageDTO.atAllBuilder(atAllKey));
                }
                GroupCardDTO groupCardDTO = new GroupCardDTO(alarmTitle,alarmColor,messageList);
                Map<String, String> paramMap = ContentUtil.createParamMap(LarkConstants.MSG_TYPE, LarkConstants.DEFAULT_MSG_TYPE, LarkConstants.CARD, JSONUtils.toJsonString(groupCardDTO));
                String ret = HttpUtils.post(String.format(LarkConstants.LARK_API + LarkConstants.GROUP_HOOK_URL, receiverConfig.getToken()),JSONUtils.toJsonString(paramMap),null);
            } catch (Exception e) {
                failToReceivers.add(receiverConfig);
                log.error("lark send error", e);
            }
        }


        if (!CollectionUtils.isEmpty(failToReceivers)) {
            String recordMessage = String.format("send to %s fail", String.join(",", failToReceivers.stream().map(ReceiverConfig::getGroupName).collect(Collectors.toList())));
            result.setStatus(false);
            result.setMessage(recordMessage);
        }else{
            result.setStatus(true);
        }
        return result;
    }
}
