/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.impala.hook;

import org.apache.impala.hooks.QueryCompleteContext;
import org.apache.impala.hooks.QueryEventHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImpalaHook implements QueryEventHook {
    private static final Logger LOG = LoggerFactory.getLogger(ImpalaHook.class);

    private ImpalaLineageHook lineageHook;

    /**
     * Execute Impala hook
     */
    public void onQueryComplete(QueryCompleteContext context)  {
        try {
            lineageHook.process(context.getLineageGraph());
        } catch (Exception ex) {
            String errorMessage = String.format("Error in processing impala lineage: {}", context.getLineageGraph());
            LOG.error(errorMessage, ex);
        }
    }

    /**
     * Initialization of Impala hook
     */
    public void onImpalaStartup() {
        lineageHook = new ImpalaLineageHook();
    }
}
