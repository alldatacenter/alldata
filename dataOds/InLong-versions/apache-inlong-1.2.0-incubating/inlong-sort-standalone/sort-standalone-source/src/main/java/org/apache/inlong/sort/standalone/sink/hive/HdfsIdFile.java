/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.standalone.sink.hive;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

/**
 * 
 * HdfsIdFile
 */
public class HdfsIdFile {

    public static final Logger LOG = InlongLoggerFactory.getLogger(HiveSink.class);

    public static final String SUBPATH_INTMP = "/intmp/";
    public static final String SUBPATH_IN = "/in/";
    public static final String SUBPATH_OUT = "/out/";
    public static final byte SEPARATOR_MESSAGE = '\n';
    public static final String OUTTMP_FILE_POSTFIX = ".outtmp";

    private final HiveSinkContext context;
    private final HdfsIdConfig idConfig;
    private final String strIdRootPath;

    private final DistributedFileSystem fs;
    private final Path intmpPath;
    private final Path inPath;
    private final Path outPath;
    private final Path intmpFilePath;
    private final String strIntmpFile;
    private final FSDataOutputStream intmpOutput;
    private final long createTime;
    private long modifiedTime;
    private boolean isOpen = true;

    /**
     * Constructor
     * 
     * @param  context
     * @param  idConfig
     * @param  strIdRootPath
     * @throws IOException
     */
    public HdfsIdFile(HiveSinkContext context, HdfsIdConfig idConfig, String strIdRootPath) throws IOException {
        this.context = context;
        this.idConfig = idConfig;
        this.strIdRootPath = strIdRootPath;
        this.createTime = System.currentTimeMillis();
        this.modifiedTime = createTime;

        String hdfsPath = context.getHdfsPath();
        this.intmpPath = new Path(hdfsPath + strIdRootPath + SUBPATH_INTMP);
        this.fs = new DistributedFileSystem();
        fs.initialize(new Path(hdfsPath).toUri(), new Configuration());
        fs.mkdirs(intmpPath);
        this.inPath = new Path(hdfsPath + strIdRootPath + SUBPATH_IN);
        fs.mkdirs(inPath);
        this.outPath = new Path(hdfsPath + strIdRootPath + SUBPATH_OUT);
        fs.mkdirs(outPath);

        this.strIntmpFile = getFileName(context, createTime);
        this.intmpFilePath = new Path(intmpPath, strIntmpFile);
        // check if file exists
        if (fs.exists(intmpFilePath)) {
            // remove file
            fs.delete(intmpFilePath, true);
        }
        this.intmpOutput = fs.create(intmpFilePath, true);
    }

    /**
     * getFileName
     * 
     * @param  context
     * @param  fileTime
     * @return
     */
    public static String getFileName(HiveSinkContext context, long fileTime) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        return context.getNodeId() + "." + format.format(new Date(fileTime));
    }

    /**
     * close
     * 
     * @throws IOException
     */
    public void close() {
        this.isOpen = false;
        if (intmpOutput != null) {
            try {
                intmpOutput.flush();
                intmpOutput.close();
                if (intmpOutput.getPos() != 0) {
                    Path inFilePath = new Path(this.inPath, strIntmpFile);
                    fs.rename(intmpFilePath, inFilePath);
                } else {
                    fs.delete(intmpFilePath, true);
                }
                this.fs.close();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    /**
     * get modifiedTime
     * 
     * @return the modifiedTime
     */
    public long getModifiedTime() {
        return modifiedTime;
    }

    /**
     * set modifiedTime
     * 
     * @param modifiedTime the modifiedTime to set
     */
    public void setModifiedTime(long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    /**
     * get context
     * 
     * @return the context
     */
    public HiveSinkContext getContext() {
        return context;
    }

    /**
     * get idConfig
     * 
     * @return the idConfig
     */
    public HdfsIdConfig getIdConfig() {
        return idConfig;
    }

    /**
     * get strIdRootPath
     * 
     * @return the strIdRootPath
     */
    public String getStrIdRootPath() {
        return strIdRootPath;
    }

    /**
     * get intmpPath
     * 
     * @return the intmpPath
     */
    public Path getIntmpPath() {
        return intmpPath;
    }

    /**
     * get inPath
     * 
     * @return the inPath
     */
    public Path getInPath() {
        return inPath;
    }

    /**
     * get outPath
     * 
     * @return the outPath
     */
    public Path getOutPath() {
        return outPath;
    }

    /**
     * get intmpFilePath
     * 
     * @return the intmpFilePath
     */
    public Path getIntmpFilePath() {
        return intmpFilePath;
    }

    /**
     * get intmpOutput
     * 
     * @return the intmpOutput
     */
    public FSDataOutputStream getIntmpOutput() {
        return intmpOutput;
    }

    /**
     * get createTime
     * 
     * @return the createTime
     */
    public long getCreateTime() {
        return createTime;
    }

    /**
     * get fs
     * 
     * @return the fs
     */
    public DistributedFileSystem getFs() {
        return fs;
    }

    /**
     * get strIntmpFile
     * 
     * @return the strIntmpFile
     */
    public String getStrIntmpFile() {
        return strIntmpFile;
    }

    /**
     * get isOpen
     * 
     * @return the isOpen
     */
    public boolean isOpen() {
        return isOpen;
    }

}
