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

package org.apache.inlong.dataproxy.source;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.group.ChannelGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import org.apache.commons.io.IOUtils;
import org.apache.flume.Context;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.conf.Configurable;
import org.apache.inlong.common.metric.MetricRegister;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.metrics.DataProxyMetricItemSet;
import org.apache.inlong.dataproxy.utils.EventLoopUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple tcp source
 *
 */
public class SimpleTcpSource extends BaseSource
        implements Configurable, EventDrivenSource {

    private static final Logger logger = LoggerFactory.getLogger(SimpleTcpSource.class);

    public static ArrayList<String> blacklist = new ArrayList<String>();

    private static final String blacklistFilePath = "blacklist.properties";

    private static int TRAFFIC_CLASS_TYPE_0 = 0;

    private static int TRAFFIC_CLASS_TYPE_96 = 96;

    private static int HIGH_WATER_MARK_DEFAULT_VALUE = 64 * 1024;

    private static int DEFAULT_SLEEP_TIME_MS = 5 * 1000;

    private static long propsLastModified;

    private CheckBlackListThread checkBlackListThread;

    private boolean tcpNoDelay = true;

    private boolean keepAlive = true;

    private int highWaterMark;

    private int trafficClass;

    protected String topic;

    private ServerBootstrap bootstrap;

    private DataProxyMetricItemSet metricItemSet;

    public SimpleTcpSource() {
        super();

    }

    /**
     * check black list
     * @param blacklist
     * @param allChannels
     */
    public static void checkBlackList(ArrayList blacklist, ChannelGroup allChannels) {
        if (blacklist != null) {
            Iterator<Channel> it = allChannels.iterator();
            while (it.hasNext()) {
                Channel channel = it.next();
                String strRemoteIP = null;
                SocketAddress remoteSocketAddress = channel.remoteAddress();
                if (null != remoteSocketAddress) {
                    strRemoteIP = remoteSocketAddress.toString();
                    try {
                        strRemoteIP = strRemoteIP.substring(1, strRemoteIP.indexOf(':'));
                    } catch (Exception ee) {
                        logger.warn("fail to get the remote IP, and strIP={},remoteSocketAddress={}", strRemoteIP,
                                remoteSocketAddress);
                    }
                }
                if (strRemoteIP != null && blacklist.contains(strRemoteIP)) {
                    logger.error(strRemoteIP + " is in blacklist, so disconnect it !");
                    channel.disconnect();
                    channel.close();
                    allChannels.remove(channel);
                }
            }
        }
    }

    private ArrayList<String> load(String fileName) {
        ArrayList<String> arrayList = new ArrayList<String>();
        if (fileName == null) {
            logger.error("fail to loadProperties, filename is null");
            return arrayList;
        }
        FileReader reader = null;
        BufferedReader br = null;
        try {
            reader = new FileReader("conf/" + fileName);
            br = new BufferedReader(reader);
            String line = null;
            while ((line = br.readLine()) != null) {
                arrayList.add(line);
            }
        } catch (UnsupportedEncodingException e) {
            logger.error("fail to loadPropery, file ={}, and e= {}", fileName, e);
        } catch (Exception e) {
            logger.error("fail to loadProperty, file ={}, and e= {}", fileName, e);
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(br);
        }
        return arrayList;
    }

    private class CheckBlackListThread extends Thread {
        private boolean shutdown = false;

        public void shutdouwn() {
            shutdown = true;
        }

        @Override
        public void run() {
            logger.info("CheckBlackListThread thread {} start.", Thread.currentThread().getName());
            while (!shutdown) {
                try {
                    File blacklistFile = new File("conf/" + blacklistFilePath);
                    if (blacklistFile.lastModified() > propsLastModified) {
                        blacklist = load(blacklistFilePath);
                        propsLastModified = blacklistFile.lastModified();
                        SimpleDateFormat formator = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        logger.info("blacklist.properties:{}\n{}",
                                formator.format(new Date(blacklistFile.lastModified())), blacklist);
                    }
                    Thread.sleep(DEFAULT_SLEEP_TIME_MS);
                    checkBlackList(blacklist, allChannels);
                } catch (InterruptedException e) {
                    logger.info("ConfigReloader thread exit!");
                    return;
                } catch (Throwable t) {
                    logger.error("ConfigReloader thread error!", t);
                }
            }
        }
    }

    @Override
    public synchronized void startSource() {
        logger.info("start " + this.getName());
        this.metricItemSet = new DataProxyMetricItemSet(this.getName());
        MetricRegister.register(metricItemSet);
        checkBlackListThread = new CheckBlackListThread();
        checkBlackListThread.start();
//        ThreadRenamingRunnable.setThreadNameDeterminer(ThreadNameDeterminer.CURRENT);

        logger.info("Set max workers : {} ;", maxThreads);

        acceptorThreadFactory = new DefaultThreadFactory("tcpSource-nettyBoss-threadGroup");

        this.acceptorGroup = EventLoopUtil.newEventLoopGroup(
                acceptorThreads, false, acceptorThreadFactory);

        this.workerGroup = EventLoopUtil
                .newEventLoopGroup(maxThreads, enableBusyWait,
                        new DefaultThreadFactory("tcpSource-nettyWorker-threadGroup"));

        bootstrap = new ServerBootstrap();

        bootstrap.childOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);
        bootstrap.childOption(ChannelOption.TCP_NODELAY, tcpNoDelay);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, keepAlive);
        bootstrap.childOption(ChannelOption.SO_RCVBUF, receiveBufferSize);
        bootstrap.childOption(ChannelOption.SO_SNDBUF, sendBufferSize);
//        serverBootstrap.childOption("child.trafficClass", trafficClass);
        bootstrap.childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, highWaterMark);
        bootstrap.channel(EventLoopUtil.getServerSocketChannelClass(workerGroup));
        EventLoopUtil.enableTriggeredMode(bootstrap);
        bootstrap.group(acceptorGroup, workerGroup);
        logger.info("load msgFactory=" + msgFactoryName
                + " and serviceDecoderName=" + serviceDecoderName);

        ChannelInitializer fac = this.getChannelInitializerFactory();
        bootstrap.childHandler(fac);
        try {
            if (host == null) {
                channelFuture = bootstrap.bind(new InetSocketAddress(port)).sync();
            } else {
                channelFuture = bootstrap.bind(new InetSocketAddress(host, port)).sync();
            }
        } catch (Exception e) {
            logger.error("Simple TCP Source error bind host {} port {},program will exit! e = {}",
                    host, port, e);
            System.exit(-1);
        }
        logger.info("Simple TCP Source started at host {}, port {}", host, port);
    }

    @Override
    public synchronized void stop() {
        checkBlackListThread.shutdouwn();
        super.stop();
    }

    @Override
    public void configure(Context context) {
        logger.info("context is {}", context);
        super.configure(context);
        tcpNoDelay = context.getBoolean(ConfigConstants.TCP_NO_DELAY, true);
        keepAlive = context.getBoolean(ConfigConstants.KEEP_ALIVE, true);
        highWaterMark = context.getInteger(ConfigConstants.HIGH_WATER_MARK, HIGH_WATER_MARK_DEFAULT_VALUE);
        receiveBufferSize = context.getInteger(ConfigConstants.RECEIVE_BUFFER_SIZE, RECEIVE_BUFFER_DEFAULT_SIZE);
        if (receiveBufferSize > RECEIVE_BUFFER_MAX_SIZE) {
            receiveBufferSize = RECEIVE_BUFFER_MAX_SIZE;
        }
        Preconditions.checkArgument(receiveBufferSize > BUFFER_SIZE_MUST_THAN,
                "receiveBufferSize must be > 0");

        sendBufferSize = context.getInteger(ConfigConstants.SEND_BUFFER_SIZE, SEND_BUFFER_DEFAULT_SIZE);
        if (sendBufferSize > SEND_BUFFER_MAX_SIZE) {
            sendBufferSize = SEND_BUFFER_MAX_SIZE;
        }
        Preconditions.checkArgument(sendBufferSize > BUFFER_SIZE_MUST_THAN,
                "sendBufferSize must be > 0");

        trafficClass = context.getInteger(ConfigConstants.TRAFFIC_CLASS, TRAFFIC_CLASS_TYPE_0);
        Preconditions.checkArgument((trafficClass == TRAFFIC_CLASS_TYPE_0
                        || trafficClass == TRAFFIC_CLASS_TYPE_96),
                "trafficClass must be == 0 or == 96");

        try {
            maxThreads = context.getInteger(ConfigConstants.MAX_THREADS, 32);
        } catch (NumberFormatException e) {
            logger.warn("Simple TCP Source max-threads property must specify an integer value. {}",
                    context.getString(ConfigConstants.MAX_THREADS));
        }
    }

    /**
     * get metricItemSet
     * @return the metricItemSet
     */
    public DataProxyMetricItemSet getMetricItemSet() {
        return metricItemSet;
    }

    @Override
    public String getProtocolName() {
        return "tcp";
    }
}
