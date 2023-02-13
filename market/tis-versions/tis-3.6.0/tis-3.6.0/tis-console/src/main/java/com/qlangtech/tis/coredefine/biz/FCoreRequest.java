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
package com.qlangtech.tis.coredefine.biz;

import com.qlangtech.tis.coredefine.module.action.CoreAction.CoreRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-4-15
 */
public class FCoreRequest {

    protected final CoreRequest request;

    private boolean valid = false;

    protected final List<String> ips = new ArrayList<String>(0);

    // 已经分配到的组数
    private final int assigndGroup;

    public FCoreRequest(CoreRequest request, int groupCount) {
        this(request, groupCount, 0);
    }

    /**
     * @param request
     * @param groupCount 现在应用的总组数
     */
    public FCoreRequest(CoreRequest request, int groupCount, int assigndGroup) {
        super();
        this.request = request;
        if (groupCount < 1) {
            throw new IllegalArgumentException("groupCount can not be null");
        }
        this.replicCount = new short[groupCount];
        this.assigndGroup = assigndGroup;
    }

    public String getCreateNodeSet() {
        // return ips.stream().map((ip) -> StringUtils.substringBefore(ip, ":")).collect(Collectors.joining(","));
        return ips.stream().collect(Collectors.joining(","));
    }

    public String getIndexName() {
        return this.request.getServiceName();
    }

    // 标识每个组内副本个数
    private final short[] replicCount;

    private final Map<Integer, Collection<String>> serversView = new HashMap<Integer, Collection<String>>();

    public void addNodeIps(int group, String ip) {
        ips.add(ip);
        request.addNodeIps(String.valueOf(group), ip);
        replicCount[group]++;
        Collection<String> servers = serversView.get(group);
        if (servers == null) {
            servers = new ArrayList<String>();
            serversView.put(group, servers);
        }
        servers.add(ip);
    }

    public Map<Integer, Collection<String>> getServersView() {
        return this.serversView;
    }

    public short[] getReplicCount() {
        return Arrays.copyOfRange(replicCount, assigndGroup, replicCount.length);
    }

    public String[] getIps() {
        return ips.toArray(new String[ips.size()]);
    }

    public List<String> getAllIps() {
        return this.ips;
    }

    /**
     * @param valid the valid to set
     */
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /**
     * @return the valid
     */
    public boolean isValid() {
        return valid;
    }

    public CoreRequest getRequest() {
        return request;
    }
}
