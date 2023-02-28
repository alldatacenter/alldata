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

package com.qlangtech.plugins.incr.flink.chunjun.doris.table;

import com.qlangtech.tis.datax.TableAlias;
import com.qlangtech.tis.offline.DataxUtils;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.factories.DynamicTableSinkFactory;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-11-13 11:48
 **/
public class TISDorisDynamicTableFactory implements DynamicTableSinkFactory {
    // public static final String IDENTIFIER = ChunjunSqlType.getTableSinkTypeName(IEndTypeGetter.EndType.Doris); //"tis-doris-x";


    @Override
    public DynamicTableSink createDynamicTableSink(Context context) {
//        final FactoryUtil.TableFactoryHelper helper =
//                FactoryUtil.createTableFactoryHelper(this, context);
//        // 1.所有的requiredOptions和optionalOptions参数
//        final ReadableConfig config = helper.getOptions();
//        // 2.参数校验
//        helper.validate();
//        String dataXName = config.get(DATAX_NAME);
//        String sourceTableName = config.get(SourceTableName);
//
//        ChunjunSinkFactory.CreateChunjunSinkFunctionResult sinkFunctionResult
//                = createChunjunSinkFunctionResult(dataXName, sourceTableName);
//
//        // 3.封装参数
//        TableSchema physicalSchema =
//                TableSchemaUtils.getPhysicalSchema(context.getCatalogTable().getSchema());
//        return new TISDorisDynamicTableSink(physicalSchema, sinkFunctionResult.getSinkFunction(), sinkFunctionResult.getParallelism());
        return null;
    }

//    public static ChunjunSinkFactory.CreateChunjunSinkFunctionResult
//    createChunjunSinkFunctionResult(String dataXName, String sourceTableName) {
//        ChunjunSinkFactory chujunSinKFactory
//                = (ChunjunSinkFactory) TISSinkFactory.getIncrSinKFactory(dataXName);
//
//        DataxProcessor processor = DataxProcessor.load(null, dataXName);
//        IDataxReader reader = processor.getReader(null);
//        List<ISelectedTab> tabs = reader.getSelectedTabs();
//
//        Optional<ISelectedTab> stab
//                = tabs.stream().filter((t) -> sourceTableName.equals(t.getName())).findFirst();
//        if (!stab.isPresent()) {
//            throw new IllegalStateException("sourceTabName:" + sourceTableName + " can not find relevant 'ISelectedTab'");
//        }
//
//        TableAliasMapper tabAlias = processor.getTabAlias();
//        TableAlias tableAlias = tabAlias.getWithCheckNotNull(sourceTableName);
//
//        return chujunSinKFactory.createSinFunctionResult(processor
//                , (SelectedTab) stab.get(), tableAlias.getTo(), false);
//    }

    @Override
    public String factoryIdentifier() {
        //  return IDENTIFIER;
        return null;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        Set<ConfigOption<?>> requiredOptions =
                Stream.of(DATAX_NAME, SourceTableName)
                        .collect(Collectors.toSet());
        return requiredOptions;
    }

    public static final ConfigOption<String> DATAX_NAME =
            ConfigOptions.key(DataxUtils.DATAX_NAME)
                    .stringType()
                    .noDefaultValue()
                    .withDescription("YOUR DORIS " + DataxUtils.DATAX_NAME);

    public static final ConfigOption<String> SourceTableName =
            ConfigOptions.key(TableAlias.KEY_FROM_TABLE_NAME)
                    .stringType()
                    .noDefaultValue()
                    .withDescription(TableAlias.KEY_FROM_TABLE_NAME);

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        return requiredOptions();
    }
}
