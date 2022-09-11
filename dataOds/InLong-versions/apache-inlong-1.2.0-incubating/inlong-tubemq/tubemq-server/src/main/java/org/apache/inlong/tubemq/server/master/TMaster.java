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

package org.apache.inlong.tubemq.server.master;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.balance.ConsumerEvent;
import org.apache.inlong.tubemq.corebase.balance.EventStatus;
import org.apache.inlong.tubemq.corebase.balance.EventType;
import org.apache.inlong.tubemq.corebase.cluster.BrokerInfo;
import org.apache.inlong.tubemq.corebase.cluster.NodeAddrInfo;
import org.apache.inlong.tubemq.corebase.cluster.Partition;
import org.apache.inlong.tubemq.corebase.cluster.ProducerInfo;
import org.apache.inlong.tubemq.corebase.cluster.SubscribeInfo;
import org.apache.inlong.tubemq.corebase.cluster.TopicInfo;
import org.apache.inlong.tubemq.corebase.config.TLSConfig;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.CloseRequestB2M;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.CloseRequestC2M;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.CloseRequestP2M;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.CloseResponseM2B;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.CloseResponseM2C;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.CloseResponseM2P;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.EnableBrokerFunInfo;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.EventProto;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.GetPartMetaRequestC2M;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.GetPartMetaResponseM2C;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.HeartRequestB2M;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.HeartRequestC2M;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.HeartRequestC2MV2;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.HeartRequestP2M;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.HeartResponseM2B;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.HeartResponseM2C;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.HeartResponseM2CV2;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.HeartResponseM2P;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.MasterAuthorizedInfo;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.MasterBrokerAuthorizedInfo;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.RegisterRequestB2M;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.RegisterRequestC2M;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.RegisterRequestC2MV2;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.RegisterRequestP2M;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.RegisterResponseM2B;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.RegisterResponseM2C;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.RegisterResponseM2CV2;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.RegisterResponseM2P;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.corebase.utils.DataConverterUtil;
import org.apache.inlong.tubemq.corebase.utils.OpsSyncInfo;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.corebase.utils.ThreadUtils;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.apache.inlong.tubemq.corerpc.RpcConfig;
import org.apache.inlong.tubemq.corerpc.RpcConstants;
import org.apache.inlong.tubemq.corerpc.RpcServiceFactory;
import org.apache.inlong.tubemq.corerpc.exception.StandbyException;
import org.apache.inlong.tubemq.corerpc.service.MasterService;
import org.apache.inlong.tubemq.server.Stoppable;
import org.apache.inlong.tubemq.server.common.aaaserver.CertificateMasterHandler;
import org.apache.inlong.tubemq.server.common.aaaserver.CertifiedResult;
import org.apache.inlong.tubemq.server.common.aaaserver.SimpleCertificateMasterHandler;
import org.apache.inlong.tubemq.server.common.exception.HeartbeatException;
import org.apache.inlong.tubemq.server.common.heartbeat.HeartbeatManager;
import org.apache.inlong.tubemq.server.common.heartbeat.TimeoutInfo;
import org.apache.inlong.tubemq.server.common.heartbeat.TimeoutListener;
import org.apache.inlong.tubemq.server.common.paramcheck.PBParameterUtils;
import org.apache.inlong.tubemq.server.common.paramcheck.ParamCheckResult;
import org.apache.inlong.tubemq.server.common.utils.ClientSyncInfo;
import org.apache.inlong.tubemq.server.common.utils.HasThread;
import org.apache.inlong.tubemq.server.common.utils.RowLock;
import org.apache.inlong.tubemq.server.common.utils.Sleeper;
import org.apache.inlong.tubemq.server.master.balance.DefaultLoadBalancer;
import org.apache.inlong.tubemq.server.master.balance.LoadBalancer;
import org.apache.inlong.tubemq.server.master.metamanage.DefaultMetaDataService;
import org.apache.inlong.tubemq.server.master.metamanage.MetaDataService;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BrokerConfEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.ClusterSettingEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupResCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicDeployEntity;
import org.apache.inlong.tubemq.server.master.nodemanage.nodebroker.BrokerAbnHolder;
import org.apache.inlong.tubemq.server.master.nodemanage.nodebroker.BrokerRunManager;
import org.apache.inlong.tubemq.server.master.nodemanage.nodebroker.DefBrokerRunManager;
import org.apache.inlong.tubemq.server.master.nodemanage.nodebroker.TopicPSInfoManager;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumeGroupInfo;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumeType;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumerEventManager;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumerInfo;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumerInfoHolder;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeproducer.ProducerInfoHolder;
import org.apache.inlong.tubemq.server.master.stats.MasterJMXHolder;
import org.apache.inlong.tubemq.server.master.stats.MasterSrvStatsHolder;
import org.apache.inlong.tubemq.server.master.utils.Chore;
import org.apache.inlong.tubemq.server.master.utils.SimpleVisitTokenManager;
import org.apache.inlong.tubemq.server.master.web.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TMaster extends HasThread implements MasterService, Stoppable {

    private static final Logger logger = LoggerFactory.getLogger(TMaster.class);
    private static final int MAX_BALANCE_DELAY_TIME = 10;

    private final ConcurrentHashMap<String/* consumerId */, Map<String/* topic */, Map<String, Partition>>>
            currentSubInfo = new ConcurrentHashMap<>();
    private final RpcServiceFactory rpcServiceFactory =     //rpc service factory
            new RpcServiceFactory();
    private final MetaDataService defMetaDataService;      // meta data manager
    private final BrokerRunManager brokerRunManager;       // broker run status manager
    private final ConsumerEventManager consumerEventManager;    //consumer event manager
    private final TopicPSInfoManager topicPSInfoManager;        //topic publish/subscribe info manager
    private final ExecutorService svrExecutor;
    private final ExecutorService cltExecutor;
    private final ProducerInfoHolder producerHolder;            //producer holder
    private final ConsumerInfoHolder consumerHolder;            //consumer holder
    private final RowLock masterRowLock;                        //lock
    private final WebServer webServer;                          //web server
    private final LoadBalancer loadBalancer;                    //load balance
    private final MasterConfig masterConfig;                    //master config
    private final NodeAddrInfo masterAddInfo;                   //master address info
    private final HeartbeatManager heartbeatManager;            //heartbeat manager
    private final ShutdownHook shutdownHook;                    //shutdown hook
    private final CertificateMasterHandler serverAuthHandler;           //server auth handler
    private AtomicBoolean shutdownHooked = new AtomicBoolean(false);
    private AtomicLong idGenerator = new AtomicLong(0);     //id generator
    private volatile boolean stopped = false;                   //stop flag
    private Thread balancerChore;                               //balance chore
    private boolean initialized = false;
    private boolean startupBalance = true;
    private int balanceDelayTimes = 0;
    private AtomicInteger curSvrBalanceParal = new AtomicInteger(0);
    private AtomicInteger curCltBalanceParal = new AtomicInteger(0);
    private Sleeper stopSleeper = new Sleeper(1000, this);
    private SimpleVisitTokenManager visitTokenManager;

    /**
     * constructor
     *
     * @param masterConfig
     * @throws Exception
     */
    public TMaster(MasterConfig masterConfig) throws Exception {
        this.masterConfig = masterConfig;
        this.masterRowLock =
                new RowLock("Master-RowLock", this.masterConfig.getRowLockWaitDurMs());
        if (this.masterConfig.isUseBdbStoreMetaData()) {
            this.chkAndCreateBdbMetaDataPath();
        }
        this.masterAddInfo =
                new NodeAddrInfo(masterConfig.getHostName(), masterConfig.getPort());
        // register metric bean
        MasterJMXHolder.registerMXBean();
        this.svrExecutor = Executors.newFixedThreadPool(this.masterConfig.getRebalanceParallel());
        this.cltExecutor = Executors.newFixedThreadPool(this.masterConfig.getRebalanceParallel());
        this.visitTokenManager = new SimpleVisitTokenManager(this.masterConfig);
        this.serverAuthHandler = new SimpleCertificateMasterHandler(this.masterConfig);
        this.heartbeatManager = new HeartbeatManager();
        this.producerHolder = new ProducerInfoHolder();
        this.consumerHolder = new ConsumerInfoHolder(this);
        this.consumerEventManager = new ConsumerEventManager(consumerHolder);
        this.topicPSInfoManager = new TopicPSInfoManager(this);
        this.loadBalancer = new DefaultLoadBalancer();
        heartbeatManager.regConsumerCheckBusiness(masterConfig.getConsumerHeartbeatTimeoutMs(),
                new TimeoutListener() {
                    @Override
                    public void onTimeout(String nodeId, TimeoutInfo nodeInfo) {
                        logger.info(new StringBuilder(512).append("[Consumer Timeout] ")
                                .append(nodeId).toString());
                        new ReleaseConsumer().run(nodeId, true);
                    }
                });
        heartbeatManager.regProducerCheckBusiness(masterConfig.getProducerHeartbeatTimeoutMs(),
                new TimeoutListener() {
                    @Override
                    public void onTimeout(final String nodeId, TimeoutInfo nodeInfo) {
                        logger.info(new StringBuilder(512).append("[Producer Timeout] ")
                                .append(nodeId).toString());
                        new ReleaseProducer().run(nodeId, true);
                    }
                });
        this.defMetaDataService = new DefaultMetaDataService(this);
        this.brokerRunManager = new DefBrokerRunManager(this);
        this.defMetaDataService.start();
        RpcConfig rpcTcpConfig = new RpcConfig();
        rpcTcpConfig.put(RpcConstants.REQUEST_TIMEOUT,
                masterConfig.getRpcReadTimeoutMs());
        rpcTcpConfig.put(RpcConstants.NETTY_WRITE_HIGH_MARK,
                masterConfig.getNettyWriteBufferHighWaterMark());
        rpcTcpConfig.put(RpcConstants.NETTY_WRITE_LOW_MARK,
                masterConfig.getNettyWriteBufferLowWaterMark());
        rpcTcpConfig.put(RpcConstants.NETTY_TCP_SENDBUF,
                masterConfig.getSocketSendBuffer());
        rpcTcpConfig.put(RpcConstants.NETTY_TCP_RECEIVEBUF,
                masterConfig.getSocketRecvBuffer());
        rpcServiceFactory.publishService(MasterService.class,
                this, masterConfig.getPort(), rpcTcpConfig);
        if (masterConfig.isTlsEnable()) {
            TLSConfig tlsConfig = masterConfig.getTlsConfig();
            RpcConfig rpcTlsConfig = new RpcConfig();
            rpcTlsConfig.put(RpcConstants.TLS_OVER_TCP, true);
            rpcTlsConfig.put(RpcConstants.TLS_KEYSTORE_PATH,
                    tlsConfig.getTlsKeyStorePath());
            rpcTlsConfig.put(RpcConstants.TLS_KEYSTORE_PASSWORD,
                    tlsConfig.getTlsKeyStorePassword());
            rpcTlsConfig.put(RpcConstants.TLS_TWO_WAY_AUTHENTIC,
                    tlsConfig.isTlsTwoWayAuthEnable());
            if (tlsConfig.isTlsTwoWayAuthEnable()) {
                rpcTlsConfig.put(RpcConstants.TLS_TRUSTSTORE_PATH,
                        tlsConfig.getTlsTrustStorePath());
                rpcTlsConfig.put(RpcConstants.TLS_TRUSTSTORE_PASSWORD,
                        tlsConfig.getTlsTrustStorePassword());
            }
            rpcTlsConfig.put(RpcConstants.REQUEST_TIMEOUT,
                    masterConfig.getRpcReadTimeoutMs());
            rpcTlsConfig.put(RpcConstants.NETTY_WRITE_HIGH_MARK,
                    masterConfig.getNettyWriteBufferHighWaterMark());
            rpcTlsConfig.put(RpcConstants.NETTY_WRITE_LOW_MARK,
                    masterConfig.getNettyWriteBufferLowWaterMark());
            rpcTlsConfig.put(RpcConstants.NETTY_TCP_SENDBUF,
                    masterConfig.getSocketSendBuffer());
            rpcTlsConfig.put(RpcConstants.NETTY_TCP_RECEIVEBUF,
                    masterConfig.getSocketRecvBuffer());
            rpcServiceFactory.publishService(MasterService.class,
                    this, tlsConfig.getTlsPort(), rpcTlsConfig);
        }
        this.webServer = new WebServer(masterConfig, this);
        this.webServer.start();

        this.shutdownHook = new ShutdownHook();
        Runtime.getRuntime().addShutdownHook(this.shutdownHook);
    }

    /**
     * Get master config
     *
     * @return
     */
    public MasterConfig getMasterConfig() {
        return masterConfig;
    }

    public MetaDataService getMetaDataService() {
        return defMetaDataService;
    }

    public HeartbeatManager getHeartbeatManager() {
        return heartbeatManager;
    }

    public BrokerRunManager getBrokerRunManager() {
        return brokerRunManager;
    }

    /**
     * Producer register request to master
     *
     * @param request
     * @param rmtAddress
     * @param overtls
     * @return register response
     * @throws Exception
     */
    @Override
    public RegisterResponseM2P producerRegisterP2M(RegisterRequestP2M request,
                                                   final String rmtAddress,
                                                   boolean overtls) throws Exception {
        final StringBuilder strBuffer = new StringBuilder(512);
        RegisterResponseM2P.Builder builder = RegisterResponseM2P.newBuilder();
        builder.setSuccess(false);
        builder.setBrokerCheckSum(-1);
        CertifiedResult certResult =
                serverAuthHandler.identityValidUserInfo(request.getAuthInfo(), true);
        if (!certResult.result) {
            builder.setErrCode(certResult.errCode);
            builder.setErrMsg(certResult.errInfo);
            return builder.build();
        }
        ParamCheckResult paramCheckResult =
                PBParameterUtils.checkClientId(request.getClientId(), strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        final String producerId = (String) paramCheckResult.checkData;
        paramCheckResult = PBParameterUtils.checkHostName(request.getHostName(), strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        final String hostName = (String) paramCheckResult.checkData;
        paramCheckResult =
                PBParameterUtils.checkProducerTopicList(request.getTopicListList(), strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        final Set<String> transTopicSet = (Set<String>) paramCheckResult.checkData;
        if (!request.hasBrokerCheckSum()) {
            builder.setErrCode(TErrCodeConstants.BAD_REQUEST);
            builder.setErrMsg("Request miss necessary brokerCheckSum field!");
            return builder.build();
        }
        checkNodeStatus(producerId, strBuffer);
        CertifiedResult authorizeResult =
                serverAuthHandler.validProducerAuthorizeInfo(
                        certResult.userName, transTopicSet, rmtAddress);
        if (!authorizeResult.result) {
            builder.setErrCode(authorizeResult.errCode);
            builder.setErrMsg(authorizeResult.errInfo);
            return builder.build();
        }
        final String clientJdkVer = request.hasJdkVersion() ? request.getJdkVersion() : "";
        heartbeatManager.regProducerNode(producerId);
        producerHolder.setProducerInfo(producerId,
                new HashSet<>(transTopicSet), hostName, overtls);
        Tuple2<Long, Map<Integer, String>> brokerStaticInfo =
                brokerRunManager.getBrokerStaticInfo(overtls);
        builder.setBrokerCheckSum(brokerStaticInfo.getF0());
        builder.addAllBrokerInfos(brokerStaticInfo.getF1().values());
        builder.setAuthorizedInfo(genAuthorizedInfo(certResult.authorizedToken, false).build());
        ClientMaster.ApprovedClientConfig.Builder clientConfigBuilder =
                buildApprovedClientConfig(request.getAppdConfig());
        if (clientConfigBuilder != null) {
            builder.setAppdConfig(clientConfigBuilder);
        }
        logger.info(strBuffer.append("[Producer Register] ").append(producerId)
            .append(", isOverTLS=").append(overtls)
            .append(", clientJDKVer=").append(clientJdkVer).toString());
        builder.setSuccess(true);
        builder.setErrCode(TErrCodeConstants.SUCCESS);
        builder.setErrMsg("OK!");
        return builder.build();
    }

    /**
     * Producer heartbeat request with master
     *
     * @param request
     * @param rmtAddress
     * @param overtls
     * @return heartbeat response
     * @throws Exception
     */
    @Override
    public HeartResponseM2P producerHeartbeatP2M(HeartRequestP2M request,
                                                 final String rmtAddress,
                                                 boolean overtls) throws Exception {
        final StringBuilder strBuffer = new StringBuilder(512);
        HeartResponseM2P.Builder builder = HeartResponseM2P.newBuilder();
        builder.setSuccess(false);
        builder.setBrokerCheckSum(-1);
        CertifiedResult certResult =
                serverAuthHandler.identityValidUserInfo(request.getAuthInfo(), true);
        if (!certResult.result) {
            builder.setErrCode(certResult.errCode);
            builder.setErrMsg(certResult.errInfo);
            return builder.build();
        }
        ParamCheckResult paramCheckResult =
                PBParameterUtils.checkClientId(request.getClientId(), strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        final String producerId = (String) paramCheckResult.checkData;
        paramCheckResult = PBParameterUtils.checkHostName(request.getHostName(), strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        final String hostName = (String) paramCheckResult.checkData;
        paramCheckResult =
                PBParameterUtils.checkProducerTopicList(request.getTopicListList(), strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        final Set<String> transTopicSet = (Set<String>) paramCheckResult.checkData;
        if (!request.hasBrokerCheckSum()) {
            builder.setErrCode(TErrCodeConstants.BAD_REQUEST);
            builder.setErrMsg("Request miss necessary brokerCheckSum field!");
            return builder.build();
        }
        final long inBrokerCheckSum = request.getBrokerCheckSum();
        checkNodeStatus(producerId, strBuffer);
        CertifiedResult authorizeResult =
                serverAuthHandler.validProducerAuthorizeInfo(
                        certResult.userName, transTopicSet, rmtAddress);
        if (!authorizeResult.result) {
            builder.setErrCode(authorizeResult.errCode);
            builder.setErrMsg(authorizeResult.errInfo);
            return builder.build();
        }
        try {
            heartbeatManager.updProducerNode(producerId);
        } catch (HeartbeatException e) {
            builder.setErrCode(TErrCodeConstants.HB_NO_NODE);
            builder.setErrMsg(e.getMessage());
            return builder.build();
        }
        topicPSInfoManager.addProducerTopicPubInfo(producerId, transTopicSet);
        producerHolder.updateProducerInfo(producerId,
                transTopicSet, hostName, overtls);
        Map<String, String> availTopicPartitions = getProducerTopicPartitionInfo(producerId);
        builder.addAllTopicInfos(availTopicPartitions.values());
        builder.setAuthorizedInfo(genAuthorizedInfo(certResult.authorizedToken, false).build());
        Tuple2<Long, Map<Integer, String>> brokerStaticInfo =
                brokerRunManager.getBrokerStaticInfo(overtls);
        builder.setBrokerCheckSum(brokerStaticInfo.getF0());
        if (brokerStaticInfo.getF0() != inBrokerCheckSum) {
            builder.addAllBrokerInfos(brokerStaticInfo.getF1().values());
        }
        ClientMaster.ApprovedClientConfig.Builder clientConfigBuilder =
                buildApprovedClientConfig(request.getAppdConfig());
        if (clientConfigBuilder != null) {
            builder.setAppdConfig(clientConfigBuilder);
        }
        if (logger.isDebugEnabled()) {
            logger.debug(strBuffer.append("[Push Producer's available topic count:]")
                    .append(producerId).append(TokenConstants.LOG_SEG_SEP)
                    .append(availTopicPartitions.size()).toString());
        }
        builder.setSuccess(true);
        builder.setErrCode(TErrCodeConstants.SUCCESS);
        builder.setErrMsg("OK!");
        return builder.build();
    }

    /**
     * Producer close request with master
     *
     * @param request
     * @param rmtAddress
     * @param overtls
     * @return close response
     * @throws Exception
     */
    @Override
    public CloseResponseM2P producerCloseClientP2M(CloseRequestP2M request,
                                                   final String rmtAddress,
                                                   boolean overtls) throws Exception {
        final StringBuilder strBuffer = new StringBuilder(512);
        CloseResponseM2P.Builder builder = CloseResponseM2P.newBuilder();
        builder.setSuccess(false);
        CertifiedResult certResult =
                serverAuthHandler.identityValidUserInfo(request.getAuthInfo(), true);
        if (!certResult.result) {
            builder.setErrCode(certResult.errCode);
            builder.setErrMsg(certResult.errInfo);
            return builder.build();
        }
        ParamCheckResult paramCheckResult =
                PBParameterUtils.checkClientId(request.getClientId(), strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        final String producerId = (String) paramCheckResult.checkData;
        checkNodeStatus(producerId, strBuffer);
        new ReleaseProducer().run(producerId, false);
        heartbeatManager.unRegProducerNode(producerId);
        logger.info(strBuffer.append("[Producer Closed] ")
                .append(producerId).append(", isOverTLS=").append(overtls).toString());
        builder.setSuccess(true);
        builder.setErrCode(TErrCodeConstants.SUCCESS);
        builder.setErrMsg("OK!");
        return builder.build();
    }

    /**
     * Consumer register request with master
     *
     * @param request
     * @param rmtAddress
     * @param overtls
     * @return register response
     * @throws Exception
     */
    @Override
    public RegisterResponseM2C consumerRegisterC2M(RegisterRequestC2M request,
                                                   final String rmtAddress,
                                                   boolean overtls) throws Exception {
        // #lizard forgives
        ProcessResult result = new ProcessResult();
        final StringBuilder strBuffer = new StringBuilder(512);
        RegisterResponseM2C.Builder builder = RegisterResponseM2C.newBuilder();
        builder.setSuccess(false);
        CertifiedResult certResult =
                serverAuthHandler.identityValidUserInfo(request.getAuthInfo(), false);
        if (!certResult.result) {
            builder.setErrCode(certResult.errCode);
            builder.setErrMsg(certResult.errInfo);
            return builder.build();
        }
        ParamCheckResult paramCheckResult =
                PBParameterUtils.checkClientId(request.getClientId(), strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        final String consumerId = (String) paramCheckResult.checkData;
        paramCheckResult = PBParameterUtils.checkHostName(request.getHostName(), strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        //final String hostName = (String) paramCheckResult.checkData;
        paramCheckResult = PBParameterUtils.checkGroupName(request.getGroupName(), strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        final String groupName = (String) paramCheckResult.checkData;
        paramCheckResult = PBParameterUtils.checkConsumerTopicList(request.getTopicListList(), strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        Set<String> reqTopicSet = (Set<String>) paramCheckResult.checkData;
        String requiredParts = request.hasRequiredPartition() ? request.getRequiredPartition() : "";
        ConsumeType csmType = (request.hasRequireBound() && request.getRequireBound())
                ? ConsumeType.CONSUME_BAND : ConsumeType.CONSUME_NORMAL;
        final String clientJdkVer = request.hasJdkVersion() ? request.getJdkVersion() : "";
        paramCheckResult = PBParameterUtils.checkConsumerOffsetSetInfo(csmType,
                reqTopicSet, requiredParts, strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        Map<String, Long> requiredPartMap = (Map<String, Long>) paramCheckResult.checkData;
        Map<String, TreeSet<String>> reqTopicConditions =
                DataConverterUtil.convertTopicConditions(request.getTopicConditionList());
        String sessionKey = request.hasSessionKey() ? request.getSessionKey() : "";
        long sessionTime = request.hasSessionTime()
                ? request.getSessionTime() : System.currentTimeMillis();
        int sourceCount = request.hasTotalCount()
                ? request.getTotalCount() : -1;
        int qryPriorityId = request.hasQryPriorityId()
                ? request.getQryPriorityId() : TBaseConstants.META_VALUE_UNDEFINED;
        List<SubscribeInfo> subscribeList =
                DataConverterUtil.convertSubInfo(request.getSubscribeInfoList());
        boolean isNotAllocated = true;
        if (CollectionUtils.isNotEmpty(subscribeList)
                || ((request.hasNotAllocated() && !request.getNotAllocated()))) {
            isNotAllocated = false;
        }
        boolean isSelectBig = (!request.hasSelectBig() || request.getSelectBig());
        // build consumer object
        ConsumerInfo inConsumerInfo =
                new ConsumerInfo(consumerId, overtls, groupName,
                        reqTopicSet, reqTopicConditions, csmType,
                        sessionKey, sessionTime, sourceCount, isSelectBig, requiredPartMap);
        paramCheckResult =
                PBParameterUtils.checkConsumerInputInfo(inConsumerInfo,
                        masterConfig, defMetaDataService, brokerRunManager, strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        ConsumerInfo inConsumerInfo2 = (ConsumerInfo) paramCheckResult.checkData;
        checkNodeStatus(consumerId, strBuffer);
        CertifiedResult authorizeResult =
                serverAuthHandler.validConsumerAuthorizeInfo(certResult.userName,
                        groupName, reqTopicSet, reqTopicConditions, rmtAddress);
        if (!authorizeResult.result) {
            builder.setErrCode(authorizeResult.errCode);
            builder.setErrMsg(authorizeResult.errInfo);
            return builder.build();
        }
        // need removed for authorize center begin
        if (!this.defMetaDataService
                .isConsumeTargetAuthorized(consumerId, groupName,
                        reqTopicSet, reqTopicConditions, strBuffer, result)) {
            if (strBuffer.length() > 0) {
                logger.warn(strBuffer.toString());
            }
            builder.setErrCode(result.getErrCode());
            builder.setErrMsg(result.getErrMsg());
            return builder.build();
        }
        // need removed for authorize center end
        Integer lid = null;
        ConsumeGroupInfo consumeGroupInfo = null;
        try {
            lid = masterRowLock.getLock(null, StringUtils.getBytesUtf8(consumerId), true);
            if (!consumerHolder.addConsumer(inConsumerInfo2,
                    isNotAllocated, strBuffer, paramCheckResult)) {
                builder.setErrCode(paramCheckResult.errCode);
                builder.setErrMsg(paramCheckResult.errMsg);
                return builder.build();
            }
            consumeGroupInfo = (ConsumeGroupInfo) paramCheckResult.checkData;
            topicPSInfoManager.addGroupSubTopicInfo(groupName, reqTopicSet);
            if (CollectionUtils.isNotEmpty(subscribeList)) {
                Map<String, Map<String, Partition>> topicPartSubMap =
                        new HashMap<>();
                currentSubInfo.put(consumerId, topicPartSubMap);
                for (SubscribeInfo info : subscribeList) {
                    Map<String, Partition> partMap = topicPartSubMap.get(info.getTopic());
                    if (partMap == null) {
                        partMap = new HashMap<>();
                        topicPartSubMap.put(info.getTopic(), partMap);
                    }
                    partMap.put(info.getPartition().getPartitionKey(), info.getPartition());
                    logger.info(strBuffer.append("[SubInfo Report]")
                            .append(info.toString()).toString());
                    strBuffer.delete(0, strBuffer.length());
                }
            }
            heartbeatManager.regConsumerNode(getConsumerKey(groupName, consumerId));
        } catch (IOException e) {
            logger.warn("Failed to lock.", e);
        } finally {
            if (lid != null) {
                this.masterRowLock.releaseRowLock(lid);
            }
        }
        logger.info(strBuffer.append("[Consumer Register] ")
                .append(consumerId).append(", isOverTLS=").append(overtls)
                .append(", clientJDKVer=").append(clientJdkVer).toString());
        strBuffer.delete(0, strBuffer.length());
        if (request.hasDefFlowCheckId() || request.hasGroupFlowCheckId()) {
            builder.setSsdStoreId(TBaseConstants.META_VALUE_UNDEFINED);
            builder.setDefFlowCheckId(TBaseConstants.META_VALUE_UNDEFINED);
            builder.setGroupFlowCheckId(TBaseConstants.META_VALUE_UNDEFINED);
            builder.setQryPriorityId(TBaseConstants.META_VALUE_UNDEFINED);
            builder.setDefFlowControlInfo(" ");
            builder.setGroupFlowControlInfo(" ");
            ClusterSettingEntity defSetting =
                    defMetaDataService.getClusterDefSetting(false);
            GroupResCtrlEntity groupResCtrlConf =
                    defMetaDataService.getGroupCtrlConf(groupName);
            if (defSetting.enableFlowCtrl()) {
                builder.setDefFlowCheckId(defSetting.getSerialId());
                if (request.getDefFlowCheckId() != defSetting.getSerialId()) {
                    builder.setDefFlowControlInfo(defSetting.getGloFlowCtrlRuleInfo());
                }
            }
            if (groupResCtrlConf != null
                    && groupResCtrlConf.isFlowCtrlEnable()) {
                builder.setGroupFlowCheckId(groupResCtrlConf.getSerialId());
                builder.setQryPriorityId(groupResCtrlConf.getQryPriorityId());
                if (request.getGroupFlowCheckId() != groupResCtrlConf.getSerialId()) {
                    builder.setGroupFlowControlInfo(groupResCtrlConf.getFlowCtrlInfo());
                }
            }
        }
        builder.setAuthorizedInfo(genAuthorizedInfo(certResult.authorizedToken, false));
        builder.setNotAllocated(consumeGroupInfo.isNotAllocate());
        builder.setSuccess(true);
        builder.setErrCode(TErrCodeConstants.SUCCESS);
        builder.setErrMsg("OK!");
        return builder.build();
    }

    /**
     * Consumer heartbeat request with master
     *
     * @param request
     * @param rmtAddress
     * @param overtls
     * @return heartbeat response
     * @throws Throwable
     */
    @Override
    public HeartResponseM2C consumerHeartbeatC2M(HeartRequestC2M request,
                                                 final String rmtAddress,
                                                 boolean overtls) throws Throwable {
        // #lizard forgives
        final StringBuilder strBuffer = new StringBuilder(512);
        // response
        HeartResponseM2C.Builder builder = HeartResponseM2C.newBuilder();
        builder.setSuccess(false);
        // identity valid
        CertifiedResult certResult =
                serverAuthHandler.identityValidUserInfo(request.getAuthInfo(), false);
        if (!certResult.result) {
            builder.setErrCode(certResult.errCode);
            builder.setErrMsg(certResult.errInfo);
            return builder.build();
        }
        ParamCheckResult paramCheckResult =
                PBParameterUtils.checkClientId(request.getClientId(), strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        final String clientId = (String) paramCheckResult.checkData;
        paramCheckResult = PBParameterUtils.checkGroupName(request.getGroupName(), strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        final String groupName = (String) paramCheckResult.checkData;
        checkNodeStatus(clientId, strBuffer);
        ConsumeGroupInfo consumeGroupInfo = consumerHolder.getConsumeGroupInfo(groupName);
        if (consumeGroupInfo == null) {
            builder.setErrCode(TErrCodeConstants.HB_NO_NODE);
            builder.setErrMsg(strBuffer.append("Not found groupName ")
                    .append(groupName).append(" in holder!").toString());
            return builder.build();
        }
        // authorize check
        CertifiedResult authorizeResult =
                serverAuthHandler.validConsumerAuthorizeInfo(certResult.userName,
                        groupName, consumeGroupInfo.getTopicSet(),
                        consumeGroupInfo.getTopicConditions(), rmtAddress);
        if (!authorizeResult.result) {
            builder.setErrCode(authorizeResult.errCode);
            builder.setErrMsg(authorizeResult.errInfo);
            return builder.build();
        }
        // heartbeat check
        try {
            heartbeatManager.updConsumerNode(getConsumerKey(groupName, clientId));
        } catch (HeartbeatException e) {
            builder.setErrCode(TErrCodeConstants.HB_NO_NODE);
            builder.setErrMsg(strBuffer
                    .append("Update consumer node exception:")
                    .append(e.getMessage()).toString());
            return builder.build();
        }
        //
        Map<String, Map<String, Partition>> topicPartSubList =
                currentSubInfo.get(clientId);
        if (topicPartSubList == null) {
            topicPartSubList = new HashMap<>();
            Map<String, Map<String, Partition>> tmpTopicPartSubList =
                    currentSubInfo.putIfAbsent(clientId, topicPartSubList);
            if (tmpTopicPartSubList != null) {
                topicPartSubList = tmpTopicPartSubList;
            }
        }
        long rebalanceId = request.hasEvent()
                ? request.getEvent().getRebalanceId() : TBaseConstants.META_VALUE_UNDEFINED;
        List<String> strSubInfoList = request.getSubscribeInfoList();
        if (request.getReportSubscribeInfo()) {
            List<SubscribeInfo> infoList = DataConverterUtil.convertSubInfo(strSubInfoList);
            if (!checkIfConsist(topicPartSubList, infoList)) {
                topicPartSubList.clear();
                for (SubscribeInfo info : infoList) {
                    Map<String, Partition> partMap =
                            topicPartSubList.get(info.getTopic());
                    if (partMap == null) {
                        partMap = new HashMap<>();
                        topicPartSubList.put(info.getTopic(), partMap);
                    }
                    Partition regPart =
                            new Partition(info.getPartition().getBroker(),
                                    info.getTopic(), info.getPartitionId());
                    partMap.put(regPart.getPartitionKey(), regPart);
                }
                if (rebalanceId <= 0) {
                    logger.warn(strBuffer.append("[Consistent Warn]").append(clientId)
                            .append(" sub info is not consistent with master.").toString());
                    strBuffer.delete(0, strBuffer.length());
                }
            }
        }
        //
        if (rebalanceId > 0) {
            ConsumerEvent processedEvent =
                    new ConsumerEvent(request.getEvent().getRebalanceId(),
                            EventType.valueOf(request.getEvent().getOpType()),
                            DataConverterUtil.convertSubInfo(request.getEvent().getSubscribeInfoList()),
                            EventStatus.valueOf(request.getEvent().getStatus()));
            strBuffer.append("[Event Processed] ");
            logger.info(processedEvent.toStrBuilder(strBuffer).toString());
            strBuffer.delete(0, strBuffer.length());
            try {
                consumeGroupInfo.settAllocated();
                consumerEventManager.removeFirst(clientId);
            } catch (Throwable e) {
                logger.warn("Unknown exception for remove first event:", e);
            }
        }
        //
        ConsumerEvent event =
                consumerEventManager.peek(clientId);
        if (event != null
                && event.getStatus() != EventStatus.PROCESSING) {
            event.setStatus(EventStatus.PROCESSING);
            strBuffer.append("[Push Consumer Event]");
            logger.info(event.toStrBuilder(strBuffer).toString());
            strBuffer.delete(0, strBuffer.length());
            EventProto.Builder eventProtoBuilder =
                    EventProto.newBuilder();
            eventProtoBuilder.setRebalanceId(event.getRebalanceId());
            eventProtoBuilder.setOpType(event.getType().getValue());
            eventProtoBuilder.addAllSubscribeInfo(
                    DataConverterUtil.formatSubInfo(event.getSubscribeInfoList()));
            EventProto eventProto = eventProtoBuilder.build();
            builder.setEvent(eventProto);
        }
        if (request.hasDefFlowCheckId()
                || request.hasGroupFlowCheckId()) {
            builder.setSsdStoreId(TBaseConstants.META_VALUE_UNDEFINED);
            builder.setQryPriorityId(TBaseConstants.META_VALUE_UNDEFINED);
            builder.setDefFlowCheckId(TBaseConstants.META_VALUE_UNDEFINED);
            builder.setGroupFlowCheckId(TBaseConstants.META_VALUE_UNDEFINED);
            builder.setDefFlowControlInfo(" ");
            builder.setGroupFlowControlInfo(" ");
            ClusterSettingEntity defSetting =
                    defMetaDataService.getClusterDefSetting(false);
            GroupResCtrlEntity groupResCtrlConf =
                    defMetaDataService.getGroupCtrlConf(groupName);
            if (defSetting.enableFlowCtrl()) {
                builder.setDefFlowCheckId(defSetting.getSerialId());
                if (request.getDefFlowCheckId() != defSetting.getSerialId()) {
                    builder.setDefFlowControlInfo(defSetting.getGloFlowCtrlRuleInfo());
                }
            }
            if (groupResCtrlConf != null
                    && groupResCtrlConf.isFlowCtrlEnable()) {
                builder.setGroupFlowCheckId(groupResCtrlConf.getSerialId());
                builder.setQryPriorityId(groupResCtrlConf.getQryPriorityId());
                if (request.getGroupFlowCheckId() != groupResCtrlConf.getSerialId()) {
                    builder.setGroupFlowControlInfo(groupResCtrlConf.getFlowCtrlInfo());
                }
            }
        }
        builder.setAuthorizedInfo(genAuthorizedInfo(certResult.authorizedToken, false));
        builder.setNotAllocated(consumeGroupInfo.isNotAllocate());
        builder.setSuccess(true);
        builder.setErrCode(TErrCodeConstants.SUCCESS);
        builder.setErrMsg("OK!");
        return builder.build();
    }

    /**
     * Consumer close request with master
     *
     * @param request
     * @param rmtAddress
     * @param overtls
     * @return close response
     * @throws Exception
     */
    @Override
    public CloseResponseM2C consumerCloseClientC2M(CloseRequestC2M request,
                                                   final String rmtAddress,
                                                   boolean overtls) throws Exception {
        StringBuilder strBuffer = new StringBuilder(512);
        CloseResponseM2C.Builder builder = CloseResponseM2C.newBuilder();
        builder.setSuccess(false);
        CertifiedResult certResult =
                serverAuthHandler.identityValidUserInfo(request.getAuthInfo(), false);
        if (!certResult.result) {
            builder.setErrCode(certResult.errCode);
            builder.setErrMsg(certResult.errInfo);
            return builder.build();
        }
        ParamCheckResult paramCheckResult =
                PBParameterUtils.checkClientId(request.getClientId(), strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        final String clientId = (String) paramCheckResult.checkData;
        paramCheckResult = PBParameterUtils.checkGroupName(request.getGroupName(), strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        final String groupName = (String) paramCheckResult.checkData;
        checkNodeStatus(clientId, strBuffer);
        String nodeId = getConsumerKey(groupName, clientId);
        logger.info(strBuffer.append("[Consumer Closed]").append(nodeId)
                .append(", isOverTLS=").append(overtls).toString());
        new ReleaseConsumer().run(nodeId, false);
        heartbeatManager.unRegConsumerNode(nodeId);
        builder.setSuccess(true);
        builder.setErrCode(TErrCodeConstants.SUCCESS);
        builder.setErrMsg("OK!");
        return builder.build();
    }

    /**
     * Broker register request with master
     *
     * @param request
     * @param rmtAddress
     * @param overtls
     * @return register response
     * @throws Exception
     */
    @Override
    public RegisterResponseM2B brokerRegisterB2M(RegisterRequestB2M request,
                                                 final String rmtAddress,
                                                 boolean overtls) throws Exception {
        // #lizard forgives
        RegisterResponseM2B.Builder builder = RegisterResponseM2B.newBuilder();
        builder.setSuccess(false);
        builder.setStopRead(false);
        builder.setStopWrite(false);
        builder.setTakeConfInfo(false);
        // auth
        CertifiedResult cfResult =
                serverAuthHandler.identityValidBrokerInfo(request.getAuthInfo());
        if (!cfResult.result) {
            builder.setErrCode(cfResult.errCode);
            builder.setErrMsg(cfResult.errInfo);
            return builder.build();
        }
        ProcessResult result = new ProcessResult();
        final StringBuilder strBuffer = new StringBuilder(512);
        // get clientId and check valid
        ParamCheckResult paramCheckResult =
                PBParameterUtils.checkClientId(request.getClientId(), strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        final String clientId = (String) paramCheckResult.checkData;
        // check authority
        checkNodeStatus(clientId, strBuffer);
        // get optional filed
        ClusterSettingEntity defSetting =
                defMetaDataService.getClusterDefSetting(false);
        final long reFlowCtrlId = request.hasFlowCheckId()
                ? request.getFlowCheckId() : TBaseConstants.META_VALUE_UNDEFINED;
        final int qryPriorityId = request.hasQryPriorityId()
                ? request.getQryPriorityId() : TBaseConstants.META_VALUE_UNDEFINED;
        int tlsPort = request.hasTlsPort()
                ? request.getTlsPort() : defSetting.getBrokerTLSPort();
        // build broker info
        BrokerInfo brokerInfo =
                new BrokerInfo(clientId, request.getEnableTls(), tlsPort);
        // register broker run status info
        if (!brokerRunManager.brokerRegister2M(clientId, brokerInfo,
                request.getCurBrokerConfId(), request.getConfCheckSumId(),
                true, request.getBrokerDefaultConfInfo(),
                request.getBrokerTopicSetConfInfoList(), request.getBrokerOnline(),
                overtls, strBuffer, result)) {
            builder.setErrCode(result.getErrCode());
            builder.setErrMsg(result.getErrMsg());
            return builder.build();
        }
        // print broker register log
        logger.info(strBuffer.append("[Broker Register] ").append(clientId)
            .append(" report, configureId=").append(request.getCurBrokerConfId())
            .append(",readStatusRpt=").append(request.getReadStatusRpt())
            .append(",writeStatusRpt=").append(request.getWriteStatusRpt())
            .append(",isTlsEnable=").append(brokerInfo.isEnableTLS())
            .append(",TLSport=").append(brokerInfo.getTlsPort())
            .append(",FlowCtrlId=").append(reFlowCtrlId)
            .append(",qryPriorityId=").append(qryPriorityId)
            .append(",checksumId=").append(request.getConfCheckSumId()).toString());
        strBuffer.delete(0, strBuffer.length());
        // response
        builder.setSuccess(true);
        builder.setErrCode(TErrCodeConstants.SUCCESS);
        builder.setErrMsg("OK!");
        // begin:  deprecated when brokers version equal to current master version
        builder.setAuthorizedInfo(genAuthorizedInfo(null, true));
        // end deprecated
        builder.setBrokerAuthorizedInfo(genBrokerAuthorizedInfo(null));
        EnableBrokerFunInfo.Builder enableInfo = EnableBrokerFunInfo.newBuilder();
        enableInfo.setEnableVisitTokenCheck(masterConfig.isStartVisitTokenCheck());
        enableInfo.setEnableProduceAuthenticate(masterConfig.isStartProduceAuthenticate());
        enableInfo.setEnableProduceAuthorize(masterConfig.isStartProduceAuthorize());
        enableInfo.setEnableConsumeAuthenticate(masterConfig.isStartConsumeAuthenticate());
        enableInfo.setEnableConsumeAuthorize(masterConfig.isStartConsumeAuthorize());
        builder.setEnableBrokerInfo(enableInfo);
        brokerRunManager.setRegisterDownConfInfo(brokerInfo.getBrokerId(), strBuffer, builder);
        builder.setSsdStoreId(TBaseConstants.META_VALUE_UNDEFINED);
        ClientMaster.ClusterConfig.Builder clusterConfigBuilder =
                buildClusterConfig(request.getClsConfig());
        if (clusterConfigBuilder != null) {
            builder.setClsConfig(clusterConfigBuilder);
        }
        if (request.hasFlowCheckId()) {
            builder.setQryPriorityId(defSetting.getQryPriorityId());
            builder.setFlowCheckId(defSetting.getSerialId());
            if (reFlowCtrlId != defSetting.getSerialId()) {
                if (defSetting.enableFlowCtrl()) {
                    builder.setFlowControlInfo(defSetting.getGloFlowCtrlRuleInfo());
                } else {
                    builder.setFlowControlInfo(" ");
                }
            }
        }
        logger.info(strBuffer.append("[Broker Register] ").append(clientId)
                .append(", isOverTLS=").append(overtls).toString());
        return builder.build();
    }

    /**
     * Broker heartbeat request with master
     *
     * @param request
     * @param rmtAddress
     * @param overtls
     * @return heartbeat response
     * @throws Exception
     */
    @Override
    public HeartResponseM2B brokerHeartbeatB2M(HeartRequestB2M request,
                                               final String rmtAddress,
                                               boolean overtls) throws Exception {
        // #lizard forgives
        // set response field
        HeartResponseM2B.Builder builder = HeartResponseM2B.newBuilder();
        builder.setSuccess(false);
        builder.setStopRead(false);
        builder.setStopWrite(false);
        builder.setNeedReportData(true);
        builder.setTakeConfInfo(false);
        builder.setTakeRemoveTopicInfo(false);
        builder.setFlowCheckId(TBaseConstants.META_VALUE_UNDEFINED);
        builder.setQryPriorityId(TBaseConstants.META_VALUE_UNDEFINED);
        builder.setCurBrokerConfId(TBaseConstants.META_VALUE_UNDEFINED);
        builder.setConfCheckSumId(TBaseConstants.META_VALUE_UNDEFINED);
        // identity broker info
        CertifiedResult certResult =
                serverAuthHandler.identityValidBrokerInfo(request.getAuthInfo());
        if (!certResult.result) {
            builder.setErrCode(certResult.errCode);
            builder.setErrMsg(certResult.errInfo);
            return builder.build();
        }
        ProcessResult result = new ProcessResult();
        final StringBuilder strBuffer = new StringBuilder(512);
        ParamCheckResult paramCheckResult =
                PBParameterUtils.checkBrokerId(request.getBrokerId(), strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        int brokerId = (int) paramCheckResult.checkData;
        long reFlowCtrlId = request.hasFlowCheckId()
                ? request.getFlowCheckId() : TBaseConstants.META_VALUE_UNDEFINED;
        int qryPriorityId = request.hasQryPriorityId()
                ? request.getQryPriorityId() : TBaseConstants.META_VALUE_UNDEFINED;
        checkNodeStatus(String.valueOf(brokerId), strBuffer);
        if (!brokerRunManager.brokerHeartBeat2M(brokerId,
                request.getCurBrokerConfId(), request.getConfCheckSumId(),
                request.getTakeConfInfo(), request.getBrokerDefaultConfInfo(),
                request.getBrokerTopicSetConfInfoList(), request.getTakeRemovedTopicInfo(),
                request.getRemovedTopicsInfoList(), request.getReadStatusRpt(),
                request.getWriteStatusRpt(), request.getBrokerOnline(),
                strBuffer, result)) {
            builder.setErrCode(result.getErrCode());
            builder.setErrMsg(result.getErrMsg());
            return builder.build();
        }
        if (request.getTakeConfInfo()) {
            strBuffer.append("[Broker Report] heartbeat report: brokerId=")
                .append(request.getBrokerId()).append(", configureId=")
                .append(request.getCurBrokerConfId())
                .append(",readStatusRpt=").append(request.getReadStatusRpt())
                .append(",writeStatusRpt=").append(request.getWriteStatusRpt())
                .append(",checksumId=").append(request.getConfCheckSumId())
                .append(",hasFlowCheckId=").append(request.hasFlowCheckId())
                .append(",reFlowCtrlId=").append(reFlowCtrlId)
                .append(",qryPriorityId=").append(qryPriorityId)
                .append(",brokerOnline=").append(request.getBrokerOnline())
                .append(",default broker configure is ").append(request.getBrokerDefaultConfInfo())
                .append(",broker topic configure is ").append(request.getBrokerTopicSetConfInfoList());
            strBuffer.delete(0, strBuffer.length());
        }
        // create response
        brokerRunManager.setHeatBeatDownConfInfo(brokerId, strBuffer, builder);
        BrokerConfEntity brokerConfEntity =
                defMetaDataService.getBrokerConfByBrokerId(brokerId);
        builder.setTakeRemoveTopicInfo(true);
        builder.addAllRemoveTopicConfInfo(defMetaDataService
                .getBrokerRemovedTopicStrConfigInfo(brokerConfEntity, strBuffer).values());
        builder.setSsdStoreId(TBaseConstants.META_VALUE_UNDEFINED);
        if (request.hasFlowCheckId()) {
            ClusterSettingEntity defSetting =
                    defMetaDataService.getClusterDefSetting(false);
            builder.setFlowCheckId(defSetting.getSerialId());
            builder.setQryPriorityId(defSetting.getQryPriorityId());
            if (reFlowCtrlId != defSetting.getSerialId()) {
                if (defSetting.enableFlowCtrl()) {
                    builder.setFlowControlInfo(defSetting.getGloFlowCtrlRuleInfo());
                } else {
                    builder.setFlowControlInfo(" ");
                }
            }
        }
        ClientMaster.ClusterConfig.Builder clusterConfigBuilder =
                buildClusterConfig(request.getClsConfig());
        if (clusterConfigBuilder != null) {
            builder.setClsConfig(clusterConfigBuilder);
        }
        // begin:  deprecated when brokers version equal to current master version
        builder.setAuthorizedInfo(genAuthorizedInfo(null, true));
        // end deprecated
        builder.setBrokerAuthorizedInfo(genBrokerAuthorizedInfo(null));
        builder.setSuccess(true);
        builder.setErrCode(TErrCodeConstants.SUCCESS);
        builder.setErrMsg("OK!");
        return builder.build();
    }

    /**
     * Broker close request with master
     *
     * @param request
     * @param rmtAddress
     * @param overtls
     * @return close response
     * @throws Throwable
     */
    @Override
    public CloseResponseM2B brokerCloseClientB2M(CloseRequestB2M request,
                                                 final String rmtAddress,
                                                 boolean overtls) throws Throwable {
        ProcessResult result = new ProcessResult();
        StringBuilder strBuffer = new StringBuilder(512);
        CloseResponseM2B.Builder builder = CloseResponseM2B.newBuilder();
        builder.setSuccess(false);
        CertifiedResult cfResult =
                serverAuthHandler.identityValidBrokerInfo(request.getAuthInfo());
        if (!cfResult.result) {
            builder.setErrCode(cfResult.errCode);
            builder.setErrMsg(cfResult.errInfo);
            return builder.build();
        }
        ParamCheckResult paramCheckResult =
                PBParameterUtils.checkBrokerId(request.getBrokerId(), strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        final int brokerId = (int) paramCheckResult.checkData;
        checkNodeStatus(String.valueOf(brokerId), strBuffer);
        if (!brokerRunManager.brokerClose2M(brokerId, strBuffer, result)) {
            builder.setErrCode(result.getErrCode());
            builder.setErrMsg(result.getErrMsg());
            return builder.build();
        }
        builder.setSuccess(true);
        builder.setErrCode(TErrCodeConstants.SUCCESS);
        builder.setErrMsg("OK!");
        return builder.build();
    }

    /**
     * Client balance consumer register request with master
     *
     * @param request
     * @param rmtAddress
     * @param overtls
     * @return register response
     * @throws Exception
     */
    @Override
    public RegisterResponseM2CV2 consumerRegisterC2MV2(RegisterRequestC2MV2 request,
                                                       String rmtAddress,
                                                       boolean overtls) throws Throwable {
        ProcessResult result = new ProcessResult();
        final StringBuilder sBuffer = new StringBuilder(512);
        RegisterResponseM2CV2.Builder builder = RegisterResponseM2CV2.newBuilder();
        CertifiedResult certResult =
                serverAuthHandler.identityValidUserInfo(request.getAuthInfo(), false);
        if (!certResult.result) {
            builder.setErrCode(certResult.errCode);
            builder.setErrMsg(certResult.errInfo);
            return builder.build();
        }
        ParamCheckResult paramCheckResult =
                PBParameterUtils.checkClientId(request.getClientId(), sBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        final String consumerId = (String) paramCheckResult.checkData;
        paramCheckResult = PBParameterUtils.checkHostName(request.getHostName(), sBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        //final String hostName = (String) paramCheckResult.checkData;
        paramCheckResult = PBParameterUtils.checkGroupName(request.getGroupName(), sBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        final String groupName = (String) paramCheckResult.checkData;
        paramCheckResult =
                PBParameterUtils.checkConsumerTopicList(
                        request.getTopicListList(), sBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        final Set<String> reqTopicSet = (Set<String>) paramCheckResult.checkData;
        final Map<String, TreeSet<String>> reqTopicConditions =
                DataConverterUtil.convertTopicConditions(request.getTopicConditionList());
        int sourceCount = request.getSourceCount();
        int nodeId = request.getNodeId();
        if (sourceCount > 0) {
            if (nodeId < 0 || nodeId > (sourceCount - 1)) {
                builder.setErrCode(TErrCodeConstants.BAD_REQUEST);
                builder.setErrMsg("Request nodeId value must be between in [0, sourceCount-1]!");
                return builder.build();
            }
        }
        final String clientJdkVer = request.hasJdkVersion()
                ? request.getJdkVersion() : "";
        final ConsumeType csmType = ConsumeType.CONSUME_CLIENT_REB;
        OpsSyncInfo opsTaskInfo = new OpsSyncInfo();
        if (request.hasOpsTaskInfo()) {
            opsTaskInfo.updOpsSyncInfo(request.getOpsTaskInfo());
        }
        // check master current status
        checkNodeStatus(consumerId, sBuffer);
        ClientSyncInfo clientSyncInfo = new ClientSyncInfo();
        if (request.hasSubRepInfo()) {
            clientSyncInfo.updSubRepInfo(brokerRunManager, request.getSubRepInfo());
        }
        // build consumer object
        ConsumerInfo inConsumerInfo =
                new ConsumerInfo(consumerId, overtls, groupName, csmType,
                        sourceCount, nodeId, reqTopicSet, reqTopicConditions,
                        opsTaskInfo.getCsmFromMaxOffsetCtrlId(), clientSyncInfo);
        // need removed for authorize center begin
        if (!this.defMetaDataService
                .isConsumeTargetAuthorized(consumerId, groupName,
                        reqTopicSet, reqTopicConditions, sBuffer, result)) {
            if (sBuffer.length() > 0) {
                logger.warn(sBuffer.toString());
            }
            builder.setErrCode(result.getErrCode());
            builder.setErrMsg(result.getErrMsg());
            return builder.build();
        }
        // need removed for authorize center end
        // check resource require
        paramCheckResult =
                PBParameterUtils.checkConsumerInputInfo(inConsumerInfo,
                        masterConfig, defMetaDataService, brokerRunManager, sBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        CertifiedResult authorizeResult =
                serverAuthHandler.validConsumerAuthorizeInfo(certResult.userName,
                        groupName, reqTopicSet, reqTopicConditions, rmtAddress);
        if (!authorizeResult.result) {
            builder.setErrCode(authorizeResult.errCode);
            builder.setErrMsg(authorizeResult.errInfo);
            return builder.build();
        }
        Integer lid = null;
        try {
            lid = masterRowLock.getLock(null,
                    StringUtils.getBytesUtf8(consumerId), true);
            if (!consumerHolder.addConsumer(inConsumerInfo, false, sBuffer, paramCheckResult)) {
                builder.setErrCode(paramCheckResult.errCode);
                builder.setErrMsg(paramCheckResult.errMsg);
                return builder.build();
            }
            topicPSInfoManager.addGroupSubTopicInfo(groupName, reqTopicSet);
            heartbeatManager.regConsumerNode(getConsumerKey(groupName, consumerId));
        } catch (IOException e) {
            logger.warn("Failed to lock.", e);
        } finally {
            if (lid != null) {
                this.masterRowLock.releaseRowLock(lid);
            }
        }
        ConsumeGroupInfo consumeGroupInfo =
                consumerHolder.getConsumeGroupInfo(groupName);
        if (consumeGroupInfo == null) {
            logger.warn(sBuffer.append("[Illegal Process] ").append(consumerId)
                    .append(" visit consume group(").append(groupName)
                    .append(" info failure, null information").toString());
            builder.setErrCode(TErrCodeConstants.INTERNAL_SERVER_ERROR);
            builder.setErrMsg(sBuffer.toString());
            sBuffer.delete(0, sBuffer.length());
            return builder.build();
        }
        inConsumerInfo = consumeGroupInfo.getConsumerInfo(consumerId);
        if (inConsumerInfo == null) {
            logger.warn(sBuffer.append("[Illegal Process] ").append(consumerId)
                    .append(" visit consume info failure, null information").toString());
            builder.setErrCode(TErrCodeConstants.INTERNAL_SERVER_ERROR);
            builder.setErrMsg(sBuffer.toString());
            sBuffer.delete(0, sBuffer.length());
            return builder.build();
        }
        Map<String, Map<String, Partition>> topicPartSubMap = new HashMap<>();
        currentSubInfo.put(consumerId, topicPartSubMap);
        Tuple2<Boolean, Set<Partition>> reportInfo = clientSyncInfo.getRepSubInfo();
        if (reportInfo.getF0()) {
            for (Partition info : reportInfo.getF1()) {
                Map<String, Partition> partMap =
                        topicPartSubMap.computeIfAbsent(info.getTopic(), k -> new HashMap<>());
                partMap.put(info.getPartitionKey(), info);
            }
            printReportInfo(consumerId, null, topicPartSubMap, sBuffer);
        }
        logger.info(sBuffer.append("[Consumer Register] ")
                .append(consumerId).append(", isOverTLS=").append(overtls)
                .append(", clientJDKVer=").append(clientJdkVer).toString());
        sBuffer.delete(0, sBuffer.length());
        Tuple2<Long, Map<Integer, String>> brokerStaticInfo =
                brokerRunManager.getBrokerStaticInfo(overtls);
        builder.setBrokerConfigId(brokerStaticInfo.getF0());
        if (clientSyncInfo.getBrokerConfigId()
                != brokerStaticInfo.getF0()) {
            builder.addAllBrokerConfigList(brokerStaticInfo.getF1().values());
        }
        builder.setOpsTaskInfo(buildOpsTaskInfo(consumeGroupInfo, inConsumerInfo, opsTaskInfo));
        builder.setAuthorizedInfo(genAuthorizedInfo(certResult.authorizedToken, false));
        builder.setErrCode(TErrCodeConstants.SUCCESS);
        builder.setErrMsg("OK!");
        return builder.build();
    }

    /**
     * Client balance consumer heartbeat request with master
     *
     * @param request
     * @param rmtAddress
     * @param overtls
     * @return heartbeat response
     * @throws Throwable
     */
    @Override
    public HeartResponseM2CV2 consumerHeartbeatC2MV2(HeartRequestC2MV2 request,
                                                     String rmtAddress,
                                                     boolean overtls) throws Throwable {
        final StringBuilder strBuffer = new StringBuilder(512);
        // response
        HeartResponseM2CV2.Builder builder = HeartResponseM2CV2.newBuilder();
        // identity valid
        CertifiedResult certResult =
                serverAuthHandler.identityValidUserInfo(request.getAuthInfo(), false);
        if (!certResult.result) {
            builder.setErrCode(certResult.errCode);
            builder.setErrMsg(certResult.errInfo);
            return builder.build();
        }
        ParamCheckResult paramCheckResult =
                PBParameterUtils.checkClientId(request.getClientId(), strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        final String clientId = (String) paramCheckResult.checkData;
        paramCheckResult = PBParameterUtils.checkGroupName(request.getGroupName(), strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        final String groupName = (String) paramCheckResult.checkData;
        OpsSyncInfo opsTaskInfo = new OpsSyncInfo();
        if (request.hasOpsTaskInfo()) {
            opsTaskInfo.updOpsSyncInfo(request.getOpsTaskInfo());
        }
        // check master current status
        checkNodeStatus(clientId, strBuffer);
        ClientSyncInfo clientSyncInfo = new ClientSyncInfo();
        if (request.hasSubRepInfo()) {
            clientSyncInfo.updSubRepInfo(brokerRunManager, request.getSubRepInfo());
        }
        ConsumeGroupInfo consumeGroupInfo = consumerHolder.getConsumeGroupInfo(groupName);
        if (consumeGroupInfo == null) {
            builder.setErrCode(TErrCodeConstants.HB_NO_NODE);
            builder.setErrMsg(strBuffer.append("Not found groupName ")
                    .append(groupName).append(" in holder!").toString());
            return builder.build();
        }
        ConsumerInfo inConsumerInfo =
                consumeGroupInfo.getConsumerInfo(clientId);
        if (inConsumerInfo == null) {
            builder.setErrCode(TErrCodeConstants.HB_NO_NODE);
            builder.setErrMsg(strBuffer.append("Not found client ").append(clientId)
                    .append(" in group(").append(groupName).append(")").toString());
            strBuffer.delete(0, strBuffer.length());
            return builder.build();
        }
        // authorize check
        CertifiedResult authorizeResult =
                serverAuthHandler.validConsumerAuthorizeInfo(certResult.userName,
                        groupName, consumeGroupInfo.getTopicSet(),
                        consumeGroupInfo.getTopicConditions(), rmtAddress);
        if (!authorizeResult.result) {
            builder.setErrCode(authorizeResult.errCode);
            builder.setErrMsg(authorizeResult.errInfo);
            return builder.build();
        }
        // heartbeat check
        try {
            heartbeatManager.updConsumerNode(getConsumerKey(groupName, clientId));
        } catch (HeartbeatException e) {
            builder.setErrCode(TErrCodeConstants.HB_NO_NODE);
            builder.setErrMsg(strBuffer
                    .append("Update consumer node exception:")
                    .append(e.getMessage()).toString());
            return builder.build();
        }
        inConsumerInfo.updClientReportInfo(opsTaskInfo.getCsmFromMaxOffsetCtrlId(),
                clientSyncInfo.getLstAssignedTime(), clientSyncInfo.getTopicMetaInfoId());
        Tuple2<Boolean, Set<Partition>> reportInfo = clientSyncInfo.getRepSubInfo();
        if (reportInfo.getF0()) {
            Map<String, Map<String, Partition>> curPartSubMap =
                    currentSubInfo.get(clientId);
            Map<String, Map<String, Partition>> newPartSubMap = new HashMap<>();
            for (Partition info : reportInfo.getF1()) {
                Map<String, Partition> partMap =
                        newPartSubMap.computeIfAbsent(info.getTopic(), k -> new HashMap<>());
                partMap.put(info.getPartitionKey(), info);
            }
            printReportInfo(clientId, curPartSubMap, newPartSubMap, strBuffer);
            currentSubInfo.put(clientId, newPartSubMap);
        }
        Tuple2<Long, Map<Integer, String>> brokerStaticInfo =
                brokerRunManager.getBrokerStaticInfo(overtls);
        builder.setBrokerConfigId(brokerStaticInfo.getF0());
        if (clientSyncInfo.getBrokerConfigId()
                != brokerStaticInfo.getF0()) {
            builder.addAllBrokerConfigList(brokerStaticInfo.getF1().values());
        }
        Map<String, Map<String, Partition>> curPartMap = currentSubInfo.get(clientId);
        if (inConsumerInfo.getLstAssignedTime() >= 0
                && (curPartMap != null && !curPartMap.isEmpty())
                && (System.currentTimeMillis() - inConsumerInfo.getLstAssignedTime()
                > masterConfig.getMaxMetaForceUpdatePeriodMs())) {
            Tuple2<Long, List<String>> topicMetaInfoTuple = consumeGroupInfo.getTopicMetaInfo();
            builder.setTopicMetaInfoId(topicMetaInfoTuple.getF0());
            if (topicMetaInfoTuple.getF0() != clientSyncInfo.getTopicMetaInfoId()) {
                builder.addAllTopicMetaInfoList(topicMetaInfoTuple.getF1());
            }
        }
        builder.setOpsTaskInfo(buildOpsTaskInfo(consumeGroupInfo, inConsumerInfo, opsTaskInfo));
        builder.setAuthorizedInfo(genAuthorizedInfo(certResult.authorizedToken, false));
        builder.setErrCode(TErrCodeConstants.SUCCESS);
        builder.setErrMsg("OK!");
        return builder.build();
    }

    /**
     * Client balance consumer get partition meta information with master
     *
     * @param request
     * @param rmtAddress
     * @param overtls
     * @return close response
     * @throws Exception
     */
    @Override
    public GetPartMetaResponseM2C consumerGetPartMetaInfoC2M(GetPartMetaRequestC2M request,
                                                             String rmtAddress,
                                                             boolean overtls) throws Throwable {
        StringBuilder strBuffer = new StringBuilder(512);
        GetPartMetaResponseM2C.Builder builder = GetPartMetaResponseM2C.newBuilder();
        CertifiedResult certResult =
                serverAuthHandler.identityValidUserInfo(request.getAuthInfo(), false);
        if (!certResult.result) {
            builder.setErrCode(certResult.errCode);
            builder.setErrMsg(certResult.errInfo);
            return builder.build();
        }
        ParamCheckResult paramCheckResult =
                PBParameterUtils.checkClientId(request.getClientId(), strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        final String clientId = (String) paramCheckResult.checkData;
        paramCheckResult = PBParameterUtils.checkGroupName(request.getGroupName(), strBuffer);
        if (!paramCheckResult.result) {
            builder.setErrCode(paramCheckResult.errCode);
            builder.setErrMsg(paramCheckResult.errMsg);
            return builder.build();
        }
        final String groupName = (String) paramCheckResult.checkData;
        final long brokerConfigId = request.getBrokerConfigId();
        final long topicMetaInfoId = request.getTopicMetaInfoId();
        checkNodeStatus(clientId, strBuffer);
        // get control object
        ConsumeGroupInfo consumeGroupInfo =
                consumerHolder.getConsumeGroupInfo(groupName);
        if (consumeGroupInfo == null) {
            builder.setErrCode(TErrCodeConstants.HB_NO_NODE);
            builder.setErrMsg(strBuffer.append("Not found groupName ")
                    .append(groupName).append(" in holder!").toString());
            strBuffer.delete(0, strBuffer.length());
            return builder.build();
        }
        ConsumerInfo inConsumerInfo =
                consumeGroupInfo.getConsumerInfo(clientId);
        if (inConsumerInfo == null) {
            builder.setErrCode(TErrCodeConstants.HB_NO_NODE);
            builder.setErrMsg(strBuffer.append("Not found client ").append(clientId)
                    .append(" in group(").append(groupName).append(")").toString());
            strBuffer.delete(0, strBuffer.length());
            return builder.build();
        }
        // heartbeat check
        try {
            heartbeatManager.updConsumerNode(getConsumerKey(groupName, clientId));
        } catch (HeartbeatException e) {
            builder.setErrCode(TErrCodeConstants.HB_NO_NODE);
            builder.setErrMsg(strBuffer
                    .append("Update consumer node exception:")
                    .append(e.getMessage()).toString());
            return builder.build();
        }
        Tuple2<Long, List<String>> topicMetaInfoTuple = consumeGroupInfo.getTopicMetaInfo();
        if (topicMetaInfoTuple.getF0() == TBaseConstants.META_VALUE_UNDEFINED) {
            freshTopicMetaInfo(consumeGroupInfo, strBuffer);
            topicMetaInfoTuple = consumeGroupInfo.getTopicMetaInfo();
        }
        builder.setTopicMetaInfoId(topicMetaInfoTuple.getF0());
        if (topicMetaInfoTuple.getF0() != topicMetaInfoId) {
            builder.addAllTopicMetaInfoList(topicMetaInfoTuple.getF1());
        }
        Tuple2<Long, Map<Integer, String>> brokerStaticInfo =
                brokerRunManager.getBrokerStaticInfo(overtls);
        builder.setBrokerConfigId(brokerStaticInfo.getF0());
        if (brokerConfigId != brokerStaticInfo.getF0()) {
            builder.addAllBrokerConfigList(brokerStaticInfo.getF1().values());
        }
        builder.setErrCode(TErrCodeConstants.SUCCESS);
        builder.setErrMsg("OK!");
        return builder.build();
    }

    /**
     * Generate consumer id
     *
     * @param group
     * @param consumerId
     * @return
     */
    private String getConsumerKey(String group, String consumerId) {
        return new StringBuilder(512).append(consumerId)
                .append("@").append(group).toString();
    }

    /**
     * Get producer topic partition info
     *
     * @param producerId
     * @return
     */
    private Map<String, String> getProducerTopicPartitionInfo(String producerId) {
        ProducerInfo producerInfo =
                producerHolder.getProducerInfo(producerId);
        if (producerInfo == null) {
            return new HashMap<>();
        }
        Set<String> producerInfoTopicSet =
                producerInfo.getTopicSet();
        if ((producerInfoTopicSet == null)
                || (producerInfoTopicSet.isEmpty())) {
            return new HashMap<>();
        }
        return brokerRunManager.getPubBrokerAcceptPubPartInfo(producerInfoTopicSet);
    }

    @Override
    public void run() {
        try {
            if (!this.stopped) {
                this.balancerChore = startBalancerChore(this);
                initialized = true;
                while (!this.stopped) {
                    stopSleeper.sleep();
                }
            }
        } catch (Throwable e) {
            stopChores();
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Load balance
     */
    private void balance(final TMaster tMaster) {
        final StringBuilder strBuffer = new StringBuilder(512);
        final long balanceId = idGenerator.incrementAndGet();
        if (defMetaDataService != null) {
            logger.info(strBuffer.append("[Balance Start] ").append(balanceId)
                    .append(", isMaster=").append(defMetaDataService.isSelfMaster())
                    .append(", isPrimaryNodeActive=")
                    .append(defMetaDataService.isPrimaryNodeActive()).toString());
        } else {
            logger.info(strBuffer.append("[Balance Start] ").append(balanceId)
                    .append(", BDB service is null isMaster= false, isPrimaryNodeActive=false").toString());
        }
        strBuffer.delete(0, strBuffer.length());
        // process client-balance
        processClientBalanceMetaInfo(balanceId, strBuffer);
        // process server-balance
        processServerBalance(tMaster, balanceId, strBuffer);
        logger.info(strBuffer.append("[Balance End] ").append(balanceId).toString());
    }

    private void processServerBalance(TMaster tMaster,
                                      long balanceId,
                                      StringBuilder sBuffer) {
        int curDoingTasks = this.curSvrBalanceParal.get();
        if (curDoingTasks > 0) {
            logger.info(sBuffer.append("[Svr-Balance End] ").append(balanceId)
                    .append(" the Server-Balance has ").append(curDoingTasks)
                    .append(" task(s) in progress!").toString());
            sBuffer.delete(0, sBuffer.length());
            return;
        }
        final boolean isStartBalance = startupBalance;
        List<String> groupsNeedToBalance = isStartBalance
                ? consumerHolder.getAllServerBalanceGroups() : getNeedToBalanceGroups(sBuffer);
        sBuffer.delete(0, sBuffer.length());
        int balanceTaskCnt = groupsNeedToBalance.size();
        if (balanceTaskCnt > 0) {
            // calculate process count
            int unitNum = (balanceTaskCnt + masterConfig.getRebalanceParallel() - 1)
                    / masterConfig.getRebalanceParallel();
            // start processor to do balance;
            int startIndex = 0;
            int endIndex = 0;
            // set parallel balance signal
            final long startBalanceTime = System.currentTimeMillis();
            curSvrBalanceParal.set(masterConfig.getRebalanceParallel());
            for (int i = 0; i < masterConfig.getRebalanceParallel(); i++) {
                // get groups need to balance
                startIndex = Math.min((i) * unitNum, balanceTaskCnt);
                endIndex = Math.min((i + 1) * unitNum, balanceTaskCnt);
                final List<String> subGroups = groupsNeedToBalance.subList(startIndex, endIndex);
                if (subGroups.isEmpty()) {
                    if (curSvrBalanceParal.decrementAndGet() == 0) {
                        MasterSrvStatsHolder.updSvrBalanceDurations(
                                System.currentTimeMillis() - startBalanceTime);
                    }
                    continue;
                }
                // execute balance
                this.svrExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (subGroups.isEmpty()) {
                                return;
                            }
                            // first process reset rebalance task;
                            try {
                                tMaster.processResetbalance(balanceId,
                                        isStartBalance, subGroups);
                            } catch (Throwable e) {
                                logger.warn(new StringBuilder(1024)
                                        .append("[Svr-Balance processor] Error during reset-reb,")
                                        .append("the groups that may be affected are ")
                                        .append(subGroups).append(",error is ")
                                        .append(e).toString());
                            }
                            if (tMaster.isStopped()) {
                                return;
                            }
                            // second process normal balance task;
                            try {
                                tMaster.processRebalance(balanceId,
                                        isStartBalance, subGroups);
                            } catch (Throwable e) {
                                logger.warn(new StringBuilder(1024)
                                        .append("[Svr-Balance processor] Error during normal-reb,")
                                        .append("the groups that may be affected are ")
                                        .append(subGroups).append(",error is ")
                                        .append(e).toString());
                            }
                        } catch (Throwable e) {
                            logger.warn("[Svr-Balance processor] Error during process", e);
                        } finally {
                            if (curSvrBalanceParal.decrementAndGet() == 0) {
                                MasterSrvStatsHolder.updSvrBalanceDurations(
                                        System.currentTimeMillis() - startBalanceTime);
                            }
                        }
                    }
                });
            }
        }
        startupBalance = false;
        logger.info(sBuffer.append("[Svr-Balance End] ").append(balanceId).toString());
        sBuffer.delete(0, sBuffer.length());
    }

    private void processClientBalanceMetaInfo(long balanceId, StringBuilder sBuffer) {
        int curDoingTasks = this.curCltBalanceParal.get();
        if (curDoingTasks > 0) {
            logger.info(sBuffer.append("[Clt-Balance End] ").append(balanceId)
                    .append(" the Client-Balance has ").append(curDoingTasks)
                    .append(" task(s) in progress!").toString());
            sBuffer.delete(0, sBuffer.length());
            return;
        }
        List<String> clientGroups = consumerHolder.getAllClientBalanceGroups();
        if (!clientGroups.isEmpty()) {
            int balanceTaskCnt = clientGroups.size();
            // calculate process count
            int unitNum = (balanceTaskCnt + masterConfig.getRebalanceParallel() - 1)
                    / masterConfig.getRebalanceParallel();
            // start processor to do balance;
            int startIndex = 0;
            int endIndex = 0;
            // set parallel balance signal
            curCltBalanceParal.set(masterConfig.getRebalanceParallel());
            for (int i = 0; i < masterConfig.getRebalanceParallel(); i++) {
                // get groups need to rebalance
                startIndex = Math.min((i) * unitNum, balanceTaskCnt);
                endIndex = Math.min((i + 1) * unitNum, balanceTaskCnt);
                final List<String> subGroups = clientGroups.subList(startIndex, endIndex);
                if (subGroups.isEmpty()) {
                    curCltBalanceParal.decrementAndGet();
                    continue;
                }
                // execute balance
                this.cltExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (subGroups.isEmpty()) {
                                return;
                            }
                            ConsumeGroupInfo consumeGroupInfo;
                            StringBuilder sBuffer2 = new StringBuilder(512);
                            for (String groupName : subGroups) {
                                consumeGroupInfo =
                                        consumerHolder.getConsumeGroupInfo(groupName);
                                if (consumeGroupInfo == null) {
                                    continue;
                                }
                                freshTopicMetaInfo(consumeGroupInfo, sBuffer2);
                            }
                        } catch (Throwable e) {
                            logger.warn("[Clt-Balance processor] Error during process", e);
                        } finally {
                            curCltBalanceParal.decrementAndGet();
                        }
                    }
                });
            }
        }
        logger.info(sBuffer.append("[Clt-Balance End] ").append(balanceId).toString());
        sBuffer.delete(0, sBuffer.length());
    }

    public void freshTopicMetaInfo(ConsumeGroupInfo consumeGroupInfo, StringBuilder sBuffer) {
        Map<String, String> topicMetaInfoMap =
                getTopicConfigInfo(consumeGroupInfo, sBuffer);
        consumeGroupInfo.updCsmTopicMetaInfo(topicMetaInfoMap);
    }

    private Map<String, String> getTopicConfigInfo(ConsumeGroupInfo groupInfo,
                                                   StringBuilder sBuffer) {
        Map<String, String> result = new HashMap<>();
        if (groupInfo.getTopicSet() == null || groupInfo.getTopicSet().isEmpty()) {
            return result;
        }
        Map<String, List<TopicDeployEntity>> topicDeployInfoMap =
                defMetaDataService.getTopicConfMapByTopicAndBrokerIds(
                        groupInfo.getTopicSet(), null);
        if (topicDeployInfoMap == null || topicDeployInfoMap.isEmpty()) {
            return result;
        }
        int count = 0;
        int statusId = 0;
        TopicInfo topicInfo;
        Set<String> fbdTopicSet =
                defMetaDataService.getDisableTopicByGroupName(groupInfo.getGroupName());
        for (Map.Entry<String, List<TopicDeployEntity>> entry : topicDeployInfoMap.entrySet()) {
            if (entry == null) {
                continue;
            }
            count = 0;
            statusId = 0;
            for (TopicDeployEntity deployInfo : entry.getValue()) {
                topicInfo =
                        brokerRunManager.getPubBrokerTopicInfo(
                                deployInfo.getBrokerId(), deployInfo.getTopicName());
                if (topicInfo != null
                        && topicInfo.isAcceptSubscribe()
                        && !fbdTopicSet.contains(topicInfo.getTopic())) {
                    statusId = 1;
                }
                if (count++ == 0) {
                    sBuffer.append(entry.getKey()).append(TokenConstants.SEGMENT_SEP);
                } else {
                    sBuffer.append(TokenConstants.ARRAY_SEP);
                }
                sBuffer.append(deployInfo.getBrokerId()).append(TokenConstants.ATTR_SEP)
                        .append(deployInfo.getNumTopicStores()).append(TokenConstants.ATTR_SEP)
                        .append(deployInfo.getNumPartitions()).append(TokenConstants.ATTR_SEP)
                        .append(statusId);
            }
            if (count > 0) {
                result.put(entry.getKey(), sBuffer.toString());
            }
            sBuffer.delete(0, sBuffer.length());
        }
        return result;
    }

    /**
     * process unReset group balance
     *
     * @param rebalanceId   the re-balance id
     * @param isFirstReb    whether is first re-balance
     * @param groups        the need re-balance group set
     */
    public void processRebalance(long rebalanceId, boolean isFirstReb, List<String> groups) {
        // #lizard forgives
        Map<String, Map<String, List<Partition>>> finalSubInfoMap = null;
        final StringBuilder strBuffer = new StringBuilder(512);
        // choose different load balance strategy
        if (isFirstReb) {
            finalSubInfoMap = this.loadBalancer.bukAssign(consumerHolder,
                    brokerRunManager, groups, defMetaDataService, strBuffer);
        } else {
            finalSubInfoMap = this.loadBalancer.balanceCluster(currentSubInfo,
                    consumerHolder, brokerRunManager, groups, defMetaDataService, strBuffer);
        }
        // allocate partitions to consumers
        for (Map.Entry<String, Map<String, List<Partition>>> entry : finalSubInfoMap.entrySet()) {
            if (entry == null) {
                continue;
            }
            String consumerId = entry.getKey();
            if (consumerId == null) {
                continue;
            }
            ConsumerInfo consumerInfo =
                    consumerHolder.getConsumerInfo(consumerId);
            if (consumerInfo == null) {
                continue;
            }
            Set<String> blackTopicSet =
                    defMetaDataService.getDisableTopicByGroupName(consumerInfo.getGroupName());
            Map<String, List<Partition>> topicSubPartMap = entry.getValue();
            List<SubscribeInfo> deletedSubInfoList = new ArrayList<>();
            List<SubscribeInfo> addedSubInfoList = new ArrayList<>();
            for (Map.Entry<String, List<Partition>> topicEntry : topicSubPartMap.entrySet()) {
                if (topicEntry == null) {
                    continue;
                }
                String topic = topicEntry.getKey();
                List<Partition> finalPartList = topicEntry.getValue();
                Map<String, Partition> currentPartMap = null;
                Map<String, Map<String, Partition>> curTopicSubInfoMap =
                        currentSubInfo.get(consumerId);
                if (curTopicSubInfoMap == null || curTopicSubInfoMap.get(topic) == null) {
                    currentPartMap = new HashMap<>();
                } else {
                    currentPartMap = curTopicSubInfoMap.get(topic);
                    if (currentPartMap == null) {
                        currentPartMap = new HashMap<>();
                    }
                }
                if (consumerInfo.isOverTLS()) {
                    for (Partition currentPart : currentPartMap.values()) {
                        if (!blackTopicSet.contains(currentPart.getTopic())) {
                            boolean found = false;
                            for (Partition newPart : finalPartList) {
                                if (newPart.getPartitionFullStr(true)
                                        .equals(currentPart.getPartitionFullStr(true))) {
                                    found = true;
                                    break;
                                }
                            }
                            if (found) {
                                continue;
                            }
                        }
                        deletedSubInfoList
                                .add(new SubscribeInfo(consumerId, consumerInfo.getGroupName(),
                                        consumerInfo.isOverTLS(), currentPart));
                    }
                    for (Partition finalPart : finalPartList) {
                        if (!blackTopicSet.contains(finalPart.getTopic())) {
                            boolean found = false;
                            for (Partition curPart : currentPartMap.values()) {
                                if (finalPart.getPartitionFullStr(true)
                                        .equals(curPart.getPartitionFullStr(true))) {
                                    found = true;
                                    break;
                                }
                            }
                            if (found) {
                                continue;
                            }
                            addedSubInfoList.add(new SubscribeInfo(consumerId,
                                    consumerInfo.getGroupName(), true, finalPart));
                        }
                    }
                } else {
                    for (Partition currentPart : currentPartMap.values()) {
                        if ((blackTopicSet.contains(currentPart.getTopic()))
                                || (!finalPartList.contains(currentPart))) {
                            deletedSubInfoList.add(new SubscribeInfo(consumerId,
                                    consumerInfo.getGroupName(), false, currentPart));
                        }
                    }
                    for (Partition finalPart : finalPartList) {
                        if ((currentPartMap.get(finalPart.getPartitionKey()) == null)
                                && (!blackTopicSet.contains(finalPart.getTopic()))) {
                            addedSubInfoList.add(new SubscribeInfo(consumerId,
                                    consumerInfo.getGroupName(), false, finalPart));
                        }
                    }
                }
            }
            boolean isDelEmpty = deletedSubInfoList.isEmpty();
            boolean isAddEmtpy = addedSubInfoList.isEmpty();
            if (!isDelEmpty) {
                EventType opType =
                        (!isAddEmtpy) ? EventType.DISCONNECT : EventType.ONLY_DISCONNECT;
                consumerEventManager
                        .addDisconnectEvent(consumerId,
                                new ConsumerEvent(rebalanceId, opType,
                                        deletedSubInfoList, EventStatus.TODO));
                for (SubscribeInfo info : deletedSubInfoList) {
                    logger.info(strBuffer.append("[Disconnect]")
                            .append(info.toString()).toString());
                    strBuffer.delete(0, strBuffer.length());
                }
            }
            if (!isAddEmtpy) {
                EventType opType =
                        (!isDelEmpty) ? EventType.CONNECT : EventType.ONLY_CONNECT;
                consumerEventManager
                        .addConnectEvent(consumerId,
                                new ConsumerEvent(rebalanceId, opType,
                                        addedSubInfoList, EventStatus.TODO));
                for (SubscribeInfo info : addedSubInfoList) {
                    logger.info(strBuffer.append("[Connect]")
                            .append(info.toString()).toString());
                    strBuffer.delete(0, strBuffer.length());
                }
            }
        }
    }

    /**
     * process Reset balance
     */
    public void processResetbalance(long rebalanceId, boolean isFirstReb, List<String> groups) {
        // #lizard forgives
        final StringBuilder strBuffer = new StringBuilder(512);
        Map<String, Map<String, Map<String, Partition>>> finalSubInfoMap = null;
        // choose different load balance strategy
        if (isFirstReb) {
            finalSubInfoMap =  this.loadBalancer.resetBukAssign(consumerHolder,
                    brokerRunManager, groups, this.defMetaDataService, strBuffer);
        } else {
            finalSubInfoMap = this.loadBalancer.resetBalanceCluster(currentSubInfo,
                    consumerHolder, brokerRunManager, groups, this.defMetaDataService, strBuffer);
        }
        // filter
        for (Map.Entry<String, Map<String, Map<String, Partition>>> entry
                : finalSubInfoMap.entrySet()) {
            if (entry == null) {
                continue;
            }
            String consumerId = entry.getKey();
            if (consumerId == null) {
                continue;
            }
            ConsumerInfo consumerInfo =
                    consumerHolder.getConsumerInfo(consumerId);
            if (consumerInfo == null) {
                continue;
            }
            // allocate partitions to consumers
            Set<String> blackTopicSet =
                    defMetaDataService.getDisableTopicByGroupName(consumerInfo.getGroupName());
            Map<String, Map<String, Partition>> topicSubPartMap = entry.getValue();
            List<SubscribeInfo> deletedSubInfoList = new ArrayList<>();
            List<SubscribeInfo> addedSubInfoList = new ArrayList<>();
            for (Map.Entry<String, Map<String, Partition>> topicEntry : topicSubPartMap.entrySet()) {
                if (topicEntry == null) {
                    continue;
                }
                String topic = topicEntry.getKey();
                Map<String, Partition> finalPartMap = topicEntry.getValue();
                Map<String, Partition> currentPartMap = null;
                Map<String, Map<String, Partition>> curTopicSubInfoMap =
                        currentSubInfo.get(consumerId);
                if (curTopicSubInfoMap == null
                        || curTopicSubInfoMap.get(topic) == null) {
                    currentPartMap = new HashMap<>();
                } else {
                    currentPartMap = curTopicSubInfoMap.get(topic);
                    if (currentPartMap == null) {
                        currentPartMap = new HashMap<>();
                    }
                }
                // filter
                for (Partition currentPart : currentPartMap.values()) {
                    if ((blackTopicSet.contains(currentPart.getTopic()))
                            || (finalPartMap.get(currentPart.getPartitionKey()) == null)) {
                        deletedSubInfoList
                                .add(new SubscribeInfo(consumerId, consumerInfo.getGroupName(),
                                        consumerInfo.isOverTLS(), currentPart));
                    }
                }
                for (Partition finalPart : finalPartMap.values()) {
                    if ((currentPartMap.get(finalPart.getPartitionKey()) == null)
                            && (!blackTopicSet.contains(finalPart.getTopic()))) {
                        addedSubInfoList.add(new SubscribeInfo(consumerId,
                                consumerInfo.getGroupName(), consumerInfo.isOverTLS(), finalPart));
                    }
                }
            }
            // generate consumer event
            boolean isDelEmpty = deletedSubInfoList.isEmpty();
            boolean isAddEmtpy = addedSubInfoList.isEmpty();
            if (!isDelEmpty) {
                EventType opType =
                        (!isAddEmtpy) ? EventType.DISCONNECT : EventType.ONLY_DISCONNECT;
                consumerEventManager.addDisconnectEvent(consumerId,
                        new ConsumerEvent(rebalanceId, opType,
                                deletedSubInfoList, EventStatus.TODO));
                for (SubscribeInfo info : deletedSubInfoList) {
                    logger.info(strBuffer.append("[ResetDisconnect]")
                            .append(info.toString()).toString());
                    strBuffer.delete(0, strBuffer.length());
                }
            }
            if (!isAddEmtpy) {
                EventType opType =
                        (!isDelEmpty) ? EventType.CONNECT : EventType.ONLY_CONNECT;
                consumerEventManager.addConnectEvent(consumerId,
                        new ConsumerEvent(rebalanceId, opType,
                                addedSubInfoList, EventStatus.TODO));
                for (SubscribeInfo info : addedSubInfoList) {
                    logger.info(strBuffer.append("[ResetConnect]")
                            .append(info.toString()).toString());
                    strBuffer.delete(0, strBuffer.length());
                }
            }
        }
    }

    /**
     * check if master subscribe info consist consumer subscribe info
     *
     * @param masterSubInfoMap
     * @param consumerSubInfoList
     * @return
     */
    private boolean checkIfConsist(Map<String, Map<String, Partition>> masterSubInfoMap,
                                   List<SubscribeInfo> consumerSubInfoList) {
        int masterInfoSize = 0;
        for (Map.Entry<String, Map<String, Partition>> entry
                : masterSubInfoMap.entrySet()) {
            masterInfoSize += entry.getValue().size();
        }
        if (masterInfoSize != consumerSubInfoList.size()) {
            return false;
        }
        for (SubscribeInfo info : consumerSubInfoList) {
            Map<String, Partition> masterInfoMap =
                    masterSubInfoMap.get(info.getTopic());
            if (masterInfoMap == null || masterInfoMap.isEmpty()) {
                return false;
            }

            if (masterInfoMap.get(info.getPartition().getPartitionKey()) == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * get broker authorized info
     *
     * @param authAuthorizedToken
     * @return
     */
    private MasterBrokerAuthorizedInfo.Builder genBrokerAuthorizedInfo(String authAuthorizedToken) {
        MasterBrokerAuthorizedInfo.Builder authorizedBuilder = MasterBrokerAuthorizedInfo.newBuilder();
        authorizedBuilder.setVisitAuthorizedToken(visitTokenManager.getBrokerVisitTokens());
        if (TStringUtils.isNotBlank(authAuthorizedToken)) {
            authorizedBuilder.setAuthAuthorizedToken(authAuthorizedToken);
        }
        return authorizedBuilder;
    }

    /**
     * get client authorized info
     *
     * @param authAuthorizedToken
     * @return
     */
    private MasterAuthorizedInfo.Builder genAuthorizedInfo(String authAuthorizedToken, boolean isBroker) {
        MasterAuthorizedInfo.Builder authorizedBuilder = MasterAuthorizedInfo.newBuilder();
        if (isBroker) {
            authorizedBuilder.setVisitAuthorizedToken(visitTokenManager.getFreshVisitToken());
        } else {
            authorizedBuilder.setVisitAuthorizedToken(visitTokenManager.getCurVisitToken());
        }
        if (TStringUtils.isNotBlank(authAuthorizedToken)) {
            authorizedBuilder.setAuthAuthorizedToken(authAuthorizedToken);
        }
        return authorizedBuilder;
    }

    /**
     * get need balance group list
     *
     * @param strBuffer
     * @return
     */
    private List<String> getNeedToBalanceGroups(final StringBuilder strBuffer) {
        List<String> groupsNeedToBalance = new ArrayList<>();
        Set<String> groupHasUnfinishedEvent = new HashSet<>();
        if (consumerEventManager.hasEvent()) {
            Set<String> consumerIdSet =
                    consumerEventManager.getUnProcessedIdSet();
            Map<String, TimeoutInfo> heartbeatMap =
                    heartbeatManager.getConsumerRegMap();
            for (String consumerId : consumerIdSet) {
                if (consumerId == null) {
                    continue;
                }
                String group = consumerHolder.getGroupName(consumerId);
                if (group == null) {
                    continue;
                }
                if (heartbeatMap.get(getConsumerKey(group, consumerId)) == null) {
                    continue;
                }
                if (consumerEventManager.getUnfinishedCount(group)
                        >= MAX_BALANCE_DELAY_TIME) {
                    consumerEventManager.removeAll(consumerId);
                    logger.info(strBuffer.append("Unfinished event for group :")
                            .append(group).append(" exceed max balanceDelayTime=")
                            .append(MAX_BALANCE_DELAY_TIME).append(", clear consumer: ")
                            .append(consumerId).append(" unProcessed events.").toString());
                    strBuffer.delete(0, strBuffer.length());
                } else {
                    groupHasUnfinishedEvent.add(group);
                }
            }
        }
        consumerEventManager.updateUnfinishedCountMap(groupHasUnfinishedEvent);
        List<String> allGroups = consumerHolder.getAllServerBalanceGroups();
        if (groupHasUnfinishedEvent.isEmpty()) {
            for (String group : allGroups) {
                if (group != null) {
                    groupsNeedToBalance.add(group);
                }
            }
        } else {
            for (String group : allGroups) {
                if (group != null) {
                    if (!groupHasUnfinishedEvent.contains(group)) {
                        groupsNeedToBalance.add(group);
                    }
                }
            }
        }
        return groupsNeedToBalance;
    }

    /**
     * Stop chores
     */
    private void stopChores() {
        if (this.balancerChore != null) {
            this.balancerChore.interrupt();
        }
    }

    /**
     * build approved client configure
     *
     * @param inClientConfig client reported Configure info
     * @return ApprovedClientConfig
     */
    private ClientMaster.ApprovedClientConfig.Builder buildApprovedClientConfig(
            ClientMaster.ApprovedClientConfig inClientConfig) {
        ClientMaster.ApprovedClientConfig.Builder outClientConfig = null;
        if (inClientConfig != null) {
            outClientConfig = ClientMaster.ApprovedClientConfig.newBuilder();
            ClusterSettingEntity settingEntity =
                    this.defMetaDataService.getClusterDefSetting(false);
            if (settingEntity == null) {
                outClientConfig.setConfigId(TBaseConstants.META_VALUE_UNDEFINED);
            } else {
                outClientConfig.setConfigId(settingEntity.getSerialId());
                if (settingEntity.getSerialId() != inClientConfig.getConfigId()) {
                    outClientConfig.setMaxMsgSize(settingEntity.getMaxMsgSizeInB());
                }
            }
        }
        return outClientConfig;
    }

    private ClientMaster.OpsTaskInfo.Builder buildOpsTaskInfo(ConsumeGroupInfo groupInfo,
                                                              ConsumerInfo nodeInfo,
                                                              OpsSyncInfo opsTaskInfo) {
        ClientMaster.OpsTaskInfo.Builder builder = ClientMaster.OpsTaskInfo.newBuilder();
        long csmFromMaxCtrlId = groupInfo.getCsmFromMaxCtrlId();
        if (csmFromMaxCtrlId != TBaseConstants.META_VALUE_UNDEFINED
                && nodeInfo.getCsmFromMaxOffsetCtrlId() != csmFromMaxCtrlId) {
            builder.setCsmFrmMaxOffsetCtrlId(csmFromMaxCtrlId);
        }
        ClusterSettingEntity defSetting =
                defMetaDataService.getClusterDefSetting(false);
        GroupResCtrlEntity groupResCtrlConf =
                defMetaDataService.getGroupCtrlConf(nodeInfo.getGroupName());
        if (defSetting.enableFlowCtrl()) {
            builder.setDefFlowCheckId(defSetting.getSerialId());
            if (opsTaskInfo.getDefFlowChkId() != defSetting.getSerialId()) {
                builder.setDefFlowControlInfo(defSetting.getGloFlowCtrlRuleInfo());
            }
        }
        if (groupResCtrlConf != null
                && groupResCtrlConf.isFlowCtrlEnable()) {
            builder.setGroupFlowCheckId(groupResCtrlConf.getSerialId());
            builder.setQryPriorityId(groupResCtrlConf.getQryPriorityId());
            if (opsTaskInfo.getGroupFlowChkId() != groupResCtrlConf.getSerialId()) {
                builder.setGroupFlowControlInfo(groupResCtrlConf.getFlowCtrlInfo());
            }
        }
        return builder;
    }

    private void printReportInfo(String consumerId,
                                 Map<String, Map<String, Partition>> curPartSubMa,
                                 Map<String, Map<String, Partition>> newPartSubMap,
                                 StringBuilder sBuffer) {
        boolean printHeader = true;
        if (curPartSubMa == null || curPartSubMa.isEmpty()) {
            if (newPartSubMap != null && !newPartSubMap.isEmpty()) {
                for (Map<String, Partition> newPartMap : newPartSubMap.values()) {
                    if (newPartMap == null || newPartMap.isEmpty()) {
                        continue;
                    }
                    for (Partition info : newPartMap.values()) {
                        printHeader = printReportItemInfo(consumerId,
                                info, true, printHeader, sBuffer);
                    }
                }
            }
        } else {
            if (newPartSubMap == null || newPartSubMap.isEmpty()) {
                for (Map<String, Partition> curPartMap : curPartSubMa.values()) {
                    if (curPartMap == null || curPartMap.isEmpty()) {
                        continue;
                    }
                    for (Partition info : curPartMap.values()) {
                        printHeader = printReportItemInfo(consumerId,
                                info, false, printHeader, sBuffer);
                    }
                }
            } else {
                for (Map.Entry<String, Map<String, Partition>> curEntry
                        : curPartSubMa.entrySet()) {
                    if (curEntry == null || curEntry.getKey() == null) {
                        continue;
                    }
                    Map<String, Partition> newPartMap =
                            newPartSubMap.get(curEntry.getKey());
                    if (newPartMap == null || newPartMap.isEmpty()) {
                        Map<String, Partition> curPartMap = curEntry.getValue();
                        if (curPartMap == null || curPartMap.isEmpty()) {
                            continue;
                        }
                        for (Partition info : curPartMap.values()) {
                            printHeader = printReportItemInfo(consumerId,
                                    info, false, printHeader, sBuffer);
                        }
                    } else {
                        Map<String, Partition> curPartMap = curEntry.getValue();
                        if (curPartMap == null || curPartMap.isEmpty()) {
                            for (Partition info : newPartMap.values()) {
                                printHeader = printReportItemInfo(consumerId,
                                        info, true, printHeader, sBuffer);
                            }
                        } else {
                            for (Map.Entry<String, Partition> curPartEntry
                                    : curPartMap.entrySet()) {
                                if (curPartEntry == null
                                        || curPartEntry.getKey() == null
                                        || curPartEntry.getValue() == null) {
                                    continue;
                                }
                                if (newPartMap.get(curPartEntry.getKey()) == null) {
                                    printHeader = printReportItemInfo(consumerId,
                                            curPartEntry.getValue(), false, printHeader, sBuffer);
                                }
                            }
                            for (Map.Entry<String, Partition> newPartEntry
                                    : newPartMap.entrySet()) {
                                if (newPartEntry == null
                                        || newPartEntry.getKey() == null
                                        || newPartEntry.getValue() == null) {
                                    continue;
                                }
                                if (curPartMap.get(newPartEntry.getKey()) == null) {
                                    printHeader = printReportItemInfo(consumerId,
                                            newPartEntry.getValue(), true, printHeader, sBuffer);
                                }
                            }
                        }
                    }
                }
                for (Map.Entry<String, Map<String, Partition>> newEntry
                        : newPartSubMap.entrySet()) {
                    if (newEntry == null || newEntry.getKey() == null) {
                        continue;
                    }
                    if (curPartSubMa.get(newEntry.getKey()) != null) {
                        continue;
                    }
                    Map<String, Partition> newPartMap = newEntry.getValue();
                    if (newPartMap == null || newPartMap.isEmpty()) {
                        continue;
                    }
                    for (Partition info : newPartMap.values()) {
                        printHeader = printReportItemInfo(consumerId,
                                info, true, printHeader, sBuffer);
                    }
                }
            }
        }
        if (sBuffer.length() > 0) {
            logger.info(sBuffer.append("]").toString());
        }
        sBuffer.delete(0, sBuffer.length());

    }

    private boolean printReportItemInfo(String consumerId, Partition info,
                                        boolean isAdd, boolean printHeader,
                                        StringBuilder sBuffer) {
        if (info == null) {
            return printHeader;
        }
        String type = isAdd ? "[added]" : "[deleted]";
        if (printHeader) {
            sBuffer.append("[SubInfo Report] client ")
                    .append(consumerId).append(" report the info: [");
            printHeader = false;
        }
        sBuffer.append(type).append(info.toString()).append(", ");
        return printHeader;
    }

    /**
     * build cluster configure info
     *
     * @param inClusterConfig broker reported Configure info
     * @return ClusterConfig
     */
    private ClientMaster.ClusterConfig.Builder buildClusterConfig(
            ClientMaster.ClusterConfig inClusterConfig) {
        ClientMaster.ClusterConfig.Builder outClsConfig = null;
        if (inClusterConfig != null) {
            outClsConfig = ClientMaster.ClusterConfig.newBuilder();
            ClusterSettingEntity settingEntity =
                    this.defMetaDataService.getClusterDefSetting(false);
            if (settingEntity == null) {
                outClsConfig.setConfigId(TBaseConstants.META_VALUE_UNDEFINED);
            } else {
                outClsConfig.setConfigId(settingEntity.getSerialId());
                if (settingEntity.getSerialId() != inClusterConfig.getConfigId()) {
                    outClsConfig.setMaxMsgSize(settingEntity.getMaxMsgSizeInB());
                }
            }
        }
        return outClsConfig;
    }

    /**
     * Start balance chore
     *
     * @param master
     * @return
     */
    private Thread startBalancerChore(final TMaster master) {
        Chore chore = new Chore("BalancerChore",
                masterConfig.getConsumerBalancePeriodMs(), master) {
            @Override
            protected void chore() {
                try {
                    master.balance(master);
                } catch (Throwable e) {
                    logger.warn("Rebalance throwable error: ", e);
                }
            }
        };
        return ThreadUtils.setDaemonThreadRunning(chore.getThread());
    }

    public void stop() {
        stop("");
    }

    /**
     * stop master service
     *
     * @param why stop reason
     */
    @Override
    public void stop(String why) {
        logger.info(why);
        this.stopped = true;
        try {
            webServer.stop();
            rpcServiceFactory.destroy();
            svrExecutor.shutdown();
            cltExecutor.shutdown();
            stopChores();
            heartbeatManager.stop();
            defMetaDataService.stop();
            visitTokenManager.stop();
            if (!shutdownHooked.get()) {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            }
        } catch (Exception e) {
            logger.error("Stop master error!", e);
        }
    }

    @Override
    public boolean isStopped() {
        return this.stopped;
    }

    /**
     * Getter
     *
     * @return
     */
    public ConcurrentHashMap<String, Map<String, Map<String, Partition>>> getCurrentSubInfoMap() {
        return currentSubInfo;
    }

    public TopicPSInfoManager getTopicPSInfoManager() {
        return topicPSInfoManager;
    }

    public BrokerAbnHolder getBrokerAbnHolder() {
        return brokerRunManager.getBrokerAbnHolder();
    }

    public ProducerInfoHolder getProducerHolder() {
        return producerHolder;
    }

    public ConsumerInfoHolder getConsumerHolder() {
        return consumerHolder;
    }

    /**
     * check bdb meta-data path, create it if not exist
     *
     * @throws Exception
     */
    private void chkAndCreateBdbMetaDataPath() throws Exception {
        String bdbEnvPath = this.masterConfig.getMetaDataPath();
        final File dir = new File(bdbEnvPath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new Exception(new StringBuilder(256)
                    .append("Could not make bdb data directory ")
                    .append(dir.getAbsolutePath()).toString());
        }
        if (!dir.isDirectory() || !dir.canRead()) {
            throw new Exception(new StringBuilder(256)
                    .append("bdb data path ")
                    .append(dir.getAbsolutePath())
                    .append(" is not a readable directory").toString());
        }
    }

    private void checkNodeStatus(String clientId, final StringBuilder strBuffer) throws Exception {
        if (!defMetaDataService.isSelfMaster()) {
            throw new StandbyException(strBuffer.append(masterAddInfo.getHostPortStr())
                    .append(" is not master now. the connecting client id is ")
                    .append(clientId).toString());
        }

    }

    private abstract static class AbstractReleaseRunner {
        abstract void run(String arg, boolean isTimeout);
    }

    private class ReleaseConsumer extends AbstractReleaseRunner {
        @Override
        void run(String arg, boolean isTimeout) {
            String[] nodeStrs = arg.split(TokenConstants.GROUP_SEP);
            String consumerId = nodeStrs[0];
            String group = nodeStrs[1];
            Integer lid = null;
            try {
                lid = masterRowLock.getLock(null,
                        StringUtils.getBytesUtf8(consumerId), true);
                ConsumerInfo info = consumerHolder.removeConsumer(group, consumerId, isTimeout);
                currentSubInfo.remove(consumerId);
                consumerEventManager.removeAll(consumerId);
                if (info != null) {
                    if (consumerHolder.isConsumeGroupEmpty(group)) {
                        topicPSInfoManager.rmvGroupSubTopicInfo(group, info.getTopicSet());
                    }
                }
            } catch (IOException e) {
                logger.warn("Failed to lock.", e);
            } finally {
                if (lid != null) {
                    masterRowLock.releaseRowLock(lid);
                }
            }
        }
    }

    private class ReleaseProducer extends AbstractReleaseRunner {
        @Override
        void run(String clientId, boolean isTimeout) {
            if (clientId != null) {
                ProducerInfo info = producerHolder.removeProducer(clientId, isTimeout);
                if (info != null) {
                    topicPSInfoManager.rmvProducerTopicPubInfo(clientId, info.getTopicSet());
                }
            }
        }
    }

    private final class ShutdownHook extends Thread {
        @Override
        public void run() {
            if (shutdownHooked.compareAndSet(false, true)) {
                TMaster.this.stop("TMaster shutdown hooked.");
            }
        }
    }

}
