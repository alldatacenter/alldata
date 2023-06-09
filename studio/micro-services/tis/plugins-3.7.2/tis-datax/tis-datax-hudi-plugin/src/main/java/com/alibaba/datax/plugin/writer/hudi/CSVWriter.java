///**
// *   Licensed to the Apache Software Foundation (ASF) under one
// *   or more contributor license agreements.  See the NOTICE file
// *   distributed with this work for additional information
// *   regarding copyright ownership.  The ASF licenses this file
// *   to you under the Apache License, Version 2.0 (the
// *   "License"); you may not use this file except in compliance
// *   with the License.  You may obtain a copy of the License at
// *
// *       http://www.apache.org/licenses/LICENSE-2.0
// *
// *   Unless required by applicable law or agreed to in writing, software
// *   distributed under the License is distributed on an "AS IS" BASIS,
// *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *   See the License for the specific language governing permissions and
// *   limitations under the License.
// */
//
//package com.alibaba.datax.plugin.writer.hudi;
//
//import com.alibaba.datax.common.element.Column;
//import com.fasterxml.jackson.databind.*;
//import com.fasterxml.jackson.databind.cfg.MapperConfig;
//import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
//import com.fasterxml.jackson.databind.introspect.BasicBeanDescription;
//import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
//import com.fasterxml.jackson.dataformat.csv.CsvMapper;
//import com.fasterxml.jackson.dataformat.csv.CsvSchema;
//
//import java.io.PrintStream;
//import java.util.Collections;
//import java.util.List;
//
///**
// * 参考： org.apache.hudi.utilities.testutils.UtilitiesTestBase.saveCsvToDFS
// *
// * @See com.fasterxml.jackson.databind.ser.std.MapSerializer
// * @author: 百岁（baisui@qlangtech.com）
// * @create: 2022-01-22 14:17
// **/
//public class CSVWriter {
//    public static final char CSV_Column_Separator = ',';
//    public static final String CSV_NULL_VALUE = "null";
//    public static final char CSV_ESCAPE_CHAR = '"';
//    public static final boolean CSV_FILE_USE_HEADER = true;
//
//    public static void main(String[] args) throws Exception {
//        CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();
//        csvSchemaBuilder.addColumn("name",   CsvSchema.ColumnType.STRING);
//        csvSchemaBuilder.addColumn("hobbie", CsvSchema.ColumnType.STRING);
//        csvSchemaBuilder.addColumn("age",    CsvSchema.ColumnType.NUMBER);
//
//        ObjectWriter csvObjWriter = new CsvMapper() {
////            @Override
////            protected ClassIntrospector defaultClassIntrospector() {
////                return new BasicClassIntrospector() {
////                    @Override
////                    protected BasicBeanDescription _findStdJdkCollectionDesc(MapperConfig<?> cfg, JavaType type, MixInResolver r) {
////                        boolean useAnnotations = cfg.isAnnotationProcessingEnabled();
////                        AnnotatedClass ac = AnnotatedClass.construct(type.getRawClass(),
////                                (useAnnotations ? cfg.getAnnotationIntrospector() : null), r);
////
////                        return new TisBasicBeanDescription(cfg, type,
////                                ac, Collections.<BeanPropertyDefinition>emptyList());
////                    }
////                };
////            }
//        }
////                .setMixInResolver(new ClassIntrospector.MixInResolver() {
////                    @Override
////                    public Class<?> findMixInClassFor(Class<?> cls) {
////                        return Map.class;
////                    }
////
////                    @Override
////                    public ClassIntrospector.MixInResolver copy() {
////                        return this;
////                    }
////                })
//                .setSerializerFactory(new TISSerializerFactory(Collections.emptyList()))
//                // .writerFor(Map.class)
//                .writerFor(com.alibaba.datax.common.element.Record.class)
//                .with(csvSchemaBuilder.setUseHeader(true).setColumnSeparator(',').build());
//
////        Map<String, String> row = Maps.newHashMap();
////        row.put("name", "baisui");
////        row.put("age", "111");
//
//        //  List<User> ulist = Lists.newArrayList();
//
//        PrintStream os = new PrintStream(System.out);
//        SequenceWriter sequenceWriter = csvObjWriter.writeValues(os);//.writeValue(os, ulist);
//        for (int i = 0; i < 1000; i++) {
//            DftRecord row = new DftRecord();
////            Map row = Maps.newHashMap();
////            row.put("name", "ba,isui" + i);
////            row.put("age",  11);
//            //    ulist.add(row);
//
//            sequenceWriter.write(row);
//            Thread.sleep(200);
//        }
//        os.flush();
//
////        os.flush();
////        os.close();
//
//    }
//
//
//    public static class DftRecord implements com.alibaba.datax.common.element.Record {
//        @Override
//        public void addColumn(Column column) {
//
//        }
//
//        @Override
//        public void setColumn(int i, Column column) {
//
//        }
//
//        @Override
//        public Column getColumn(int i) {
//            return null;
//        }
//
//        @Override
//        public int getColumnNumber() {
//            return 0;
//        }
//
//        @Override
//        public int getByteSize() {
//            return 0;
//        }
//
//        @Override
//        public int getMemorySize() {
//            return 0;
//        }
//    }
//
//
//    public static class User {
//        private String name;
//        private int age;
//        private String hobbie;
//
//        public String getName() {
//            return name;
//        }
//
//        public String getHobbie() {
//            return hobbie;
//        }
//
//        public void setHobbie(String hobbie) {
//            this.hobbie = hobbie;
//        }
//
//        public void setName(String name) {
//            this.name = name;
//        }
//
//        public int getAge() {
//            return age;
//        }
//
//        public void setAge(int age) {
//            this.age = age;
//        }
//    }
//
//
//    public static class TisBasicBeanDescription extends BasicBeanDescription {
//
//        protected TisBasicBeanDescription(MapperConfig<?> config, JavaType type
//                , AnnotatedClass classDef, List<BeanPropertyDefinition> props) {
//            super(config, type, classDef, props);
//        }
//    }
//}
