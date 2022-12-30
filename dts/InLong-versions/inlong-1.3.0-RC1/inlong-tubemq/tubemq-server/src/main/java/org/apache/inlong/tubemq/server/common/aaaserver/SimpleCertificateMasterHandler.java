/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.master.MasterConfig;

public class SimpleCertificateMasterHandler implements CertificateMasterHandler {

    private final MasterConfig masterConfig;

    public SimpleCertificateMasterHandler(final MasterConfig masterConfig) {
        this.masterConfig = masterConfig;
    }

    @Override
    public CertifiedResult identityValidBrokerInfo(
            final ClientMaster.MasterCertificateInfo certificateInfo) {
        CertifiedResult result = new CertifiedResult();
        if (!masterConfig.isNeedBrokerVisitAuth()) {
            result.setSuccessResult("", "");
            return result;
        }
        if (certificateInfo == null) {
            return result;
        }
        ClientMaster.AuthenticateInfo authInfo = certificateInfo.getAuthInfo();
        if (authInfo == null) {
            result.setFailureResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                    "Illegal value: AuthenticateInfo is null!");
            return result;
        }
        if (TStringUtils.isBlank(authInfo.getUserName())) {
            result.setFailureResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                    "Illegal value: authInfo.userName is Blank!");
            return result;
        }
        String inUserName = authInfo.getUserName().trim();
        if (TStringUtils.isBlank(authInfo.getSignature())) {
            result.setFailureResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                    "Illegal value: authInfo.signature is Blank!");
            return result;
        }
        String inSignature = authInfo.getSignature().trim();
        if (!inUserName.equals(masterConfig.getVisitName())) {
            result.setFailureResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                    "Illegal value: userName is not equal in authenticateToken!");
            return result;
        }
        if (Math.abs(System.currentTimeMillis() - authInfo.getTimestamp())
                > masterConfig.getAuthValidTimeStampPeriodMs()) {
            result.setFailureResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                    "Illegal value: timestamp out of effective period in authenticateToken!");
            return result;
        }
        String signature =
                TStringUtils.getAuthSignature(inUserName,
                        masterConfig.getVisitPassword(),
                        authInfo.getTimestamp(), authInfo.getNonce());
        if (!inSignature.equals(signature)) {
            result.setFailureResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                    "Illegal value: userName or password is not correct!");
            return result;
        }
        result.setSuccessResult("", "");
        return result;
    }

    @Override
    public CertifiedResult identityValidUserInfo(final ClientMaster.MasterCertificateInfo certificateInfo,
                                                 boolean isProduce) {
        String inUserName = "";
        String authorizedToken = "";
        String othParams = "";
        CertifiedResult result = new CertifiedResult();
        if (isProduce) {
            if (!masterConfig.isStartProduceAuthenticate()) {
                result.setSuccessResult(inUserName, authorizedToken);
                return result;
            }
        } else {
            if (!masterConfig.isStartConsumeAuthenticate()) {
                result.setSuccessResult(inUserName, authorizedToken);
                return result;
            }
        }
        if (certificateInfo == null) {
            result.setFailureResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                    "Server required MasterCertificateInfo!");
            return result;
        }
        ClientMaster.AuthenticateInfo authInfo = certificateInfo.getAuthInfo();
        if (authInfo == null) {
            result.setFailureResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                    "Illegal value: AuthenticateInfo is null!");
            return result;
        }
        if (TStringUtils.isBlank(authInfo.getUserName())) {
            result.setFailureResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                    "Illegal value: authInfo.userName is Blank!");
            return result;
        }
        inUserName = authInfo.getUserName().trim();
        if (TStringUtils.isNotBlank(authInfo.getOthParams())) {
            othParams = authInfo.getOthParams().trim();
        }
        if (TStringUtils.isBlank(authInfo.getSignature())) {
            result.setFailureResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                    "Illegal value: authInfo.signature is Blank!");
            return result;
        }
        String inSignature = authInfo.getSignature().trim();
        if (Math.abs(System.currentTimeMillis() - authInfo.getTimestamp())
                > masterConfig.getAuthValidTimeStampPeriodMs()) {
            result.setFailureResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                    "Illegal value: timestamp out of effective period in authenticateToken!");
            return result;
        }
        // get username and password from certificate center  begin
        // get username and password from certificate center  end
        // get identified userName and authorized token info and return
        result.setSuccessResult(inUserName, authorizedToken);
        return result;
    }

    @Override
    public CertifiedResult validProducerAuthorizeInfo(String userName, Set<String> topics, String clientIp) {
        CertifiedResult result = new CertifiedResult();
        if (!masterConfig.isStartProduceAuthorize()) {
            result.setSuccessResult("", "");
            return result;
        }
        // call authorize center begin
        // call authorize center end
        result.setSuccessResult("", "");
        return result;
    }

    @Override
    public CertifiedResult validConsumerAuthorizeInfo(String userName, String groupName, Set<String> topics,
                                                      Map<String, TreeSet<String>> topicConds, String clientIp) {
        CertifiedResult result = new CertifiedResult();
        if (!masterConfig.isStartProduceAuthorize()) {
            result.setSuccessResult("", "");
            return result;
        }
        // call authorize center begin
        // call authorize center end
        result.setSuccessResult("", "");
        return result;
    }

}
