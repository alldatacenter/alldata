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
package com.qlangtech.tis.tair;

import java.io.Serializable;
import com.qlangtech.tis.tair.imp.ITSearchCache;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2014年8月29日下午5:21:51
 */
public class TSearchCache implements ITSearchCache {

    @Override
    public boolean put(Serializable key, Serializable obj) {
        return false;
    }

    @Override
    public boolean put(Serializable key, Serializable obj, int expir) {
        return false;
    }

    @Override
    public boolean invalid(Serializable key) {
        return false;
    }

    @Override
    public <T> T getObj(Serializable key) {
        return null;
    }
    // private static final Logger LOGGER_LOG =
    // LoggerFactory.getLogger(TSearchCache.class);
    //
    // private static final int NAMESPACE = 728;
    // private MultiClusterTairManager mdbTairManager;
    //
    // public MultiClusterTairManager getMdbTairManager() {
    // return mdbTairManager;
    // }
    //
    // @Autowired
    // public void setMdbTairManager(MultiClusterTairManager mdbTairManager) {
    // this.mdbTairManager = mdbTairManager;
    // }
    //
    // @Override
    // public boolean put(Serializable key, Serializable obj) {
    // ResultCode rc = mdbTairManager.put(NAMESPACE, key, obj);
    // return rc.isSuccess();
    // }
    //
    // @Override
    // public boolean put(Serializable key, Serializable obj, int expir) {
    // ResultCode rc = mdbTairManager.put(NAMESPACE, key, obj, 0, expir);
    // return rc.isSuccess();
    // }
    //
    // @Override
    // public boolean invalid(Serializable key) {
    // ResultCode rc = mdbTairManager.invalid(NAMESPACE, key);
    // return rc.isSuccess();
    // }
    //
    // @SuppressWarnings("all")
    // public <T> T getObj(Serializable key) {
    // Result<DataEntry> result = mdbTairManager.get(NAMESPACE, key);
    // if (ResultCode.DATANOTEXSITS.equals(result.getRc())) {
    // return null;
    // }
    // if (ResultCode.SUCCESS.equals(result.getRc())) {
    // return (T) result.getValue().getValue();
    // }
    //
    // LOGGER_LOG.error("get " + key + " failed," + result.getRc().getCode());
    // return null;
    //
    //
    // }
}
