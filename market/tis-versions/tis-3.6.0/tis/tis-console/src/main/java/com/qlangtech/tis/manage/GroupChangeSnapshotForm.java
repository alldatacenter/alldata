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
package com.qlangtech.tis.manage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2011-12-16
 */
public class GroupChangeSnapshotForm {

    private String groupSnapshot;

    public String getGroupSnapshot() {
        return groupSnapshot;
    }

    public void setGroupSnapshot(String groupSnapshot) {
        this.groupSnapshot = groupSnapshot;
    }

    private static final Pattern pattern = Pattern.compile("(\\d+)-(\\d+)");

    public Integer getSnapshotId() {
        return getInt(1);
    }

    private Integer getInt(int group) {
        Matcher matcher = pattern.matcher(this.getGroupSnapshot());
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(group));
        }
        return null;
    }

    public Integer getGroupId() {
        return getInt(2);
    }

    public static void main(String[] arg) {
        GroupChangeSnapshotForm form = new GroupChangeSnapshotForm();
        form.setGroupSnapshot("1-2");
        System.out.println(form.getSnapshotId());
        System.out.println(form.getGroupId());
    }
}
