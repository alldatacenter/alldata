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

package com.alibaba.datax.plugin.writer.hudi;

import com.alibaba.datax.common.element.Column;
import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.plugin.TaskPluginCollector;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.writer.hdfswriter.HdfsColMeta;
import com.alibaba.datax.plugin.writer.hdfswriter.HdfsHelper;
import com.alibaba.datax.plugin.writer.hdfswriter.HdfsWriter;
import com.alibaba.datax.plugin.writer.hdfswriter.HdfsWriterErrorCode;
import com.qlangtech.tis.config.hive.IHiveConnGetter;
import com.qlangtech.tis.fs.IPath;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.plugin.datax.BasicDataXHdfsWriter;
import com.qlangtech.tis.plugin.datax.hudi.DataXHudiWriter;
import com.qlangtech.tis.plugin.datax.hudi.HudiDumpPostTask;
import com.qlangtech.tis.plugin.datax.hudi.HudiTableMeta;
import com.qlangtech.tis.plugin.ds.IColMetaGetter;
import org.apache.avro.Conversions;
import org.apache.avro.Schema;
import org.apache.avro.data.TimeConversions;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.collections.CollectionUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.sql.Types;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-01-22 15:16
 **/
public class TisDataXHudiWriter extends HdfsWriter {

    private static final Logger logger = LoggerFactory.getLogger(TisDataXHudiWriter.class);
    public static final String KEY_HUDI_TABLE_NAME = "hudiTableName";

    public static class Job extends BasicDataXHdfsWriter.Job {

        private HudiTableMeta tabMeta;

        private DataXHudiWriter writerPlugin;
        // private IPath tabDumpDir;
        private IPath rootDir;

        @Override
        public List<Configuration> split(int mandatoryNumber) {
            final String tabName = this.cfg.getNecessaryValue(
                    com.alibaba.datax.plugin.unstructuredstorage.writer.Key.FILE_NAME, HdfsWriterErrorCode.REQUIRED_VALUE);
            List<Configuration> cfgs = super.split(mandatoryNumber);
            for (Configuration cfg : cfgs) {
                cfg.set(KEY_HUDI_TABLE_NAME, tabName);
            }
            return cfgs;
        }


        @Override
        public void init() {
            getPluginJobConf();
            this.tabMeta = new HudiTableMeta(this.cfg);
            super.init();

        }

        @Override
        public void prepare() {
            super.prepare();

        }

        protected IPath getRootPath() {
            if (rootDir == null) {
                ITISFileSystem fs = this.getFileSystem();
                Objects.requireNonNull(fs, "fileSystem can not be null");
                rootDir = fs.getRootDir();
            }
            return rootDir;
        }

        private DataXHudiWriter getHudiWriterPlugin() {
            if (this.writerPlugin == null) {
                this.writerPlugin = (DataXHudiWriter) this.getWriterPlugin();
            }
            return this.writerPlugin;
        }

        private IHiveConnGetter getHiveConnGetter() {
            return getHudiWriterPlugin().getHiveConnMeta();
        }

        protected Path createTabDumpParentPath(ITISFileSystem fs) {
            return HudiDumpPostTask.createTabDumpParentPath(fs, getDumpDir()).unwrap(Path.class);
        }

        @Override
        public void post() {
            super.post();
        }


        protected IPath getDumpDir() {

            return this.tabMeta.getDumpDir(this.getFileSystem(), this.getHiveConnGetter());
        }
    }


    public static class Task extends BasicDataXHdfsWriter.Task {
        private Schema avroSchema;
        private List<IColMetaGetter> colsMeta;
        private HudiTableMeta tabMeta;

        private DataFileWriter<GenericRecord> dataFileWriter;

        @Override
        public void init() {
            super.init();
            this.tabMeta = new HudiTableMeta(this.writerSliceConfig
                    , this.writerSliceConfig.getNecessaryValue(KEY_HUDI_TABLE_NAME, HdfsWriterErrorCode.REQUIRED_VALUE));
            this.avroSchema = this.getAvroSchema();
        }

        protected Schema getAvroSchema() {
            ITISFileSystem fs = writerPlugin.getFsFactory().getFileSystem();
            IPath tabSourceSchema = HudiTableMeta.getTableSourceSchema(
                    fs, this.tabMeta.getDumpDir(fs, ((DataXHudiWriter) writerPlugin).getHiveConnMeta()));
            try (InputStream reader = fs.open(tabSourceSchema)) {
                Objects.requireNonNull(reader, "schema reader can not be null");
                return new Schema.Parser().parse(reader);
            } catch (Exception e) {
                throw new RuntimeException(String.valueOf(tabSourceSchema), e);
            }
        }

        @Override
        public void prepare() {
            super.prepare();
            this.colsMeta = HdfsColMeta.getColsMeta(this.writerSliceConfig);
            if (CollectionUtils.isEmpty(this.colsMeta)) {
                throw new IllegalStateException("colsMeta can not be empty");
            }
        }

        @Override
        protected void orcFileStartWrite(FileSystem fileSystem, JobConf conf
                , RecordReceiver lineReceiver, Configuration config, String fileName, TaskPluginCollector taskPluginCollector) {
            //  FileFormatUtils.orcFileStartWrite(fileSystem, conf, lineReceiver, config, fileName, taskPluginCollector);
            throw new UnsupportedOperationException();
        }

        @Override
        protected void startTextWrite(HdfsHelper fsHelper, RecordReceiver lineReceiver
                , Configuration config, String fileName, TaskPluginCollector taskPluginCollector) {
            //  TextFileUtils.startTextWrite(fsHelper, lineReceiver, config, fileName, taskPluginCollector);
            throw new UnsupportedOperationException();
        }


        @Override
        protected void avroFileStartWrite(RecordReceiver lineReceiver
                , Configuration config, String fileName, TaskPluginCollector taskPluginCollector) {
            try {
                Path targetPath = new Path(config.getString(com.alibaba.datax.plugin.unstructuredstorage.writer.Key.FILE_NAME));
                GenericDatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(this.avroSchema);
                datumWriter.getData().addLogicalTypeConversion(new Conversions.DecimalConversion());
                datumWriter.getData().addLogicalTypeConversion(new TimeConversions.DateConversion());
                datumWriter.getData().addLogicalTypeConversion(new TimeConversions.TimeMillisConversion());
                datumWriter.getData().addLogicalTypeConversion(new TimeConversions.TimestampMillisConversion());
                try (OutputStream output = getOutputStream(targetPath)) {
                    dataFileWriter = new DataFileWriter<>(datumWriter);
                    dataFileWriter = dataFileWriter.create(this.avroSchema, output);
                    Record record = null;
                    while ((record = lineReceiver.getFromReader()) != null) {
                        dataFileWriter.append(convertAvroRecord(record));
                    }
                    dataFileWriter.flush();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void post() {

        }

        private GenericRecord convertAvroRecord(Record record) {
            GenericRecord r = new GenericData.Record(this.avroSchema);
            int i = 0;
            Column column = null;
            for (IColMetaGetter meta : colsMeta) {
                column = record.getColumn(i++);
                if (column.getRawData() == null) {
                    r.put(meta.getName(), null);
                    continue;
                }
                r.put(meta.getName(), parseAvroVal(meta, column));
            }

            return r;
        }

        private Object parseAvroVal(IColMetaGetter meta, Column colVal) {
            switch (meta.getType().type) {
                case Types.TINYINT:
                case Types.INTEGER:
                case Types.SMALLINT:
                    return colVal.asBigInteger().intValue();
                case Types.BIGINT:
                    return colVal.asBigInteger().longValue();
                case Types.FLOAT:
                case Types.DOUBLE:
                    return colVal.asDouble();
                case Types.DECIMAL:
                    return colVal.asBigDecimal();
                case Types.DATE:
                    return colVal.asDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                case Types.TIME:
                    return colVal.asDate().toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
                case Types.TIMESTAMP:
                    return colVal.asDate().toInstant();
                case Types.BIT:
                case Types.BOOLEAN:
                    return colVal.asBoolean();
                case Types.BLOB:
                case Types.BINARY:
                case Types.LONGVARBINARY:
                case Types.VARBINARY:
                    return ByteBuffer.wrap(colVal.asBytes());
                case Types.VARCHAR:
                case Types.LONGNVARCHAR:
                case Types.NVARCHAR:
                case Types.LONGVARCHAR:
                    // return visitor.varcharType(this);
                default:
                    return colVal.asString();// "VARCHAR(" + type.columnSize + ")";
            }
        }


        @Override
        protected void csvFileStartWrite(
                RecordReceiver lineReceiver, Configuration config
                , String fileName, TaskPluginCollector taskPluginCollector) {
            throw new UnsupportedOperationException();
        }

        protected OutputStream getOutputStream(Path targetPath) {
            return this.hdfsHelper.getOutputStream(targetPath);
        }
    }
}

