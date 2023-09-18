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

package com.qlangtech.tis.plugin.datax.hudi;

import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.writer.hdfswriter.HdfsColMeta;
import com.qlangtech.tis.config.hive.IHiveConnGetter;
import com.qlangtech.tis.datax.TimeFormat;
import com.qlangtech.tis.fs.IPath;
import com.qlangtech.tis.fs.IPathInfo;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.offline.DataxUtils;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.DataType;
import com.qlangtech.tis.plugin.ds.IColMetaGetter;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-20 11:21
 **/
public class HudiTableMeta {

    public static final String FILE_NAME = "fileName";

    public static final String KEY_SOURCE_ORDERING_FIELD = "hudiSourceOrderingField";
    public final List<IColMetaGetter> colMetas;
    private final String sourceOrderingField;
    private final String dataXName;
    private final String pkName;
    // private final String partitionpathField;
    private final int shuffleParallelism;
    private final HudiWriteTabType hudiTabType;
    private final String hudiTabName;
    private IPath tabDumpDir = null;

    public static IPath createSourceSchema(ITISFileSystem fs
            , String tabName, IPath fsSourceSchemaPath, HudiSelectedTab hudiTabMeta) {
        List<CMeta> colsMetas = hudiTabMeta.getCols();
        if (CollectionUtils.isEmpty(colsMetas)) {
            throw new IllegalStateException("colsMetas of hudiTabMeta can not be empty");
        }


        try (OutputStream schemaWriter = fs.getOutputStream(fsSourceSchemaPath)) {
            SchemaBuilder.RecordBuilder<Schema> builder = SchemaBuilder.record(tabName);
//            builder.prop("testFiled", LogicalTypes.date().addToSchema(Schema.create(Schema.Type.INT)));
            SchemaBuilder.FieldAssembler<Schema> fields = builder.fields();

            for (CMeta meta : colsMetas) {
                meta.getType().accept(new DataType.TypeVisitor<Void>() {

                    @Override
                    public Void bigInt(DataType type) {
                        if (meta.isNullable()) {
                            fields.optionalLong(meta.getName());
                        } else {
                            fields.requiredLong(meta.getName());
                        }
                        return null;
                    }

                    @Override
                    public Void doubleType(DataType type) {
                        if (meta.isNullable()) {
                            fields.optionalDouble(meta.getName());
                        } else {
                            fields.requiredDouble(meta.getName());
                        }
                        return null;
                    }

                    @Override
                    public Void dateType(DataType type) {
                        Schema schema = LogicalTypes.date().addToSchema(Schema.create(Schema.Type.INT));
                        addNullableSchema(fields, schema, meta);
                        return null;
                    }

                    @Override
                    public Void timestampType(DataType type) {
                        Schema schema = LogicalTypes.timestampMillis().addToSchema(Schema.create(Schema.Type.LONG));
                        addNullableSchema(fields, schema, meta);
                        return null;
                    }

                    @Override
                    public Void bitType(DataType type) {
                        if (meta.isNullable()) {
                            fields.optionalBoolean(meta.getName());
                        } else {
                            fields.optionalBoolean(meta.getName());
                        }
                        return null;
                    }

                    @Override
                    public Void blobType(DataType type) {
                        if (meta.isNullable()) {
                            // strType.stringDefault(StringUtils.EMPTY);
                            fields.optionalBytes(meta.getName());
                        } else {
                            //   strType.noDefault();
                            fields.requiredBytes(meta.getName());
                        }
                        return null;
                    }

                    @Override
                    public Void varcharType(DataType type) {
                        if (meta.isNullable()) {
                            // strType.stringDefault(StringUtils.EMPTY);
                            fields.optionalString(meta.getName());
                        } else {
                            //   strType.noDefault();
                            fields.requiredString(meta.getName());
                        }
                        return null;
                    }

                    @Override
                    public Void intType(DataType type) {
                        if (meta.isNullable()) {
                            fields.optionalInt(meta.getName());
                        } else {
                            fields.requiredInt(meta.getName());
                        }
                        return null;
                    }

                    @Override
                    public Void floatType(DataType type) {
                        if (meta.isNullable()) {
                            fields.optionalFloat(meta.getName());
                        } else {
                            fields.requiredFloat(meta.getName());
                        }
                        return null;
                    }

                    @Override
                    public Void decimalType(DataType type) {
                        Objects.requireNonNull(type, "dataType can not be null");
                        // hive精度有限制 HiveDecimalUtils.validateParameter
                        validateDecimalParameter(type.columnSize, type.getDecimalDigits());
                        Schema schema = LogicalTypes.decimal(
                                type.columnSize, type.getDecimalDigits()).addToSchema(Schema.create(Schema.Type.BYTES));
                        addNullableSchema(fields, schema, meta);
                        return null;
                    }

                    private void validateDecimalParameter(int precision, int scale) {
                        if (precision >= 1 && precision <= 38) {
                            if (scale >= 0 && scale <= 38) {
                                if (precision < scale) {
                                    throw new IllegalArgumentException("Decimal scale must be less than or equal to precision");
                                }
                            } else {
                                throw new IllegalArgumentException("Decimal scale out of allowed range [0,38]");
                            }
                        } else {
                            throw new IllegalArgumentException("Decimal precision out of allowed range [1,38]");
                        }
                    }

                    @Override
                    public Void timeType(DataType type) {
                        Schema schema = LogicalTypes.timeMillis().addToSchema(Schema.create(Schema.Type.INT));
                        addNullableSchema(fields, schema, meta);
                        return null;
                    }

                    @Override
                    public Void tinyIntType(DataType dataType) {
                        smallIntType(dataType);
                        return null;
                    }

                    @Override
                    public Void smallIntType(DataType dataType) {
                        if (meta.isNullable()) {
                            fields.optionalInt(meta.getName());
                        } else {
                            fields.requiredInt(meta.getName());
                        }
                        return null;
                    }
                });
            }
            Schema schema = fields.endRecord();
            if (schema.getFields().size() != colsMetas.size()) {
                throw new IllegalStateException("schema.getFields():" + schema.getFields().size() + " is not equal to 'colsMeta.size()':" + colsMetas.size());
            }
            IOUtils.write(schema.toString(true), schemaWriter, TisUTF8.get());
            schemaWriter.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return fsSourceSchemaPath;
    }

    public static IPath createFsSourceSchema(ITISFileSystem fs
            , String tabName, IPath tabDumpDir, HudiSelectedTab hudiTabMeta) {
        return createSourceSchema(fs, tabName, getTableSourceSchema(fs, tabDumpDir), hudiTabMeta);
    }

    public static IPath getTableSourceSchema(ITISFileSystem fs, IPath tabDumpDir) {
        return fs.getPath(tabDumpDir, "meta/schema.avsc");
    }

    public static IPath getTableSourceSchema(ITISFileSystem fs, IHiveConnGetter hiveConn, String tabName, String dumpTimeStamp) {
        IPath dumpDir = HudiTableMeta.getDumpDir(fs, tabName, dumpTimeStamp, hiveConn);
        return getTableSourceSchema(fs, dumpDir);
    }


    protected static void addNullableSchema(SchemaBuilder.FieldAssembler<Schema> fields, Schema schema, CMeta meta) {
        if (meta.isNullable()) {
            schema = Schema.createUnion(Schema.create(Schema.Type.NULL), schema);
        }
        fields.name(meta.getName()).type(schema).noDefault();
    }


    public boolean isColsEmpty() {
        return CollectionUtils.isEmpty(this.colMetas);
    }

    public HudiTableMeta(Configuration paramCfg) {
        this(paramCfg, paramCfg.getNecessaryValue(FILE_NAME, HudiWriterErrorCode.REQUIRED_VALUE));
    }

    public HudiTableMeta(Configuration paramCfg, String hudiTabName) {
        this.colMetas = HdfsColMeta.getColsMeta(paramCfg);
        if (this.isColsEmpty()) {
            throw new IllegalStateException("colMetas can not be null");
        }

        this.hudiTabName = hudiTabName;// paramCfg.getNecessaryValue(Key.FILE_NAME, HdfsWriterErrorCode.REQUIRED_VALUE);
        this.sourceOrderingField
                = paramCfg.getNecessaryValue(KEY_SOURCE_ORDERING_FIELD, HudiWriterErrorCode.REQUIRED_VALUE);
        this.dataXName = paramCfg.getNecessaryValue(DataxUtils.DATAX_NAME, HudiWriterErrorCode.REQUIRED_VALUE);
        this.pkName = paramCfg.getNecessaryValue("hudiRecordkey", HudiWriterErrorCode.REQUIRED_VALUE);
        //  this.partitionpathField = paramCfg.getNecessaryValue("hudiPartitionpathField", HdfsWriterErrorCode.REQUIRED_VALUE);
        this.shuffleParallelism
                = Integer.parseInt(paramCfg.getNecessaryValue("shuffleParallelism", HudiWriterErrorCode.REQUIRED_VALUE));
        this.hudiTabType = HudiWriteTabType.parse(paramCfg.getNecessaryValue("hudiTabType", HudiWriterErrorCode.REQUIRED_VALUE));
    }

//    public IPath getDumpDir(BasicHdfsWriterJob writerJob, IHiveConnGetter hiveConn) {
//        return getDumpDir(writerJob.getFileSystem(), hiveConn);
//    }

    public IPath getDumpDir(ITISFileSystem fs, IHiveConnGetter hiveConn) {
        if (this.tabDumpDir == null) {
            this.tabDumpDir = getDumpDir(fs, hudiTabName, TimeFormat.yyyyMMddHHmmss.format(DataxUtils.getDumpTimeStamp()), hiveConn);
        }
        return this.tabDumpDir;
    }

    public static IPath getDumpDir(ITISFileSystem fs, String hudiTabName, String dumpTimeStamp, IHiveConnGetter hiveConn) {
        if (StringUtils.isEmpty(hudiTabName)) {
            throw new IllegalArgumentException("param hudiTabName can not be null");
        }
        if (StringUtils.isEmpty(dumpTimeStamp)) {
            throw new IllegalArgumentException("param dumpTimeStamp can not be null");
        }
        return fs.getPath(fs.getRootDir(), hiveConn.getDbName() + "/" + dumpTimeStamp + "/" + hudiTabName);
    }

    public static List<Option> getHistoryBatchs(ITISFileSystem fs, IHiveConnGetter hiveConn) {
        IPath path = fs.getPath(fs.getRootDir(), hiveConn.getDbName());
        List<IPathInfo> child = fs.listChildren(path);
        return child.stream().map((c) -> new Option(c.getName())).collect(Collectors.toList());
    }

    public static IPath getHudiDataDir(ITISFileSystem fs, String hudiTabName, String dumpTimeStamp, IHiveConnGetter hiveConn) {
        return getHudiDataDir(fs, getDumpDir(fs, hudiTabName, dumpTimeStamp, hiveConn));
    }

    public static IPath getHudiDataDir(ITISFileSystem fs, IPath getDumpDir) {
        return fs.getPath(getDumpDir, "hudi");
    }

    public String getSourceOrderingField() {
        return sourceOrderingField;
    }

    public String getDataXName() {
        return dataXName;
    }

    public String getPkName() {
        return pkName;
    }

    public int getShuffleParallelism() {
        return shuffleParallelism;
    }

    public HudiWriteTabType getHudiTabType() {
        return hudiTabType;
    }
}
