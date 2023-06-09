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
package com.qlangtech.tis.trigger.jst;

import com.qlangtech.tis.cloud.ITISCoordinator;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.fullbuild.indexbuild.IIndexBuildParam;
import com.qlangtech.tis.fullbuild.indexbuild.IndexBuildSourcePathCreator;
import com.qlangtech.tis.fullbuild.indexbuild.LuceneVersion;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.Objects;

/**
 * 共享区处理完成信息之后需要向弹内发送直接结果，以执行后续流程
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2014年9月11日下午5:52:50
 */
public class ImportDataProcessInfo implements Serializable, Cloneable, IIndexBuildParam {

    public static final String KEY_DELIMITER = "split_char";

    public static final String DELIMITER_001 = "char001";

    public static final String DELIMITER_TAB = "tab";

    private static final long serialVersionUID = 1L;

    private final Integer taskId;

    private String indexName;

    private String indexBuilder;

    // 编译索引使用的Lucene版本
    private LuceneVersion luceneVersion;

    private String timepoint;

    private String buildTableTitleItems;

    private String hdfsdelimiter;
    private IndexBuildSourcePathCreator buildSourcePathCreator;

    @Override
    public IndexBuildSourcePathCreator getIndexBuildSourcePathCreator() {
        Objects.requireNonNull(buildSourcePathCreator, "buildSourcePathCreator can not be null ");
        return this.buildSourcePathCreator;
    }

    public void setBuildSourcePathCreator(IndexBuildSourcePathCreator buildSourcePathCreator) {
        this.buildSourcePathCreator = buildSourcePathCreator;
    }

    private final ITISFileSystem fileSystem;
    private final ITISCoordinator coordinator;

    public ImportDataProcessInfo(Integer taskId, ITISFileSystem fsFactory, ITISCoordinator coordinator) {
        super();
        this.taskId = taskId;
        this.fileSystem = fsFactory;
        this.coordinator = coordinator;
    }

    @Override
    public ITISCoordinator getCoordinator() {
        return this.coordinator;
    }

    public static String createIndexDir(ITISFileSystem fsFactory, String timePoint, String groupNum, String serviceName, boolean isSourceDir) {
        return fsFactory.getRootDir() + "/" + serviceName + "/all/" + groupNum + (!isSourceDir ? "/output" : StringUtils.EMPTY) + "/" + timePoint;
    }

    @Override
    public String getIndexBuilder() {
        return indexBuilder;
    }

    public void setIndexBuilder(String indexBuilder) {
        this.indexBuilder = indexBuilder;
    }

    public LuceneVersion getLuceneVersion() {
        return luceneVersion;
    }

    public void setLuceneVersion(LuceneVersion luceneVersion) {
        this.luceneVersion = luceneVersion;
    }

    public String getIndexBuildOutputPath(int groupIndex) {
        return createIndexDir(fileSystem, this.timepoint, String.valueOf(groupIndex), this.getIndexName(), false);
    }

    @Override
    public String getHdfsdelimiter() {
        return hdfsdelimiter;
    }

    public void setHdfsdelimiter(String hdfsdelimiter) {
        this.hdfsdelimiter = hdfsdelimiter;
    }


    @Override
    public String getBuildTableTitleItems() {
        return buildTableTitleItems;
    }

    public void setBuildTableTitleItems(String buildTableTitleItems) {
        this.buildTableTitleItems = buildTableTitleItems;
    }

    @Override
    public String getTimepoint() {
        return timepoint;
    }

    public void setTimepoint(String timepoint) {
        this.timepoint = timepoint;
    }

    @Override
    public String getIndexName() {
        return indexName;
    }

    public String getCoreName(int groupNum) {
        return this.getIndexName() + '-' + groupNum;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public Integer getTaskId() {
        return taskId;
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ImportDataProcessInfo)) {
            return false;
        }
        ImportDataProcessInfo other = (ImportDataProcessInfo) obj;
        return other.getTaskId() == (other.getTaskId() + 0);
        // return super.equals(obj);
    }

    public static void main(String[] args) throws Exception {
    }
}
