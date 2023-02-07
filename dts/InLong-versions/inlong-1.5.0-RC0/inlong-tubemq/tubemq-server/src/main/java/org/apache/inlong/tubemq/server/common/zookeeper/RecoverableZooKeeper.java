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

package org.apache.inlong.tubemq.server.common.zookeeper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.inlong.tubemq.server.common.utils.Bytes;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A zookeeper that can handle 'recoverable' errors. To handle recoverable errors, developers need
 * to realize that there are two classes of requests: idempotent and non-idempotent requests. Read
 * requests and unconditional sets and deletes are examples of idempotent requests, they can be
 * reissued with the same results. (Although, the delete may throw a NoNodeException on reissue its
 * effect on the ZooKeeper state is the same.) Non-idempotent requests need special handling,
 * application and library writers need to keep in mind that they may need to encode information in
 * the data or name of znodes to detect retries. A simple example is a create that uses a sequence
 * flag. If a process issues a create("/x-", ..., SEQUENCE) and gets a connection loss exception,
 * that process will reissue another create("/x-", ..., SEQUENCE) and get back x-111. When the
 * process does a getClientLst("/"), it sees x-1,x-30,x-109,x-110,x-111, now it could be that x-109
 * was the result of the previous create, so the process actually owns both x-109 and x-111. An easy
 * way around this is to use "x-process id-" when doing the create. If the process is using an id of
 * 352, before reissuing the create it will do a getClientLst("/") and see "x-222-1", "x-542-30",
 * "x-352-109", x-333-110". The process will know that the original create succeeded an the znode it
 * created is "x-352-109".
 *
 * See "http://wiki.apache.org/hadoop/ZooKeeper/ErrorHandling"
 *
 * Copied from <a href="http://hbase.apache.org">Apache HBase Project</a>
 */
public class RecoverableZooKeeper {

    private static final Logger logger =
            LoggerFactory.getLogger(RecoverableZooKeeper.class);
    private final RetryCounterFactory retryCounterFactory;
    // the actual ZooKeeper client instance
    private ZooKeeper zk;
    // An identifier of this process in the cluster
    private Watcher watcher;
    private int sessionTimeout;
    private String quorumServers;

    public RecoverableZooKeeper(String quorumServers, int sessionTimeout, Watcher watcher,
            int maxRetries, int retryIntervalMillis) throws IOException {
        this.zk = new ZooKeeper(quorumServers, sessionTimeout, watcher);
        this.retryCounterFactory = new RetryCounterFactory(maxRetries, retryIntervalMillis);
        this.watcher = watcher;
        this.sessionTimeout = sessionTimeout;
        this.quorumServers = quorumServers;
    }

    /**
     * Filters the given node list by the given prefixes. This method is all-inclusive--if any
     * element in the node list starts with any of the given prefixes, then it is included in the
     * result.
     *
     * @param nodes    the nodes to filter
     * @param prefixes the prefixes to include in the result
     * @return list of every element that starts with one of the prefixes
     */
    private static synchronized List<String> filterByPrefix(List<String> nodes, String... prefixes) {
        List<String> lockChildren = new ArrayList<>();
        for (String child : nodes) {
            for (String prefix : prefixes) {
                if (child.startsWith(prefix)) {
                    lockChildren.add(child);
                    break;
                }
            }
        }
        return lockChildren;
    }

    public synchronized void reconnectAfterExpiration() throws IOException, InterruptedException {

        if (zk != null) {
            States state = zk.getState();
            if (state != null && state.isConnected() && state.isAlive()) {
                return;
            }

            logger.info("[ZK_SESSION_EXPIRATION] Closing dead ZooKeeper connection, session" + " was: 0x"
                    + Long.toHexString(zk.getSessionId()));
            zk.close();
        }

        this.zk = new ZooKeeper(this.quorumServers, this.sessionTimeout, this.watcher);
        logger.info("[ZK_SESSION_EXPIRATION] Recreated a ZooKeeper, session" + " is: 0x"
                + Long.toHexString(zk.getSessionId()));
    }

    public synchronized void ensureConnectivity(KeeperException e) {
        if (e != null && e.code() != KeeperException.Code.SESSIONEXPIRED) {
            return;
        }

        try {
            reconnectAfterExpiration();
        } catch (Throwable e1) {
            logger.error("[ZK_SESSION_EXPIRATION] reconnectAfterExpiration failed.", e1);
        }
    }

    /**
     * delete is an idempotent operation. Retry before throwing exception. This function will not
     * throw NoNodeException if the path does not exist.
     */
    public synchronized void delete(String path, int version) throws InterruptedException,
            KeeperException {
        RetryCounter retryCounter = retryCounterFactory.create();
        boolean isRetry = false; // False for first attempt, true for all
        // retries.
        while (true) {
            try {
                zk.delete(path, version);
                return;
            } catch (KeeperException e) {
                ensureConnectivity(e);

                switch (e.code()) {
                    case NONODE:
                        if (isRetry) {
                            logger.info("Node " + path + " already deleted. Assuming that a "
                                    + "previous attempt succeeded.");
                            return;
                        }
                        logger.warn("Node " + path + " already deleted, and this is not a " + "retry");
                        throw e;

                    case CONNECTIONLOSS:
                    case SESSIONEXPIRED:
                    case OPERATIONTIMEOUT:
                        retryOrThrow(retryCounter, e, "delete");
                        break;

                    default:
                        throw e;
                }
            }
            retryCounter.sleepUntilNextRetry();
            retryCounter.useRetry();
            isRetry = true;
        }
    }

    /**
     * exists is an idempotent operation. Retry before throwing exception
     *
     * @return A Stat instance
     */
    public synchronized Stat exists(String path, Watcher watcher) throws KeeperException,
            InterruptedException {
        RetryCounter retryCounter = retryCounterFactory.create();
        while (true) {
            try {
                return zk.exists(path, watcher);
            } catch (KeeperException e) {
                ensureConnectivity(e);

                switch (e.code()) {
                    case CONNECTIONLOSS:
                    case SESSIONEXPIRED:
                    case OPERATIONTIMEOUT:
                        retryOrThrow(retryCounter, e, "exists");
                        break;

                    default:
                        throw e;
                }
            }
            retryCounter.sleepUntilNextRetry();
            retryCounter.useRetry();
        }
    }

    /**
     * exists is an idempotent operation. Retry before throwing exception
     *
     * @return A Stat instance
     */
    public synchronized Stat exists(String path, boolean watch) throws KeeperException,
            InterruptedException {
        RetryCounter retryCounter = retryCounterFactory.create();
        while (true) {
            try {
                return zk.exists(path, watch);
            } catch (KeeperException e) {
                ensureConnectivity(e);

                switch (e.code()) {
                    case CONNECTIONLOSS:
                    case SESSIONEXPIRED:
                    case OPERATIONTIMEOUT:
                        retryOrThrow(retryCounter, e, "exists");
                        break;

                    default:
                        throw e;
                }
            }
            retryCounter.sleepUntilNextRetry();
            retryCounter.useRetry();
        }
    }

    private synchronized void retryOrThrow(RetryCounter retryCounter, KeeperException e, String opName)
            throws KeeperException {
        logger.warn("Possibly transient ZooKeeper exception: ", e);
        if (!retryCounter.shouldRetry()) {
            logger.error("ZooKeeper " + opName + " failed after " + retryCounter.getMaxRetries()
                    + " retries");
            throw e;
        }
    }

    /**
     * getClientLst is an idempotent operation. Retry before throwing exception
     *
     * @return List of children znodes
     */
    public synchronized List<String> getChildren(String path, Watcher watcher)
            throws KeeperException, InterruptedException {
        RetryCounter retryCounter = retryCounterFactory.create();
        while (true) {
            try {
                return zk.getChildren(path, watcher);
            } catch (KeeperException e) {
                ensureConnectivity(e);

                switch (e.code()) {
                    case CONNECTIONLOSS:
                    case SESSIONEXPIRED:
                    case OPERATIONTIMEOUT:
                        retryOrThrow(retryCounter, e, "getClientLst");
                        break;

                    default:
                        throw e;
                }
            }
            retryCounter.sleepUntilNextRetry();
            retryCounter.useRetry();
        }
    }

    /**
     * getClientLst is an idempotent operation. Retry before throwing exception
     *
     * @return List of children znodes
     */
    public synchronized List<String> getChildren(String path, boolean watch) throws KeeperException,
            InterruptedException {
        RetryCounter retryCounter = retryCounterFactory.create();
        while (true) {
            try {
                return zk.getChildren(path, watch);
            } catch (KeeperException e) {
                ensureConnectivity(e);

                switch (e.code()) {
                    case CONNECTIONLOSS:
                    case SESSIONEXPIRED:
                    case OPERATIONTIMEOUT:
                        retryOrThrow(retryCounter, e, "getClientLst");
                        break;

                    default:
                        throw e;
                }
            }
            retryCounter.sleepUntilNextRetry();
            retryCounter.useRetry();
        }
    }

    /**
     * getData is an idempotent operation. Retry before throwing exception
     *
     * @return Data
     */
    public synchronized byte[] getData(String path, Watcher watcher, Stat stat)
            throws KeeperException, InterruptedException {
        RetryCounter retryCounter = retryCounterFactory.create();
        while (true) {
            try {
                byte[] revData = zk.getData(path, watcher, stat);
                return revData;
            } catch (KeeperException e) {
                ensureConnectivity(e);

                switch (e.code()) {
                    case CONNECTIONLOSS:
                    case SESSIONEXPIRED:
                    case OPERATIONTIMEOUT:
                        retryOrThrow(retryCounter, e, "getData");
                        break;

                    default:
                        throw e;
                }
            }
            retryCounter.sleepUntilNextRetry();
            retryCounter.useRetry();
        }
    }

    /**
     * getData is an idempotent operation. Retry before throwing exception
     *
     * @return Data
     */
    public synchronized byte[] getData(String path, boolean watch, Stat stat) throws KeeperException,
            InterruptedException {
        RetryCounter retryCounter = retryCounterFactory.create();
        while (true) {
            try {
                byte[] revData = zk.getData(path, watch, stat);
                return revData;
            } catch (KeeperException e) {
                ensureConnectivity(e);

                switch (e.code()) {
                    case CONNECTIONLOSS:
                    case SESSIONEXPIRED:
                    case OPERATIONTIMEOUT:
                        retryOrThrow(retryCounter, e, "getData");
                        break;

                    default:
                        throw e;
                }
            }
            retryCounter.sleepUntilNextRetry();
            retryCounter.useRetry();
        }
    }

    /**
     * setData is NOT an idempotent operation. Retry may cause BadVersion Exception Adding an
     * identifier field into the data to check whether badversion is caused by the result of
     * previous correctly setData
     *
     * @return Stat instance
     */
    public synchronized Stat setData(String path, byte[] data, int version) throws KeeperException,
            InterruptedException {
        RetryCounter retryCounter = retryCounterFactory.create();
        byte[] newData = data;
        while (true) {
            try {
                return zk.setData(path, newData, version);
            } catch (KeeperException e) {
                ensureConnectivity(e);

                switch (e.code()) {
                    case CONNECTIONLOSS:
                    case SESSIONEXPIRED:
                    case OPERATIONTIMEOUT:
                        retryOrThrow(retryCounter, e, "setData");
                        break;
                    case BADVERSION:
                        // try to verify whether the previous setData success or not
                        // try {
                        // Stat stat = new Stat();
                        // byte[] revData = zk.getData(path, false, stat);
                        // int idLength = Bytes.toInt(revData, ID_LENGTH_SIZE);
                        // int dataLength = revData.length - ID_LENGTH_SIZE -
                        // idLength;
                        // int dataOffset = ID_LENGTH_SIZE + idLength;
                        //
                        // if (Bytes.compareTo(revData, ID_LENGTH_SIZE, id.length,
                        // revData, dataOffset, dataLength) == 0) {
                        // // the bad version is caused by previous successful
                        // // setData
                        // return stat;
                        // }
                        // }
                        // catch (KeeperException keeperException) {
                        // // the ZK is not reliable at this moment. just throwing
                        // // exception
                        // throw keeperException;
                        // }

                        // throw other exceptions and verified bad version
                        // exceptions
                    default:
                        throw e;
                }
            }
            retryCounter.sleepUntilNextRetry();
            retryCounter.useRetry();
        }
    }

    /**
     * NONSEQUENTIAL create is idempotent operation. Retry before throwing exceptions. But this
     * function will not throw the NodeExist exception back to the application. </p> <p> But
     * SEQUENTIAL is NOT idempotent operation. It is necessary to add identifier to the path to
     * verify, whether the previous one is successful or not. </p>
     *
     * @return Path
     */
    public synchronized String create(String path, byte[] data, List<ACL> acl, CreateMode createMode)
            throws KeeperException, InterruptedException {
        byte[] newData = data;
        switch (createMode) {
            case EPHEMERAL:
            case PERSISTENT:
                return createNonSequential(path, newData, acl, createMode);

            case EPHEMERAL_SEQUENTIAL:
            case PERSISTENT_SEQUENTIAL:
                return createSequential(path, newData, acl, createMode);

            default:
                throw new IllegalArgumentException("Unrecognized CreateMode: " + createMode);
        }
    }

    private synchronized String createNonSequential(String path, byte[] data, List<ACL> acl,
            CreateMode createMode)
            throws KeeperException, InterruptedException {
        RetryCounter retryCounter = retryCounterFactory.create();
        boolean isRetry = false; // False for first attempt, true for all
        // retries.
        while (true) {
            try {
                return zk.create(path, data, acl, createMode);
            } catch (KeeperException e) {
                ensureConnectivity(e);

                switch (e.code()) {
                    case NODEEXISTS:
                        if (isRetry) {
                            // If the connection was lost, there is still a
                            // possibility that
                            // we have successfully created the node at our previous
                            // attempt,
                            // so we read the node and compare.
                            byte[] currentData = zk.getData(path, false, null);
                            if (currentData != null && Bytes.compareTo(currentData, data) == 0) {
                                // We successfully created a non-sequential node
                                return path;
                            }
                            logger.error("Node " + path + " already exists with "
                                    + Bytes.toStringBinary(currentData) + ", could not write "
                                    + Bytes.toStringBinary(data));
                            throw e;
                        }
                        // logger.debug("Node " + path +
                        // " already exists and this is not a " + "retry");
                        throw e;

                    case CONNECTIONLOSS:
                    case SESSIONEXPIRED:
                    case OPERATIONTIMEOUT:
                        retryOrThrow(retryCounter, e, "create");
                        break;

                    default:
                        throw e;
                }
            }
            retryCounter.sleepUntilNextRetry();
            retryCounter.useRetry();
            isRetry = true;
        }
    }

    private synchronized String createSequential(String path, byte[] data, List<ACL> acl,
            CreateMode createMode) throws KeeperException, InterruptedException {
        RetryCounter retryCounter = retryCounterFactory.create();
        boolean first = true;
        // String newPath = path + this.identifier;
        String newPath = path;
        while (true) {
            try {
                if (!first) {
                    // Check if we succeeded on a previous attempt
                    String previousResult = findPreviousSequentialNode(newPath);
                    if (previousResult != null) {
                        return previousResult;
                    }
                }
                first = false;
                return zk.create(newPath, data, acl, createMode);
            } catch (KeeperException e) {
                ensureConnectivity(e);

                switch (e.code()) {
                    case CONNECTIONLOSS:
                    case SESSIONEXPIRED:
                    case OPERATIONTIMEOUT:
                        retryOrThrow(retryCounter, e, "create");
                        break;

                    default:
                        throw e;
                }
            }
            retryCounter.sleepUntilNextRetry();
            retryCounter.useRetry();
        }
    }

    private synchronized String findPreviousSequentialNode(String path) throws KeeperException,
            InterruptedException {
        int lastSlashIdx = path.lastIndexOf('/');
        assert (lastSlashIdx != -1);
        String parent = path.substring(0, lastSlashIdx);
        String nodePrefix = path.substring(lastSlashIdx + 1);

        List<String> nodes = zk.getChildren(parent, false);
        List<String> matching = filterByPrefix(nodes, nodePrefix);
        for (String node : matching) {
            String nodePath = parent + "/" + node;
            Stat stat = zk.exists(nodePath, false);
            if (stat != null) {
                return nodePath;
            }
        }
        return null;
    }

    public synchronized long getSessionId() {
        return zk.getSessionId();
    }

    public synchronized void close() throws InterruptedException {
        if (zk != null) {
            zk.close();
        }
    }

    public synchronized States getState() {
        return zk.getState();
    }

    public synchronized ZooKeeper getZooKeeper() {
        return zk;
    }

    public synchronized byte[] getSessionPasswd() {
        return zk.getSessionPasswd();
    }

    public synchronized void sync(String path, AsyncCallback.VoidCallback cb, Object ctx) {
        this.zk.sync(path, null, null);
    }
}
