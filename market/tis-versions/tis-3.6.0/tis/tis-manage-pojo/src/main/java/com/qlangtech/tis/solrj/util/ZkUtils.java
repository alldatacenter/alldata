/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qlangtech.tis.solrj.util;

import com.qlangtech.tis.cloud.ITISCoordinator;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.realtime.utils.NetUtils;
import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年1月17日
 */
public class ZkUtils {

    public static final String ZK_ASSEMBLE_LOG_COLLECT_PATH = "/tis/incr-transfer-group/incr-state-collect";
    public static final int ZK_ASSEMBLE_LOG_COLLECT_PORT = 56432;
    public static final String ZK_PATH_OVERSEER_ELECT_LEADER = "/overseer_elect/leader";

    private static final Logger logger = LoggerFactory.getLogger(ZkUtils.class);

    public static final String PATH_SPLIT = "/";


    public static String getFirstChildValue(final ITISCoordinator coordinator, final String zkPath) {
        return getFirstChildValue(coordinator, zkPath, true);
    }

    public static String getFirstChildValue(final ITISCoordinator coordinator, final String zkPath, boolean onReconnect) {
        if (coordinator == null) {
            throw new IllegalArgumentException("param coordinator can not be null");
        }
        List<String> children = coordinator.getChildren(zkPath, true);
        if (children == null) {
            throw new IllegalStateException("zkPath:" + zkPath + " relevant children can not be null");
        }
        try {
            if (onReconnect) {
                coordinator.addOnReconnect(() -> {
                    getFirstChildValue(coordinator, zkPath, false);
                });
            }
            for (String c : children) {
                return new String(coordinator.getData(zkPath + PATH_SPLIT + c, true), TisUTF8.get());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw new IllegalStateException("zkpath:" + zkPath + " have not find child node");
    }

//    public static String getFirstChildValue(final TisZkClient zookeeper, final String zkPath, final Watcher watcher, boolean onReconnect) {
//        try {
//            List<String> children = zookeeper.getChildren(zkPath, watcher, true);
//            if (onReconnect && watcher != null) {
//                zookeeper.addOnReconnect(() -> {
//                    getFirstChildValue(zookeeper, zkPath, watcher, false);
//                });
//            }
//            for (String c : children) {
//                return new String(zookeeper.getData(zkPath + PATH_SPLIT + c, null, new Stat(), true), "utf8");
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        throw new IllegalStateException("zkpath:" + zkPath + " have not find child node");
//    }

    /**
     * 将本地ip地址(端口)以临时节点的方式注册的ZK上
     *
     * @param zookeeper
     * @param zkPath
     * @param port
     * @throws
     * @throws InterruptedException
     */
    public static String registerAddress2ZK(final ITISCoordinator zookeeper, final String zkPath, final int port) throws  InterruptedException {

        String ip = NetUtils.getHost();
        registerMyIp(zkPath, ip, port, zookeeper);
        zookeeper.addOnReconnect(() -> {
            registerMyIp(zkPath, ip, port, zookeeper);
        });
        return ip;
    }

    /**
     * 注册内容到临时节点上
     *
     * @param zookeeper
     * @param zkPath    临时节点路径例如：/tis/incr-transfer-group/search4totalpay/consume-0000001
     *                  临时节点的内容为json内容如下：
     * @param content   在Yarn集群启动的一个分区节点会自动将本节点的信息注册到Zookeeper节点上
     *                  临时节点的内容为json内容如下：id分区消费节点的id值，该值应该是这个节点启动时自动生成的一个32位的GUID值（这个id值伴随该节点的整个生命周期）
     *                  group：消费节点所在消费组，通常一个组内可以放置多个索引消费节点 host:消费节点所在的节点地址
     * @throws
     * @throws InterruptedException
     */
    public static void registerTemporaryContent(final ITISCoordinator zookeeper, final String zkPath, final String content) throws  InterruptedException {
        registerContent(zkPath, content, zookeeper);
        zookeeper.addOnReconnect(() -> {
            registerContent(zkPath, content, zookeeper);
        });
    }

    private static void registerContent(final String zkpath, String content, ITISCoordinator zookeeper) {
        try {
            String[] pathname = StringUtils.split(zkpath, PATH_SPLIT);
            if (pathname.length > 1) {
                StringBuffer path = new StringBuffer();
                guaranteeExist(//
                        zookeeper, path, Arrays.copyOfRange(pathname, 0, pathname.length - 1), 0, StringUtils.EMPTY.getBytes());
            }
            // CreateMode.EPHEMERAL_SEQUENTIAL
            zookeeper.create(zkpath, content.getBytes(TisUTF8.get()), false, true);
        } catch (Exception e) {
            logger.error(e.getMessage() + "\n zkpath:" + zkpath, e);
            //  throw new ZooKeeperException(SolrException.ErrorCode.SERVER_ERROR, e.getMessage() + "\n zkpath:" + zkpath, e);
            throw new RuntimeException(e.getMessage() + "\n zkpath:" + zkpath, e);
        }
    }

    /**
     * @param zookeeper
     * @throws
     * @throws InterruptedException
     */
    private static String registerMyIp(final String parentNodepath, String ip, int port, ITISCoordinator zookeeper) {
        try {
            if ("127.0.0.1".equals(ip)) {
                throw new IllegalStateException("ip can not be 127.0.0.1");
            }
            if (port > 0) {
                ip = ip + ":" + port;
            }
            registerContent(parentNodepath + "/nodes", ip, zookeeper);
            return ip;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 确保节点存在
     *
     * @param zookeeper
     * @param parentNodepath
     */
    public static void guaranteeExist(ITISCoordinator zookeeper, String parentNodepath, byte[] data) throws Exception {
        String[] pathname = StringUtils.split(parentNodepath, PATH_SPLIT);
        StringBuffer path = new StringBuffer();
        guaranteeExist(zookeeper, path, pathname, 0, data);
    }

    public static void guaranteeExist(ITISCoordinator zookeeper, String parentNodepath) throws Exception {

        guaranteeExist(zookeeper, parentNodepath, StringUtils.EMPTY.getBytes());
    }

    private static void guaranteeExist(ITISCoordinator zookeeper, StringBuffer path, String[] paths, int deepth, byte[] data) throws Exception {
        if (deepth >= paths.length) {
            return;
        }
        path.append(PATH_SPLIT).append(paths[deepth]);
        if (!zookeeper.exists(path.toString(), false)) {
            // zookeeper.create(path.toString(), data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            zookeeper.create(path.toString(), data, true, false);
        }
        guaranteeExist(zookeeper, path, paths, ++deepth, data);
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        System.out.println("start");
        String[] pathname = new String[]{"a", "b"};
        pathname = Arrays.copyOfRange(pathname, 0, pathname.length - 1);
        for (String n : pathname) {
            System.out.println(n);
        }
    }

//    protected static void processNode(ITISCoordinator fromZk, ITISCoordinator toZk, String zkpath) throws Exception {
//        List<String> child = fromZk.getChildren(zkpath, true);
//
//        byte[] content = null;
//        String childPath = null;
//        // 将节点拷贝
//        guaranteeExist(fromZk, zkpath);
//        for (String c : child) {
//            if (StringUtils.endsWith(zkpath, PATH_SPLIT)) {
//                childPath = zkpath + c;
//            } else {
//                childPath = zkpath + PATH_SPLIT + c;
//            }
//            content = fromZk.getData(childPath, true);
//            // 持久节点
////            if (state.getEphemeralOwner() < 1) {
////                try {
////                    // toZk.create(childPath, content, CreateMode.PERSISTENT, true);
////                    toZk.create(childPath, content, true, false);
////                    // System.out.println("create node:" + childPath);
////                } catch (Exception e) {
////                    throw new RuntimeException("childPath create error:" + childPath, e);
////                }
////                if (state.getNumChildren() > 0) {
////                    processNode(fromZk, toZk, childPath);
////                }
////            }
//        }
//    }
}
