/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas;

import com.google.common.annotations.VisibleForTesting;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.atlas.annotation.GraphTransaction;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.exception.NotFoundException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.tasks.TaskManagement;
import org.apache.atlas.utils.AtlasPerfMetrics.MetricRecorder;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class GraphTransactionInterceptor implements MethodInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(GraphTransactionInterceptor.class);

    @VisibleForTesting
    private static final ObjectUpdateSynchronizer               OBJECT_UPDATE_SYNCHRONIZER = new ObjectUpdateSynchronizer();
    private static final ThreadLocal<List<PostTransactionHook>> postTransactionHooks       = new ThreadLocal<>();
    private static final ThreadLocal<Boolean>                   isTxnOpen                  = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean>                   innerFailure               = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Map<String, AtlasVertex>>  guidVertexCache            = ThreadLocal.withInitial(() -> new HashMap<>());

    private final AtlasGraph     graph;
    private final TaskManagement taskManagement;

    private static final ThreadLocal<Map<Object, String>> vertexGuidCache =
            new ThreadLocal<Map<Object, String>>() {
                @Override
                public Map<Object, String> initialValue() {
                    return new HashMap<Object, String>();
                }
            };

    private static final ThreadLocal<Map<Object, AtlasEntity.Status>> vertexStateCache =
            new ThreadLocal<Map<Object, AtlasEntity.Status>>() {
                @Override
                public Map<Object, AtlasEntity.Status> initialValue() {
                    return new HashMap<Object, AtlasEntity.Status>();
                }
            };

    private static final ThreadLocal<Map<Object, AtlasEntity.Status>> edgeStateCache =
            new ThreadLocal<Map<Object, AtlasEntity.Status>>() {
                @Override
                public Map<Object, AtlasEntity.Status> initialValue() {
                    return new HashMap<Object, AtlasEntity.Status>();
                }
            };

    @Inject
    public GraphTransactionInterceptor(AtlasGraph graph, TaskManagement taskManagement) {
        this.graph          = graph;
        this.taskManagement = taskManagement;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method        method            = invocation.getMethod();
        String        invokingClass     = method.getDeclaringClass().getSimpleName();
        String        invokedMethodName = method.getName();
        boolean       logRollback       = method.getAnnotation(GraphTransaction.class) == null || method.getAnnotation(GraphTransaction.class).logRollback();

        boolean isInnerTxn = isTxnOpen.get();
        // Outermost txn marks any subsequent transaction as inner
        isTxnOpen.set(Boolean.TRUE);

        if (LOG.isDebugEnabled() && isInnerTxn) {
            LOG.debug("Txn entry-point {}.{} is inner txn. Commit/Rollback will be ignored", invokingClass, invokedMethodName);
        }

        boolean isSuccess = false;
        MetricRecorder metric = null;

        try {
            try {
                Object response = invocation.proceed();

                if (isInnerTxn) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Ignoring commit for nested/inner transaction {}.{}", invokingClass, invokedMethodName);
                    }
                } else {
                    metric = RequestContext.get().startMetricRecord("graphCommit");

                    doCommitOrRollback(invokingClass, invokedMethodName);
                }

                isSuccess = !innerFailure.get();

                return response;
            } catch (Throwable t) {
                if (isInnerTxn) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Ignoring rollback for nested/inner transaction {}.{}", invokingClass, invokedMethodName);
                    }
                    innerFailure.set(true);
                } else {
                    doRollback(logRollback, t);
                }
                throw t;
            }
        } finally {
            RequestContext.get().endMetricRecord(metric);

            // Only outer txn can mark as closed
            if (!isInnerTxn) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Closing outer txn");
                }

                // Reset the boolean flags
                isTxnOpen.set(Boolean.FALSE);
                innerFailure.set(Boolean.FALSE);
                clearCache();

                List<PostTransactionHook> trxHooks = postTransactionHooks.get();

                if (trxHooks != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Processing post-txn hooks");
                    }

                    postTransactionHooks.remove();

                    for (PostTransactionHook trxHook : trxHooks) {
                        try {
                            trxHook.onComplete(isSuccess);
                        } catch (Throwable t) {
                            LOG.error("postTransactionHook failed", t);
                        }
                    }
                }
            }

            OBJECT_UPDATE_SYNCHRONIZER.releaseLockedObjects();

            if (isSuccess) {
                submitTasks();
            }
        }
    }

    private void doCommitOrRollback(final String invokingClass, final String invokedMethodName) {
        if (innerFailure.get()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Inner/Nested call threw exception. Rollback on txn entry-point, {}.{}", invokingClass, invokedMethodName);
            }
            graph.rollback();
        } else {
            doCommit(invokingClass, invokedMethodName);
        }
    }

    private void doCommit(final String invokingClass, final String invokedMethodName) {
        graph.commit();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Graph commit txn {}.{}", invokingClass, invokedMethodName);
        }
    }

    private void doRollback(boolean logRollback, final Throwable t) {
        if (logRollback) {
            if (logException(t)) {
                LOG.error("graph rollback due to exception ", t);
            } else {
                LOG.error("graph rollback due to exception {}:{}", t.getClass().getSimpleName(), t.getMessage());
            }
        }

        graph.rollback();
    }

    public static void lockObjectAndReleasePostCommit(final String guid) {
        OBJECT_UPDATE_SYNCHRONIZER.lockObject(guid);
    }

    public static void lockObjectAndReleasePostCommit(final List<String> guids) {
        OBJECT_UPDATE_SYNCHRONIZER.lockObject(guids);
    }

    public static void addToVertexCache(String guid, AtlasVertex vertex) {
        Map<String, AtlasVertex> cache = guidVertexCache.get();

        cache.put(guid, vertex);
    }

    public static void removeFromVertexCache(String guid) {
        Map<String, AtlasVertex> cache = guidVertexCache.get();

        cache.remove(guid);
    }

    public static AtlasVertex getVertexFromCache(String guid) {
        Map<String, AtlasVertex> cache = guidVertexCache.get();

        return cache.get(guid);
    }

    public static void clearCache() {
        guidVertexCache.get().clear();
        vertexGuidCache.get().clear();
        vertexStateCache.get().clear();
        edgeStateCache.get().clear();
    }

    private void submitTasks() {
        if (CollectionUtils.isEmpty(RequestContext.get().getQueuedTasks()) || taskManagement == null) {
            return;
        }

        taskManagement.addAll(RequestContext.get().getQueuedTasks());
    }

    boolean logException(Throwable t) {
        if (t instanceof AtlasBaseException) {
            Response.Status httpCode = ((AtlasBaseException) t).getAtlasErrorCode().getHttpCode();
            return httpCode != Response.Status.NOT_FOUND && httpCode != Response.Status.NO_CONTENT;
        } else if (t instanceof NotFoundException) {
            return false;
        } else {
            return true;
        }
    }

    public static void addToVertexGuidCache(Object vertexId, String guid) {

        if (guid == null) {
            removeFromVertexGuidCache(vertexId);
        } else {
            Map<Object, String> cache = vertexGuidCache.get();
            cache.put(vertexId, guid);
        }
    }

    public static void removeFromVertexGuidCache(Object vertexId) {
        Map<Object, String> cache = vertexGuidCache.get();

        cache.remove(vertexId);
    }

    public static String getVertexGuidFromCache(Object vertexId) {
        Map<Object, String> cache = vertexGuidCache.get();

        return cache.get(vertexId);
    }

    public static void addToVertexStateCache(Object vertexId, AtlasEntity.Status status) {

        if (status == null) {
            removeFromVertexStateCache(vertexId);
        } else {
            Map<Object, AtlasEntity.Status> cache = vertexStateCache.get();
            cache.put(vertexId, status);
        }
    }

    public static void removeFromVertexStateCache(Object vertexId) {
        Map<Object, AtlasEntity.Status> cache = vertexStateCache.get();

        cache.remove(vertexId);
    }

    public static AtlasEntity.Status getVertexStateFromCache(Object vertexId) {
        Map<Object, AtlasEntity.Status> cache = vertexStateCache.get();

        return cache.get(vertexId);
    }

    public static void addToEdgeStateCache(Object edgeId, AtlasEntity.Status status) {

        if (status == null) {
            removeFromEdgeStateCache(edgeId);
        } else {
            Map<Object, AtlasEntity.Status> cache = edgeStateCache.get();
            cache.put(edgeId, status);
        }
    }

    public static void removeFromEdgeStateCache(Object edgeId) {
        Map<Object, AtlasEntity.Status> cache = edgeStateCache.get();

        cache.remove(edgeId);
    }

    public static AtlasEntity.Status getEdgeStateFromCache(Object edgeId) {
        Map<Object, AtlasEntity.Status> cache = edgeStateCache.get();

        return cache.get(edgeId);
    }

    public static abstract class PostTransactionHook {
        protected PostTransactionHook() {
            List<PostTransactionHook> trxHooks = postTransactionHooks.get();

            if (trxHooks == null) {
                trxHooks = new ArrayList<>();
                postTransactionHooks.set(trxHooks);
            }

            trxHooks.add(this);
        }

        public abstract void onComplete(boolean isSuccess);
    }

    private static class RefCountedReentrantLock extends ReentrantLock {
        private int refCount;

        public RefCountedReentrantLock() {
            this.refCount = 0;
        }

        public int increment() {
            return ++refCount;
        }

        public int decrement() {
            return --refCount;
        }

        public int getRefCount() { return refCount; }
    }


    public static class ObjectUpdateSynchronizer {
        private final Map<String, RefCountedReentrantLock> guidLockMap = new ConcurrentHashMap<>();
        private final ThreadLocal<List<String>>  lockedGuids = new ThreadLocal<List<String>>() {
            @Override
            protected List<String> initialValue() {
                return new ArrayList<>();
            }
        };

        public void lockObject(final List<?> guids) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("==> lockObject(): guids: {}", guids);
            }

            Collections.sort((List<String>) guids);
            for (String g : (List<String>) guids) {
                lockObject(g);
            }
        }

        private void lockObject(final String guid) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("==> lockObject(): guid: {}, guidLockMap.size: {}", guid, guidLockMap.size());
            }

            ReentrantLock lock = getOrCreateObjectLock(guid);
            lock.lock();

            lockedGuids.get().add(guid);

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== lockObject(): guid: {}, guidLockMap.size: {}", guid, guidLockMap.size());
            }
        }

        public void releaseLockedObjects() {
            if (LOG.isDebugEnabled()) {
                LOG.debug("==> releaseLockedObjects(): lockedGuids.size: {}", lockedGuids.get().size());
            }

            for (String guid : lockedGuids.get()) {
                releaseObjectLock(guid);
            }

            lockedGuids.get().clear();

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== releaseLockedObjects(): lockedGuids.size: {}", lockedGuids.get().size());
            }
        }

        private RefCountedReentrantLock getOrCreateObjectLock(String guid) {
            synchronized (guidLockMap) {
                RefCountedReentrantLock ret = guidLockMap.get(guid);
                if (ret == null) {
                    ret = new RefCountedReentrantLock();
                    guidLockMap.put(guid, ret);
                }

                ret.increment();
                return ret;
            }
        }

        private RefCountedReentrantLock releaseObjectLock(String guid) {
            synchronized (guidLockMap) {
                RefCountedReentrantLock lock = guidLockMap.get(guid);
                if (lock != null && lock.isHeldByCurrentThread()) {
                    int refCount = lock.decrement();

                    if (refCount == 0) {
                        guidLockMap.remove(guid);
                    }

                    lock.unlock();
                } else {
                    LOG.warn("releaseLockedObjects: {} Attempting to release a lock not held by current thread.", guid);
                }

                return lock;
            }
        }
    }
}
