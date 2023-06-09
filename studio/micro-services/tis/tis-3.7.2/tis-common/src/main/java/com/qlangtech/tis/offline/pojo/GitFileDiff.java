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
package com.qlangtech.tis.offline.pojo;

import org.json.JSONObject;

/**
 * git里文件两个版本的diff
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class GitFileDiff {

    private String oldPath;

    private String newPath;

    private int aMode;

    private int bMode;

    private String diff;

    private boolean newFile;

    private boolean renamedFile;

    private boolean deletedFile;

    public GitFileDiff() {
    }

    public GitFileDiff(JSONObject jsonObject) {
        this.oldPath = jsonObject.getString("old_path");
        this.newPath = jsonObject.getString("new_path");
        this.aMode = jsonObject.getInt("a_mode");
        this.bMode = jsonObject.getInt("b_mode");
        this.diff = jsonObject.getString("diff");
        this.newFile = jsonObject.getBoolean("new_file");
        this.renamedFile = jsonObject.getBoolean("renamed_file");
        this.deletedFile = jsonObject.getBoolean("deleted_file");
    }

    public String getOldPath() {
        return oldPath;
    }

    public void setOldPath(String oldPath) {
        this.oldPath = oldPath;
    }

    public String getNewPath() {
        return newPath;
    }

    public void setNewPath(String newPath) {
        this.newPath = newPath;
    }

    public int getaMode() {
        return aMode;
    }

    public void setaMode(int aMode) {
        this.aMode = aMode;
    }

    public int getbMode() {
        return bMode;
    }

    public void setbMode(int bMode) {
        this.bMode = bMode;
    }

    public String getDiff() {
        return diff;
    }

    public void setDiff(String diff) {
        this.diff = diff;
    }

    public boolean isNewFile() {
        return newFile;
    }

    public void setNewFile(boolean newFile) {
        this.newFile = newFile;
    }

    public boolean isRenamedFile() {
        return renamedFile;
    }

    public void setRenamedFile(boolean renamedFile) {
        this.renamedFile = renamedFile;
    }

    public boolean isDeletedFile() {
        return deletedFile;
    }

    public void setDeletedFile(boolean deletedFile) {
        this.deletedFile = deletedFile;
    }
}
