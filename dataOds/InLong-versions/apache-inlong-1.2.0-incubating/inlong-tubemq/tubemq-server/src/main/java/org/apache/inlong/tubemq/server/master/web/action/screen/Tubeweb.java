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

package org.apache.inlong.tubemq.server.master.web.action.screen;

import org.apache.inlong.tubemq.server.master.TMaster;
import org.apache.inlong.tubemq.server.master.metamanage.MetaDataService;
import org.apache.inlong.tubemq.server.master.web.simplemvc.Action;
import org.apache.inlong.tubemq.server.master.web.simplemvc.RequestContext;

public class Tubeweb implements Action {

    private final TMaster master;

    public Tubeweb(TMaster master) {
        this.master = master;
    }

    @Override
    public void execute(RequestContext context) {
        MetaDataService defMetaDataService = this.master.getMetaDataService();
        String masterAdd = defMetaDataService.getMasterAddress();
        if (master.getMasterConfig().isUseWebProxy() || masterAdd == null) {
            // use absolute path
            context.put("tubemqRemoteAddr", "");
        } else {
            // use the whole path of the active master
            context.put("tubemqRemoteAddr", "http://" + masterAdd + ":"
                    + master.getMasterConfig().getWebPort());
        }
    }
}