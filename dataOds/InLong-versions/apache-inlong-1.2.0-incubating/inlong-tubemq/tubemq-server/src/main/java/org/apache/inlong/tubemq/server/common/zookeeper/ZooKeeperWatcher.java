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

package org.apache.inlong.tubemq.server.common.zookeeper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import org.apache.inlong.tubemq.corebase.utils.ThreadUtils;
import org.apache.inlong.tubemq.server.common.fileconfig.ZKConfig;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Acts as the single ZooKeeper Watcher. One instance of this is instantiated for each Master,
 * RegionServer, and client process.
 * <p/>
 * <p/>
 * This is the only class that implements {@link Watcher}. Other internal classes which need to be
 * notified of ZooKeeper events must register with the local instance of this watcher via {@link
 * #registerListener}.
 * <p/>
 * <p/>
 * This class also holds and manages the connection to ZooKeeper. Code to deal with connection
 * related events and exceptions are handled here.
 *
 * modified version of <a href="http://hbase.apache.org">Apache HBase Project</a>
 */
public class ZooKeeperWatcher implements Watcher, Abortable {
    // Certain ZooKeeper nodes need to be world-readable
    @SuppressWarnings("serial")
    public static final ArrayList<ACL> CREATOR_ALL_AND_WORLD_READABLE = new ArrayList<ACL>() {
        {
            add(new ACL(ZooDefs.Perms.READ, ZooDefs.Ids.ANYONE_ID_UNSAFE));
            add(new ACL(ZooDefs.Perms.ALL, ZooDefs.Ids.AUTH_IDS));
        }
    };
    // Identifier for this watcher (for logging only). It is made of the prefix
    // passed on construction and the zookeeper sessionid.
    // private String identifier;
    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperWatcher.class);
    protected final ZKConfig conf;
    // listeners to be notified
    private final List<ZooKeeperListener> listeners = new CopyOnWriteArrayList<>();
    private final Exception constructorCaller;
    // abortable in case of zk failure
    private Abortable abortable;

    // node names
    // Used by ZKUtil:waitForZKConnectionIfAuthenticating to wait for SASL
    // negotiation to complete
    private CountDownLatch saslLatch = new CountDownLatch(1);
    // base znode for this cluster
    private String baseZNode;
    // zookeeper quorum
    private String quorum;
    // zookeeper connection
    private RecoverableZooKeeper recoverableZooKeeper;
    // znode of currently active master
    private String masterAddressZNode;

    public ZooKeeperWatcher(ZKConfig conf) throws ZooKeeperConnectionException, IOException {
        this(conf, null, false);
    }

    /**
     * Instantiate a ZooKeeper connection and watcher.
     * <p/>
     * Descriptive string that is added to zookeeper sessionid and used as identifier for this
     * instance.
     */
    public ZooKeeperWatcher(ZKConfig conf, Abortable abortable) throws ZooKeeperConnectionException,
            IOException {
        this(conf, abortable, false);
    }

    /**
     * Instantiate a ZooKeeper connection and watcher.
     * <p/>
     * Descriptive string that is added to zookeeper sessionid and used as identifier for this
     * instance.
     */
    public ZooKeeperWatcher(ZKConfig conf, Abortable abortable, boolean canCreateBaseZNode)
            throws IOException, ZooKeeperConnectionException {
        this.conf = conf;
        // Capture a stack trace now. Will print it out later if problem so we
        // can
        // distinguish amongst the myriad ZKWs.
        try {
            throw new Exception("ZKW CONSTRUCTOR STACK TRACE FOR DEBUGGING");
        } catch (Exception e) {
            this.constructorCaller = e;
        }
        this.quorum = conf.getZkServerAddr();
        // Identifier will get the sessionid appended later below down when we
        // handle the syncconnect event.
        // this.identifier = descriptor;
        this.abortable = abortable;
        setNodeNames(conf);
        this.recoverableZooKeeper = ZKUtil.connect(conf, this);
        if (canCreateBaseZNode) {
            createBaseZNodes();
        }
    }

    private void createBaseZNodes() throws ZooKeeperConnectionException {
        try {
            // Create all the necessary "directories" of znodes
            ZKUtil.createAndFailSilent(this, baseZNode);
        } catch (KeeperException e) {
            throw new ZooKeeperConnectionException(
                    prefix("Unexpected KeeperException creating base node"), e);
        }
    }

    @Override
    public String toString() {
        return recoverableZooKeeper == null ? "" : Long.toHexString(this.recoverableZooKeeper
                .getSessionId());
    }

    /**
     * Adds this instance's identifier as a prefix to the passed <code>str</code>
     *
     * @param str String to amend.
     * @return A new string with this instance's identifier as prefix
     */
    public String prefix(final String str) {
        return this.toString() + " " + str;
    }

    /**
     * Set the local variable node names using the specified ZKConfig.
     */
    private void setNodeNames(ZKConfig conf) {
        baseZNode = conf.getZkNodeRoot();
        masterAddressZNode = ZKUtil.joinZNode(baseZNode, "master");
    }

    public String getBaseZNode() {
        return baseZNode;
    }

    public void saslLatchAwait() throws InterruptedException {
        saslLatch.await();
    }

    /**
     * Register the specified listener to receive ZooKeeper events.
     */
    public void registerListener(ZooKeeperListener listener) {
        listeners.add(listener);
    }

    /**
     * Register the specified listener to receive ZooKeeper events and add it as the first in the
     * list of current listeners.
     */
    public void registerListenerFirst(ZooKeeperListener listener) {
        listeners.add(0, listener);
    }

    /**
     * Get the connection to ZooKeeper.
     *
     * @return connection reference to zookeeper
     */
    public RecoverableZooKeeper getRecoverableZooKeeper() {
        return recoverableZooKeeper;
    }

    public void reconnectAfterExpiration() throws IOException, InterruptedException {
        recoverableZooKeeper.reconnectAfterExpiration();
    }

    /**
     * Get the quorum address of this instance.
     *
     * @return quorum string of this zookeeper connection instance
     */
    public String getQuorum() {
        return quorum;
    }

    public void unregisterListener(ZooKeeperListener listener) {
        int i = 0;
        boolean exists = false;
        for (ZooKeeperListener listenerOfList : this.listeners) {
            if (listenerOfList.equals(listener)) {
                exists = true;
                break;
            }
            i++;
        }
        if (exists) {
            listeners.remove(i);
        }
    }

    /**
     * Method called from ZooKeeper for events and connection status.
     * <p/>
     * Valid events are passed along to listeners. Connection status changes are dealt with
     * locally.
     */
    @Override
    public void process(WatchedEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug(prefix("Received ZooKeeper Event, " + "type=" + event.getType() + ", " + "state="
                    + event.getState() + ", " + "path=" + event.getPath()));
        }

        switch (event.getType()) {

            // If event type is NONE, this is a connection status change
            case None: {
                connectionEvent(event);
                break;
            }

            // Otherwise pass along to the listeners

            case NodeCreated: {
                for (ZooKeeperListener listener : listeners) {
                    listener.nodeCreated(event.getPath());
                }
                break;
            }

            case NodeDeleted: {
                for (ZooKeeperListener listener : listeners) {
                    listener.nodeDeleted(event.getPath());
                }
                break;
            }

            case NodeDataChanged: {
                for (ZooKeeperListener listener : listeners) {
                    listener.nodeDataChanged(event.getPath());
                }
                break;
            }

            case NodeChildrenChanged: {
                for (ZooKeeperListener listener : listeners) {
                    listener.nodeChildrenChanged(event.getPath());
                }
                break;
            }
        }
    }

    // Connection management

    /**
     * Called when there is a connection-related event via the Watcher callback.
     * <p/>
     * If Disconnected or Expired, this should shutdown the cluster. But, since we send a
     * KeeperException.SessionExpiredException along with the abort call, it's possible for the
     * Abortable to catch it and try to create a new session with ZooKeeper. This is what the client
     * does in HCM.
     * <p/>
     */
    protected void connectionEvent(WatchedEvent event) {
        switch (event.getState()) {
            case SyncConnected:
                // Now, this callback can be invoked before the this.zookeeper is
                // set.
                // Wait a little while.
                long finished = System.currentTimeMillis() + this.conf.getZkSyncTimeMs();
                while (System.currentTimeMillis() < finished) {
                    ThreadUtils.sleep(1);
                    if (this.recoverableZooKeeper != null) {
                        break;
                    }
                }
                if (this.recoverableZooKeeper == null) {
                    logger.error("ZK is null on connection event -- see stack trace "
                                    + "for the stack trace when constructor was called on this zkw",
                            this.constructorCaller);
                    throw new NullPointerException("ZK is null");
                }
                // this.identifier = this.identifier + "-0x" +
                // Long.toHexString(this.recoverableZooKeeper.getSessionId());
                // Update our identifier. Otherwise ignore.
                if (logger.isDebugEnabled()) {
                    logger.debug(Long.toHexString(this.recoverableZooKeeper.getSessionId()) + " connected");
                }
                break;

            case SaslAuthenticated:
                if (ZKUtil.isSecureZooKeeper()) {
                    // We are authenticated, clients can proceed.
                    saslLatch.countDown();
                }
                break;

            case AuthFailed:
                if (ZKUtil.isSecureZooKeeper()) {
                    // We could not be authenticated, but clients should proceed
                    // anyway.
                    // Only access to znodes that require SASL authentication will
                    // be
                    // denied. The client may never need to access them.
                    saslLatch.countDown();
                }
                break;

            // Abort or other the server if Disconnected or Expired
            case Disconnected:
                if (logger.isDebugEnabled()) {
                    logger.debug(prefix("Received Disconnected from ZooKeeper, ignoring"));
                }
                recoverableZooKeeper.ensureConnectivity(null);
                break;
            case Expired:
                if (ZKUtil.isSecureZooKeeper()) {
                    // We consider Expired equivalent to AuthFailed for this
                    // connection. Authentication is never going to complete. The
                    // client should proceed to do cleanup.
                    saslLatch.countDown();
                }
                String msg =
                        prefix(Long.toHexString(this.recoverableZooKeeper.getSessionId())
                                + " received expired from " + "ZooKeeper, aborting");
                // TODO: One thought is to add call to ZooKeeperListener so say,
                // ZooKeeperNodeTracker can zero out its data values.
                if (this.abortable != null) {
                    this.abortable.abort(msg, new KeeperException.SessionExpiredException());
                }

                recoverableZooKeeper.ensureConnectivity(null);
                break;
        }
    }

    /**
     * Forces a synchronization of this ZooKeeper client connection.
     * <p/>
     * Executing this method before running other methods will ensure that the subsequent operations
     * are up-to-date and consistent as of the time that the sync is complete.
     * <p/>
     * This is used for compareAndSwap type operations where we need to read the data of an existing
     * node and delete or transition that node, utilizing the previously read version and data. We
     * want to ensure that the version read is up-to-date from when we begin the operation.
     */
    public void sync(String path) {
        this.recoverableZooKeeper.sync(path, null, null);
    }

    /**
     * Handles KeeperExceptions in client calls.
     * <p/>
     * This may be temporary but for now this gives one place to deal with these.
     * <p/>
     * TODO: Currently this method rethrows the exception to let the caller handle
     * <p/>
     */
    public void keeperException(KeeperException ke) throws KeeperException {
        logger.error(prefix("Received unexpected KeeperException, re-throwing exception"), ke);
        throw ke;
    }

    /**
     * Handles InterruptedExceptions in client calls.
     * <p/>
     * This may be temporary but for now this gives one place to deal with these.
     * <p/>
     * TODO: Currently, this method does nothing. Is this ever expected to happen? Do we abort or
     * can we let it run? Maybe this should be logged as WARN? It shouldn't happen?
     * <p/>
     */
    public void interruptedException(InterruptedException ie) {
        if (logger.isDebugEnabled()) {
            logger.debug(prefix("Received InterruptedException, doing nothing here"), ie);
        }
        // At least preserver interrupt.
        Thread.currentThread().interrupt();
        // no-op
    }

    /**
     * Close the connection to ZooKeeper.
     */
    public void close() {
        try {
            if (recoverableZooKeeper != null) {
                recoverableZooKeeper.close();
                recoverableZooKeeper = null;
                // super.close();
            }
        } catch (InterruptedException e) {
            //
        }
    }

    public ZKConfig getZKConfig() {
        return conf;
    }

    @Override
    public void abort(String why, Throwable e) {
        this.abortable.abort(why, e);
    }

    @Override
    public boolean isAborted() {
        return this.abortable.isAborted();
    }

    /**
     * Get the active Zookeeper address
     *
     * @return Path to the currently active master.
     */
    public String getMasterAddressZNode() {
        return this.masterAddressZNode;
    }
}
