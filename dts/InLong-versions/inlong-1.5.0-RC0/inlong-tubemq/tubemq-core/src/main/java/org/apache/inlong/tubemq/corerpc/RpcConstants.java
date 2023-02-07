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

package org.apache.inlong.tubemq.corerpc;

import org.apache.inlong.tubemq.corebase.TBaseConstants;

public final class RpcConstants {

    public static final String RPC_CODEC = "rpc.codec";

    public static final String BOSS_COUNT = "rpc.netty.boss.count";
    public static final String WORKER_COUNT = "rpc.netty.worker.count";
    public static final String CALLBACK_WORKER_COUNT = "rpc.netty.callback.count";
    public static final String CLIENT_POOL_POOL_COUNT = "rpc.netty.client.pool.count";
    public static final String CLIENT_CACHE_QUEUE_SIZE = "rpc.netty.client.cache.queue.size";
    public static final String WORKER_THREAD_NAME = "rpc.netty.worker.thread.name";
    public static final String CLIENT_ROLE_TYPE = "rpc.netty.client.role.type";
    public static final String WORKER_MEM_SIZE = "rpc.netty.worker.mem.size";
    public static final String SERVER_POOL_POOL_COUNT = "rpc.netty.server.pool.count";
    public static final String SERVER_CACHE_QUEUE_SIZE = "rpc.netty.server.cache.queue.size";
    public static final String SERVER_ROLE_TYPE = "rpc.netty.server.role.type";

    public static final String CONNECT_TIMEOUT = "rpc.connect.timeout";
    public static final String REQUEST_TIMEOUT = "rpc.request.timeout";
    public static final String READ_TIMEOUT = "rpc.read.timeout";
    public static final String WRITE_TIMEOUT = "rpc.write.timeout";
    public static final String CONNECT_READ_IDLE_DURATION = "rpc.connect.read.idle.duration";
    public static final String NETTY_WRITE_HIGH_MARK = "rpc.netty.write.highmark";
    public static final String NETTY_WRITE_LOW_MARK = "rpc.netty.write.lowmark";
    public static final String NETTY_TCP_SENDBUF = "rpc.netty.send.buffer";
    public static final String NETTY_TCP_RECEIVEBUF = "rpc.netty.receive.buffer";
    public static final String NETTY_TCP_ENABLEBUSYWAIT = "rpc.netty.enable.busy.wait";

    public static final String TCP_NODELAY = "rpc.tcp.nodelay";
    public static final String TCP_REUSEADDRESS = "rpc.tcp.reuseaddress";
    public static final String TCP_KEEP_ALIVE = "rpc.tcp.keeplive";

    public static final String TLS_OVER_TCP = "tcp.tls";
    public static final String TLS_TWO_WAY_AUTHENTIC = "tls.twoway.authentic";
    public static final String TLS_KEYSTORE_PATH = "tls.keystore.path";
    public static final String TLS_KEYSTORE_PASSWORD = "tls.keystore.password";

    public static final String TLS_TRUSTSTORE_PATH = "tls.truststore.path";
    public static final String TLS_TRUSTSTORE_PASSWORD = "tls.truststore.password";

    public static final String RPC_LQ_STATS_DURATION = "rpc.link.quality.stats.duration";
    public static final String RPC_LQ_FORBIDDEN_DURATION = "rpc.link.quality.forbidden.duration";
    public static final String RPC_LQ_MAX_ALLOWED_FAIL_COUNT = "rpc.link.quality.max.allowed.fail.count";
    public static final String RPC_LQ_MAX_FAIL_FORBIDDEN_RATE = "rpc.link.quality.max.fail.forbidden.rate";
    public static final String RPC_SERVICE_UNAVAILABLE_FORBIDDEN_DURATION =
            "rpc.unavailable.service.forbidden.duration";
    public static final int RPC_PROTOCOL_BEGIN_TOKEN = 0xFF7FF4FE;
    public static final int RPC_MAX_BUFFER_SIZE = 8192;
    public static final int MAX_FRAME_MAX_LIST_SIZE =
            (int) ((TBaseConstants.META_MAX_MESSAGE_DATA_SIZE_UPPER_LIMIT
                    + TBaseConstants.META_MB_UNIT_SIZE * 8) / RPC_MAX_BUFFER_SIZE);

    public static final int RPC_FLAG_MSG_TYPE_REQUEST = 0x0;
    public static final int RPC_FLAG_MSG_TYPE_RESPONSE = 0x1;

    public static final int RPC_SERVICE_TYPE_MASTER_SERVICE = 1;
    public static final int RPC_SERVICE_TYPE_BROKER_READ_SERVICE = 2;
    public static final int RPC_SERVICE_TYPE_BROKER_WRITE_SERVICE = 3;
    public static final int RPC_SERVICE_TYPE_BROKER_ADMIN_SERVICE = 4;
    public static final int RPC_SERVICE_TYPE_MASTER_ADMIN_SERVICE = 5;

    public static final int RPC_MSG_MASTER_METHOD_BEGIN = 1;
    public static final int RPC_MSG_MASTER_PRODUCER_REGISTER = 1;
    public static final int RPC_MSG_MASTER_PRODUCER_HEARTBEAT = 2;
    public static final int RPC_MSG_MASTER_PRODUCER_CLOSECLIENT = 3;
    public static final int RPC_MSG_MASTER_CONSUMER_REGISTER = 4;
    public static final int RPC_MSG_MASTER_CONSUMER_HEARTBEAT = 5;
    public static final int RPC_MSG_MASTER_CONSUMER_CLOSECLIENT = 6;
    public static final int RPC_MSG_MASTER_BROKER_REGISTER = 7;
    public static final int RPC_MSG_MASTER_BROKER_HEARTBEAT = 8;
    public static final int RPC_MSG_MASTER_BROKER_CLOSECLIENT = 9;
    public static final int RPC_MSG_MASTER_METHOD_END = 9;

    public static final int RPC_MSG_BROKER_METHOD_BEGIN = 11;
    public static final int RPC_MSG_BROKER_PRODUCER_REGISTER = 11;
    public static final int RPC_MSG_BROKER_PRODUCER_HEARTBEAT = 12;
    public static final int RPC_MSG_BROKER_PRODUCER_SENDMESSAGE = 13;
    public static final int RPC_MSG_BROKER_PRODUCER_CLOSE = 14;
    public static final int RPC_MSG_BROKER_CONSUMER_REGISTER = 15;
    public static final int RPC_MSG_BROKER_CONSUMER_HEARTBEAT = 16;
    public static final int RPC_MSG_BROKER_CONSUMER_GETMESSAGE = 17;
    public static final int RPC_MSG_BROKER_CONSUMER_COMMIT = 18;
    public static final int RPC_MSG_BROKER_CONSUMER_CLOSE = 19;
    // public static final int RPC_MSG_BROKER_METHOD_END = 19;

    public static final int RPC_MSG_MASTER_CONSUMER_REGISTER_V2 = 20;
    public static final int RPC_MSG_MASTER_CONSUMER_HEARTBEAT_V2 = 21;
    public static final int RPC_MSG_MASTER_CONSUMER_GET_PART_META = 22;

    public static final int MSG_OPTYPE_REGISTER = 31;
    public static final int MSG_OPTYPE_UNREGISTER = 32;

    public static final long CFG_RPC_READ_TIMEOUT_DEFAULT_MS = 10000;
    public static final long CFG_RPC_READ_TIMEOUT_MAX_MS = 600000;
    public static final long CFG_RPC_READ_TIMEOUT_MIN_MS = 3000;
    public static final int CFG_CONNECT_READ_IDLE_TIME = 300000;

    public static final int CFG_DEFAULT_BOSS_COUNT = 1;
    public static final int CFG_DEFAULT_RSP_CALLBACK_WORKER_COUNT = 3;
    public static final int CFG_DEFAULT_TOTAL_MEM_SIZE = 10485760;
    public static final int CFG_DEFAULT_CLIENT_WORKER_COUNT =
            Runtime.getRuntime().availableProcessors() + 1;
    public static final int CFG_DEFAULT_SERVER_WORKER_COUNT =
            Runtime.getRuntime().availableProcessors() * 2;
    public static final String CFG_DEFAULT_WORKER_THREAD_NAME =
            "tube_rpc_netty_worker-";

    public static final long CFG_LQ_STATS_DURATION_MS = 60000;
    public static final long CFG_LQ_FORBIDDEN_DURATION_MS = 1800000;
    public static final int CFG_LQ_MAX_ALLOWED_FAIL_COUNT = 5;
    public static final double CFG_LQ_MAX_FAIL_FORBIDDEN_RATE = 0.3;
    public static final long CFG_UNAVAILABLE_FORBIDDEN_DURATION_MS = 50000;
    public static final long CFG_DEFAULT_NETTY_WRITEBUFFER_HIGH_MARK = 50 * 1024 * 1024;
    public static final long CFG_DEFAULT_NETTY_WRITEBUFFER_LOW_MARK = 5 * 1024 * 1024;
}
