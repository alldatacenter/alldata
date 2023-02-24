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
package org.apache.impala.hooks;

/**
 * {@link QueryEventHook} is the interface for implementations that
 * can hook into supported events in Impala query execution.
 */
public interface QueryEventHook {
    /**
     * Hook method invoked when the Impala daemon starts up.
     * <p>
     * This method will block completion of daemon startup, so you should
     * execute any long-running actions asynchronously.
     * </p>
     * <h3>Error-Handling</h3>
     * <p>
     * Any {@link Exception} thrown from this method will effectively fail
     * Impala startup with an error. Implementations should handle all
     * exceptions as gracefully as they can, even if the end result is to
     * throw them.
     * </p>
     */
    void onImpalaStartup();

    /**
     * Hook method invoked asynchronously when a (qualifying) Impala query
     * has executed, but before it has returned.
     * <p>
     * This method will not block the invoking or subsequent queries,
     * but may block future hook invocations if it runs for too long
     * </p>
     * <h3>Error-Handling</h3>
     * <p>
     * Any {@link Throwable} thrown from this method will only be caught
     * and logged and will not affect the result of any query.  Hook implementations
     * should make a best-effort to handle their own exceptions.
     * </p>
     * <h3>Important:</h3>
     * <p>
     * This hook is actually invoked when the query is <i>unregistered</i>,
     * which may happen a long time after the query has executed.
     * e.g. the following sequence is possible:
     * <ol>
     *  <li>User executes query from Hue.
     *  <li>User goes home for weekend, leaving Hue tab open in browser
     *  <li>If we're lucky, the session timeout expires after some amount of idle time.
     *  <li>The query gets unregistered, lineage record gets logged
     * </ol>
     * </p>
     * <h3>Service Guarantees</h3>
     *
     * Impala makes the following guarantees about how this method is executed
     * with respect to other implementations that may be registered:
     *
     * <h4>Hooks are executed asynchronously</h4>
     *
     * All hook execution happens asynchronously of the query that triggered
     * them.  Hooks may still be executing after the query response has returned
     * to the caller.  Additionally, hooks may execute concurrently if the
     * hook executor thread size is configured appropriately.
     *
     * <h4>Hook Invocation is in Configuration Order</h4>
     *
     * The <i>submission</i> of the hook execution tasks occurs in the order
     * that the hooks were defined in configuration.  This generally means that
     * hooks will <i>start</i> executing in order, but there are no guarantees
     * about finishing order.
     * <p>
     * For example, if configured with {@code query_event_hook_classes=hook1,hook2,hook3},
     * then hook1 will start before hook2, and hook2 will start before hook3.
     * If you need to guarantee that hook1 <i>completes</i> before hook2 starts, then
     * you should specify {@code query_event_hook_nthreads=1} for serial hook
     * execution.
     * </p>
     *
     * <h4>Hook Execution Blocks</h4>
     *
     * A hook will block the thread it executes on until it completes.  If a hook hangs,
     * then the thread also hangs.  Impala (currently) will not check for hanging hooks to
     * take any action.  This means that if you have {@code query_event_hook_nthreads}
     * less than the number of hooks, then 1 hook may effectively block others from
     * executing.
     *
     * <h4>Hook Exceptions are non-fatal</h4>
     *
     * Any exception thrown from this hook method will be logged and ignored.  Therefore,
     * an exception in 1 hook will not affect another hook (when no shared resources are
     * involved).
     *
     * <h4>Hook Execution may end abruptly at Impala shutdown</h4>
     *
     * If a hook is still executing when Impala is shutdown, there are no guarantees
     * that it will complete execution before being killed.
     *
     *
     * @param context object containing the post execution context
     *                of the query
     */
    void onQueryComplete(QueryCompleteContext context);
}
