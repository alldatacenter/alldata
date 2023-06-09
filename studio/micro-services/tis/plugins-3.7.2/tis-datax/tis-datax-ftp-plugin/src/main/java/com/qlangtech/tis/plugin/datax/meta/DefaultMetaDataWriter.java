/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugin.datax.meta;

import com.alibaba.datax.plugin.ftp.common.FtpHelper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.datax.DataXFtpWriter;
import com.qlangtech.tis.plugin.datax.server.FTPServer;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.plugin.ds.DataType;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.trigger.util.JsonUtil;
import org.apache.commons.io.IOUtils;

import java.io.OutputStream;
import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-04-05 15:31
 **/
public class DefaultMetaDataWriter extends MetaDataWriter {

    public static final String COL_NAME = "name";
    public static final String COL_TYPE = "type";
    public static final String COL_PK = "pk";
    public static final String COL_NULLABLE = "nullable";

    public static List<ColumnMetaData> deserialize(JSONArray fields) {
        List<ColumnMetaData> cols = Lists.newArrayList();
        ColumnMetaData col = null;
        JSONObject field = null;
        for (int i = 0; i < fields.size(); i++) {
            // int index, String key, DataType type, boolean pk, boolean nullable
            field = fields.getJSONObject(i);
            col = new ColumnMetaData(i, field.getString(COL_NAME), DataType.ds(field.getString(COL_TYPE)), field.getBoolean(COL_PK), field.getBoolean(COL_NULLABLE));
            cols.add(col);
        }
        return cols;
    }

    @Override
    public String getFtpTargetDir(DataXFtpWriter writer, String tableName) {
        return writer.path + IOUtils.DIR_SEPARATOR + tableName;  //super.getFtpTargetDir(writer);
    }

    /**
     * 写入元数据
     *
     * @param ftpWriter
     * @param execContext
     * @param tab
     * @return
     */
    @Override
    public IRemoteTaskTrigger createMetaDataWriteTask(DataXFtpWriter ftpWriter, IExecChainContext execContext, ISelectedTab tab) {
        return new IRemoteTaskTrigger() {
            @Override
            public String getTaskName() {
                return tab.getName() + "_metadata_write";
            }

            @Override
            public void run() {
                FTPServer ftp = FTPServer.getServer(ftpWriter.linker);

                ftp.useFtpHelper((ftpHelper) -> {
                    String ftpDir = ftpWriter.path + IOUtils.DIR_SEPARATOR + tab.getName();

                    ftpHelper.mkDirRecursive(ftpDir);

                    try (OutputStream metaWriter
                                 = ftpHelper.getOutputStream(
                            ftpDir + IOUtils.DIR_SEPARATOR + FtpHelper.KEY_META_FILE, false)) {

                        JSONArray fields = new JSONArray();
                        JSONObject field = null;
                        for (CMeta c : tab.getCols()) {
                            field = new JSONObject();
                            field.put(COL_NAME, c.getName());
                            field.put(COL_TYPE, c.getType().getS());
                            field.put(COL_PK, c.isPk());
                            field.put(COL_NULLABLE, c.isNullable());
                            fields.add(field);
                        }
                        IOUtils.write(JsonUtil.toString(fields), metaWriter, TisUTF8.get());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return null;
                });

            }
        };
    }

    @TISExtension
    public static class DftDesc extends Descriptor<MetaDataWriter> {
        @Override
        public String getDisplayName() {
            return SWITCH_ON;
        }
    }
}
