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

package org.apache.solr.handler.admin;

import com.qlangtech.tis.fs.IPath;
import com.qlangtech.tis.fs.ITISFileSystem;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NoLockFactory;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.response.SolrQueryResponse;

import java.io.File;
import java.io.IOException;

/**
 * @author: baisui 百岁
 * @create: 2021-03-09 09:15
 **/
public class MockTisCoreAdminHandler extends TisCoreAdminHandler {
    public MockTisCoreAdminHandler(CoreContainer coreContainer) {
        super(coreContainer);
    }

    //  protected void downloadIndexFile2IndexDir(TaskObject taskObj, long hdfsTimeStamp, String solrCoreName, final File indexDir, final SolrQueryResponse rsp, String taskId)
    @Override
    public void downloadIndexFile2IndexDir(TaskObject taskObj, long hdfsTimeStamp, String solrCoreName, File indexDir, SolrQueryResponse rsp, String taskId) {
        super.downloadIndexFile2IndexDir(taskObj, hdfsTimeStamp, solrCoreName, indexDir, rsp, taskId);
    }

//    public void addCompletedTask(String taskId) {
//        TaskObject taskObject = new TaskObject(taskId);
//        this.addTask(COMPLETED, taskObject, false);
//    }

    public void addRunningTask(String taskId) {
        TaskObject taskObject = new TaskObject(taskId);
        this.addTask(RUNNING, taskObject, false);
    }

    @Override
    protected Directory createChildIndexDirectory(ITISFileSystem filesystem, IPath path) throws IOException {
        //return new TisHdfsDirectory(path, filesystem);
        File f = path.unwrap(File.class);
        return new MMapDirectory(f.toPath(), NoLockFactory.INSTANCE);
    }


    void addTask(String type, TaskObject o, boolean limit) {
        super.addTask(type, o, limit);
    }

}
