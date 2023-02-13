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

package org.apache.inlong.tubemq.server.common.aaaserver;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.master.MasterConfig;

public class SimpleCertificateMasterHandler implements CertificateMasterHandler {

    private final MasterConfig masterConfig;

    public SimpleCertificateMasterHandler(final MasterConfig masterConfig) {
        this.masterConfig = masterConfig;
    }

    @Override
    public boolean identityValidBrokerInfo(
            ClientMaster.MasterCertificateInfo certificateInfo, ProcessResult result) {
        if (!masterConfig.isNeedBrokerVisitAuth()) {
            result.setSuccResult(new CertifiedInfo());
            return result.isSuccess();
        }
        if (certificateInfo == null) {
            result.setSuccResult(new CertifiedInfo());
            return result.isSuccess();
        }
        ClientMaster.AuthenticateInfo authInfo = certificateInfo.getAuthInfo();
        if (authInfo == null) {
            result.setFailResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                    "Illegal value: AuthenticateInfo is null!");
            return result.isSuccess();
        }
        if (TStringUtils.isBlank(authInfo.getUserName())) {
            result.setFailResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                    "Illegal value: authInfo.userName is Blank!");
            return result.isSuccess();
        }
        String inUserName = authInfo.getUserName().trim();
        if (TStringUtils.isBlank(authInfo.getSignature())) {
            result.setFailResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                    "Illegal value: authInfo.signature is Blank!");
            return result.isSuccess();
        }
        String inSignature = authInfo.getSignature().trim();
        if (!inUserName.equals(masterConfig.getVisitName())) {
            result.setFailResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                    "Illegal value: userName is not equal in authenticateToken!");
            return result.isSuccess();
        }
        if (Math.abs(System.currentTimeMillis() - authInfo.getTimestamp()) > masterConfig
                .getAuthValidTimeStampPeriodMs()) {
            result.setFailResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                    "Illegal value: timestamp out of effective period in authenticateToken!");
            return result.isSuccess();
        }
        String signature =
                TStringUtils.getAuthSignature(inUserName,
                        masterConfig.getVisitPassword(),
                        authInfo.getTimestamp(), authInfo.getNonce());
        if (!inSignature.equals(signature)) {
            result.setFailResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                    "Illegal value: userName or password is not correct!");
            return result.isSuccess();
        }
        result.setSuccResult(new CertifiedInfo());
        return result.isSuccess();
    }

    @Override
    public boolean identityValidUserInfo(ClientMaster.MasterCertificateInfo certificateInfo,
            boolean isProduce, ProcessResult result) {
        String inUserName = "";
        String authorizedToken = "";
        String othParams = "";
        if (isProduce) {
            if (!masterConfig.isStartProduceAuthenticate()) {
                result.setSuccResult(new CertifiedInfo(inUserName, authorizedToken));
                return result.isSuccess();
            }
        } else {
            if (!masterConfig.isStartConsumeAuthenticate()) {
                result.setSuccResult(new CertifiedInfo(inUserName, authorizedToken));
                return result.isSuccess();
            }
        }
        if (certificateInfo == null) {
            result.setFailResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                    "Server required MasterCertificateInfo!");
            return result.isSuccess();
        }
        ClientMaster.AuthenticateInfo authInfo = certificateInfo.getAuthInfo();
        if (authInfo == null) {
            result.setFailResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                    "Illegal value: AuthenticateInfo is null!");
            return result.isSuccess();
        }
        if (TStringUtils.isBlank(authInfo.getUserName())) {
            result.setFailResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                    "Illegal value: authInfo.userName is Blank!");
            return result.isSuccess();
        }
        inUserName = authInfo.getUserName().trim();
        if (TStringUtils.isNotBlank(authInfo.getOthParams())) {
            othParams = authInfo.getOthParams().trim();
        }
        if (TStringUtils.isBlank(authInfo.getSignature())) {
            result.setFailResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                    "Illegal value: authInfo.signature is Blank!");
            return result.isSuccess();
        }
        String inSignature = authInfo.getSignature().trim();
        if (Math.abs(System.currentTimeMillis() - authInfo.getTimestamp()) > masterConfig
                .getAuthValidTimeStampPeriodMs()) {
            result.setFailResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                    "Illegal value: timestamp out of effective period in authenticateToken!");
            return result.isSuccess();
        }
        // get username and password from certificate center begin
        // get username and password from certificate center end
        // get identified userName and authorized token info and return
        result.setSuccResult(new CertifiedInfo(inUserName, authorizedToken));
        return result.isSuccess();
    }

    @Override
    public boolean validProducerAuthorizeInfo(String userName, Set<String> topics,
            String clientIp, ProcessResult result) {
        if (!masterConfig.isStartProduceAuthorize()) {
            result.setSuccResult(new CertifiedInfo());
            return result.isSuccess();
        }
        // call authorize center begin
        // call authorize center end
        result.setSuccResult(new CertifiedInfo());
        return result.isSuccess();
    }

    @Override
    public boolean validConsumerAuthorizeInfo(String userName, String groupName, Set<String> topics,
            Map<String, TreeSet<String>> topicConds, String clientIp, ProcessResult result) {
        if (!masterConfig.isStartProduceAuthorize()) {
            result.setSuccResult(new CertifiedInfo());
            return result.isSuccess();
        }
        // call authorize center begin
        // call authorize center end
        result.setSuccResult(new CertifiedInfo());
        return result.isSuccess();
    }

}
