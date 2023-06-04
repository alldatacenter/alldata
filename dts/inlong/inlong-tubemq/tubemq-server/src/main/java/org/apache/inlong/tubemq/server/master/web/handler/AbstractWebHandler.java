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

package org.apache.inlong.tubemq.server.master.web.handler;

import static org.apache.inlong.tubemq.server.common.webbase.WebMethodMapper.getRegisteredWebMethod;
import static org.apache.inlong.tubemq.server.common.webbase.WebMethodMapper.registerWebMethod;
import org.apache.inlong.tubemq.server.master.TMaster;
import org.apache.inlong.tubemq.server.master.metamanage.MetaDataService;

public abstract class AbstractWebHandler {

    protected TMaster master;
    protected MetaDataService defMetaDataService;

    public AbstractWebHandler(TMaster master) {
        this.master = master;
        this.defMetaDataService = this.master.getMetaDataService();
    }

    public abstract void registerWebApiMethod();

    protected void registerQueryWebMethod(String webMethodName,
            String clsMethodName) {
        innRegisterWebMethod(webMethodName, clsMethodName, false, false);
    }

    protected void registerModifyWebMethod(String webMethodName,
            String clsMethodName) {
        innRegisterWebMethod(webMethodName, clsMethodName, true, true);
    }

    protected int getRegisteredMethods(StringBuilder sBuffer) {
        return getRegisteredWebMethod(sBuffer);
    }

    private void innRegisterWebMethod(String webMethodName,
            String clsMethodName,
            boolean onlyMasterOp,
            boolean needAuthToken) {
        registerWebMethod(webMethodName, clsMethodName,
                onlyMasterOp, needAuthToken, this);
    }
}
