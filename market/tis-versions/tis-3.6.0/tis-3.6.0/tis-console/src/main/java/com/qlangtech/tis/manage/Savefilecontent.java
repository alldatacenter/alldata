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

import java.nio.charset.Charset;

import com.qlangtech.tis.manage.common.TisUTF8;
import junit.framework.Assert;
import com.qlangtech.tis.runtime.module.action.BasicModule;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public final class Savefilecontent {

    private String content;

    private Integer snapshotid;

    // 代表编辑的文件内容
    private String filename;

    private String memo;

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public byte[] getContentBytes() {
        Assert.assertNotNull("content can not be null", content);
        return content.getBytes(TisUTF8.get());
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getSnapshotid() {
        return snapshotid;
    }

    public void setSnapshotid(Integer snapshotid) {
        this.snapshotid = snapshotid;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
