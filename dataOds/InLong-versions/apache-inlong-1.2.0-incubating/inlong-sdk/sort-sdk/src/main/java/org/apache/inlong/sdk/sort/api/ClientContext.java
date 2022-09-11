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

package org.apache.inlong.sdk.sort.api;

import org.apache.inlong.sdk.sort.stat.StatManager;

public abstract class ClientContext implements Cleanable {

    protected final SortClientConfig config;

    protected final StatManager statManager;

    public ClientContext(SortClientConfig config, MetricReporter reporter) {
        this.config = config;
        this.statManager = new StatManager(this, reporter);
    }

    public SortClientConfig getConfig() {
        return config;
    }

    @Override
    public boolean clean() {
        statManager.clean();
        return true;
    }

    public StatManager getStatManager() {
        return statManager;
    }

    public void acquireRequestPermit() throws InterruptedException {
        config.getGlobalInProgressRequest().acquireUninterruptibly();
    }

    public void releaseRequestPermit() {
        config.getGlobalInProgressRequest().release();
    }
}
