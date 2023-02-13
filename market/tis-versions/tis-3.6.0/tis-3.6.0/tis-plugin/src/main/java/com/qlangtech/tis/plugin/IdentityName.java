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
package com.qlangtech.tis.plugin;

/**
 * The plugin global unique identity name
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IdentityName {

    String MSG_ERROR_NAME_DUPLICATE = "名称重复";

//    /**
//     * 相同类型的插件不能重名
//     *
//     * @return
//     */
//    String getName();

    /**
     * 取得唯一ID
     *
     * @return
     */
    //default
    String identityValue();// {
//        Describable plugin = (Describable) this;
//        Descriptor des = plugin.getDescriptor();
//        Objects.requireNonNull(des, " Descriptor of Describable instance of " + plugin.getClass().getName());
//        return des.getIdentityValue(plugin);
    //}

}
