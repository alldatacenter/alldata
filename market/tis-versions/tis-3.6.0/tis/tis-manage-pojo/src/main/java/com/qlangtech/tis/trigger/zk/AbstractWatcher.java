///**
// *   Licensed to the Apache Software Foundation (ASF) under one
// *   or more contributor license agreements.  See the NOTICE file
// *   distributed with this work for additional information
// *   regarding copyright ownership.  The ASF licenses this file
// *   to you under the Apache License, Version 2.0 (the
// *   "License"); you may not use this file except in compliance
// *   with the License.  You may obtain a copy of the License at
// *
// *       http://www.apache.org/licenses/LICENSE-2.0
// *
// *   Unless required by applicable law or agreed to in writing, software
// *   distributed under the License is distributed on an "AS IS" BASIS,
// *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *   See the License for the specific language governing permissions and
// *   limitations under the License.
// */
//package com.qlangtech.tis.trigger.zk;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
////import org.apache.zookeeper.KeeperException;
////import org.apache.zookeeper.WatchedEvent;
////import org.apache.zookeeper.Watcher;
////import org.apache.zookeeper.Watcher.Event.EventType;
//
///**
// * @author 百岁（baisui@qlangtech.com）
// * @date 2019年1月17日
// */
//public abstract class AbstractWatcher implements Watcher {
//
//    private static final Logger log = LoggerFactory.getLogger(AbstractWatcher.class);
//
//    public final void process(WatchedEvent event) {
//        if (EventType.None.equals(event.getType())) {
//            return;
//        }
//        try {
//            final Watcher thisWatch = this;
//            process(thisWatch);
//        } catch (KeeperException e) {
//            if (e.code() == KeeperException.Code.SESSIONEXPIRED || e.code() == KeeperException.Code.CONNECTIONLOSS) {
//                log.warn("ZooKeeper watch triggered, but  cannot talk to ZK");
//                return;
//            }
//            log.error("", e);
//            throw new RuntimeException(e.getMessage(), e);
//        } catch (InterruptedException e) {
//            // Restore the interrupted status
//            Thread.currentThread().interrupt();
//            log.warn("", e);
//            return;
//        }
//    }
//
//    protected abstract void process(Watcher watcher) throws KeeperException, InterruptedException;
//}
