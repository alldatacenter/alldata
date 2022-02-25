/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.notification.spool;

import org.apache.atlas.notification.spool.utils.local.FileOpAppend;
import org.apache.atlas.notification.spool.utils.local.FileOpCompaction;
import org.apache.atlas.notification.spool.utils.local.FileOpDelete;
import org.apache.atlas.notification.spool.utils.local.FileOpRead;
import org.apache.atlas.notification.spool.utils.local.FileOpUpdate;

import java.io.File;

public class FileOperations {
    private final String           emptyRecordJson;
    private final FileOpAppend     fileOpAppend;
    private final FileOpRead       fileOpLoad;
    private final FileOpUpdate     fileOpUpdate;
    private final FileOpCompaction fileOpCompaction;
    private final FileOpDelete     fileOpDelete;

    public FileOperations(String emptyRecordJson, String source) {
        this.emptyRecordJson  = emptyRecordJson;
        this.fileOpAppend     = new FileOpAppend(source);
        this.fileOpLoad       = new FileOpRead(source);
        this.fileOpUpdate     = new FileOpUpdate(source, fileOpAppend);
        this.fileOpCompaction = new FileOpCompaction(source);
        this.fileOpDelete     = new FileOpDelete(source);
    }

    public String[] load(File file) {
        fileOpLoad.perform(file);

        return fileOpLoad.getItems();
    }

    public void delete(File file, String id) {
        fileOpDelete.perform(file, id, emptyRecordJson);
    }

    public void append(File file, String json) {
        fileOpAppend.perform(file, json);
    }

    public void compact(File file) {
        fileOpCompaction.perform(file);
    }

    public void update(File file, String id, String json) {
        fileOpUpdate.setId(id);
        fileOpUpdate.perform(file, json);
    }
}
