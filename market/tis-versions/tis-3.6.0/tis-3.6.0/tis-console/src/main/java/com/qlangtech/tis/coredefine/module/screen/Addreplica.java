/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.coredefine.module.screen;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.manage.PermissionConstant;
import com.qlangtech.tis.manage.spring.aop.Func;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-8-11
 */
public class Addreplica extends Corenodemanage {

    private static final long serialVersionUID = 1L;

    @Override
    @Func(PermissionConstant.APP_REPLICA_MANAGE)
    public void execute(Context context) throws Exception {
        this.disableNavigationBar(context);
        super.execute(context);
    }

    @Override
    public boolean isEnableDomainView() {
        return false;
    }
}
