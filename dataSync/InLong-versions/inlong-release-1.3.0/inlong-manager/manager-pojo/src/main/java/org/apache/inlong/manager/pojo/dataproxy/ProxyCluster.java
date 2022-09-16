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

package org.apache.inlong.manager.pojo.dataproxy;

/**
 * ProxyCluster
 */
public class ProxyCluster {

    private String clusterName;
    private String clusterTag;
    private String extTag;
    private String extParams;

    /**
     * get clusterName
     * @return the clusterName
     */
    public String getClusterName() {
        return clusterName;
    }

    /**
     * set clusterName
     * @param clusterName the clusterName to set
     */
    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    /**
     * get clusterTag
     * @return the clusterTag
     */
    public String getClusterTag() {
        return clusterTag;
    }

    /**
     * set clusterTag
     * @param clusterTag the clusterTag to set
     */
    public void setClusterTag(String clusterTag) {
        this.clusterTag = clusterTag;
    }

    /**
     * get extTag
     * @return the extTag
     */
    public String getExtTag() {
        return extTag;
    }

    /**
     * set extTag
     * @param extTag the extTag to set
     */
    public void setExtTag(String extTag) {
        this.extTag = extTag;
    }

    /**
     * get extParams
     * @return the extParams
     */
    public String getExtParams() {
        return extParams;
    }

    /**
     * set extParams
     * @param extParams the extParams to set
     */
    public void setExtParams(String extParams) {
        this.extParams = extParams;
    }

}
