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
package com.qlangtech.tis.sql.parser.stream.generate;

import org.apache.commons.lang.StringUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class FacadeContext {

    private String fullFacadeClassName;

    private String facadeInstanceName;

    private String facadeInterfaceName;

    public String getFullFacadeClassName() {
        return fullFacadeClassName;
    }

    public void setFullFacadeClassName(String fullFacadeClassName) {
        this.fullFacadeClassName = fullFacadeClassName;
    }

    public String getFacadeInstanceName() {
        return facadeInstanceName;
    }

    public String getFacadeInstanceSetterName() {
        return "set" + StringUtils.capitalize(this.getFacadeInstanceName());
    }

    public void setFacadeInstanceName(String facadeInstanceName) {
        this.facadeInstanceName = facadeInstanceName;
    }

    public String getFacadeInterfaceName() {
        return facadeInterfaceName;
    }

    public void setFacadeInterfaceName(String facadeInterfaceName) {
        this.facadeInterfaceName = facadeInterfaceName;
    }
}
