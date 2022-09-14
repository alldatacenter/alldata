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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientBroker;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.broker.TubeBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleCertificateBrokerHandler implements CertificateBrokerHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(SimpleCertificateBrokerHandler.class);
    private static final int MAX_VISIT_TOKEN_SIZE = 6; // at least 3 items
    private long inValidTokenCheckTimeMs = 120000; // 2 minutes
    private final TubeBroker tubeBroker;
    private final AtomicReference<List<Long>> visitTokenList =
            new AtomicReference<>();
    private String lastUpdatedVisitTokens = "";
    private boolean enableVisitTokenCheck = false;
    private boolean enableProduceAuthenticate = false;
    private boolean enableProduceAuthorize = false;
    private boolean enableConsumeAuthenticate = false;
    private boolean enableConsumeAuthorize = false;

    public SimpleCertificateBrokerHandler(final TubeBroker tubeBroker) {
        this.tubeBroker = tubeBroker;
        this.visitTokenList.set(new ArrayList<Long>());
        this.inValidTokenCheckTimeMs =
            tubeBroker.getTubeConfig().getVisitTokenCheckInValidTimeMs();
    }

    @Override
    public void configure(ClientMaster.EnableBrokerFunInfo enableFunInfo) {
        if (enableFunInfo != null) {
            if (enableFunInfo.hasEnableVisitTokenCheck()) {
                this.enableVisitTokenCheck = enableFunInfo.getEnableVisitTokenCheck();
            }
            this.enableProduceAuthenticate = enableFunInfo.getEnableProduceAuthenticate();
            this.enableProduceAuthorize = enableFunInfo.getEnableProduceAuthorize();
            this.enableConsumeAuthenticate = enableFunInfo.getEnableConsumeAuthenticate();
            this.enableConsumeAuthorize = enableFunInfo.getEnableConsumeAuthorize();
        }

    }

    @Override
    public void appendVisitToken(ClientMaster.MasterBrokerAuthorizedInfo authorizedInfo) {
        if (authorizedInfo == null) {
            return;
        }
        String curBrokerVisitTokens = authorizedInfo.getVisitAuthorizedToken();
        if (TStringUtils.isBlank(curBrokerVisitTokens)
            || lastUpdatedVisitTokens.equals(curBrokerVisitTokens)) {
            return;
        }
        lastUpdatedVisitTokens = curBrokerVisitTokens;
        String[] visitTokenItems = curBrokerVisitTokens.split(TokenConstants.ARRAY_SEP);
        for (int i = 0; i < visitTokenItems.length; i++) {
            if (TStringUtils.isBlank(visitTokenItems[i])) {
                continue;
            }
            try {
                long curVisitToken = Long.parseLong(visitTokenItems[i].trim());
                List<Long> currList = visitTokenList.get();
                if (!currList.contains(curVisitToken)) {
                    while (true) {
                        currList = visitTokenList.get();
                        if (currList.contains(curVisitToken)) {
                            break;
                        }
                        List<Long> updateList = new ArrayList<>(currList);
                        while (updateList.size() >= MAX_VISIT_TOKEN_SIZE) {
                            updateList.remove(0);
                        }
                        updateList.add(curVisitToken);
                        if (visitTokenList.compareAndSet(currList, updateList)) {
                            break;
                        }
                    }
                }
            } catch (Throwable e) {
                //
            }
        }
    }

    @Override
    public CertifiedResult identityValidUserInfo(final ClientBroker.AuthorizedInfo authorizedInfo,
                                                 boolean isProduce) {
        CertifiedResult result = new CertifiedResult();
        if (authorizedInfo == null) {
            result.setFailureResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                "Authorized Info is required!");
            return result;
        }
        if (enableVisitTokenCheck) {
            long curVisitToken = authorizedInfo.getVisitAuthorizedToken();
            List<Long> currList = visitTokenList.get();
            if (tubeBroker.isKeepAlive()) {
                if (!currList.contains(curVisitToken)
                    && (System.currentTimeMillis() - tubeBroker.getLastRegTime() > inValidTokenCheckTimeMs)) {
                    result.setFailureResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                        "Visit Authorized Token is invalid!");
                    return result;
                }
            }
        }
        if ((isProduce && !enableProduceAuthenticate)
            || (!isProduce && !enableConsumeAuthenticate)) {
            result.setSuccessResult("", "");
            return result;
        }
        if (TStringUtils.isBlank(authorizedInfo.getAuthAuthorizedToken())) {
            result.setFailureResult(TErrCodeConstants.CERTIFICATE_FAILURE,
                    "authAuthorizedToken is Blank!");
            return result;
        }
        // process authAuthorizedToken info from certificate center begin
        // process authAuthorizedToken info from certificate center end
        // set userName, reAuth info
        result.setSuccessResult("", "", false);
        return result;
    }

    @Override
    public CertifiedResult validConsumeAuthorizeInfo(final String userName, final String groupName,
                                                     final String topicName, final Set<String> msgTypeLst,
                                                     boolean isRegister, String clientIp) {
        CertifiedResult result = new CertifiedResult();
        if (!enableConsumeAuthorize) {
            result.setSuccessResult("", "");
            return result;
        }

        // process authorize from authorize center begin
        // process authorize from authorize center end
        result.setSuccessResult("", "");
        return result;
    }

    @Override
    public CertifiedResult validProduceAuthorizeInfo(final String userName, final String topicName,
                                                     final String msgType, String clientIp) {
        CertifiedResult result = new CertifiedResult();
        if (!enableProduceAuthorize) {
            result.setSuccessResult("", "");
            return result;
        }

        // process authorize from authorize center begin
        // process authorize from authorize center end
        result.setSuccessResult("", "");
        return result;
    }

    @Override
    public boolean isEnableProduceAuthenticate() {
        return enableProduceAuthenticate;
    }

    @Override
    public boolean isEnableProduceAuthorize() {
        return enableProduceAuthorize;
    }

    @Override
    public boolean isEnableConsumeAuthenticate() {
        return enableConsumeAuthenticate;
    }

    @Override
    public boolean isEnableConsumeAuthorize() {
        return enableConsumeAuthorize;
    }

    @Override
    public boolean isEnableVisitTokenCheck() {
        return enableVisitTokenCheck;
    }
}
