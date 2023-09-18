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
//package com.qlangtech.tis.dump.hive;
//
//import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
//import org.apache.hadoop.hive.ql.exec.UDFArgumentLengthException;
//import org.apache.hadoop.hive.ql.metadata.HiveException;
//import org.apache.hadoop.hive.ql.udf.generic.GenericUDTF;
//import org.apache.hadoop.hive.serde.serdeConstants;
//import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
//import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
//import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
//import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
//import org.codehaus.jackson.JsonFactory;
//import org.codehaus.jackson.JsonParser.Feature;
//import org.codehaus.jackson.map.ObjectMapper;
//import org.codehaus.jackson.map.type.TypeFactory;
//import org.codehaus.jackson.type.JavaType;
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.Map;
//import java.util.Set;
//
///* *
// * @author 百岁（baisui@qlangtech.com）
// * @date 2016年12月1日
// */
//public class JsonArrayUDTF extends GenericUDTF {
//
//    private static final JsonFactory JSON_FACTORY = new JsonFactory();
//
//    static {
//        // Allows for unescaped ASCII control characters in JSON values
//        JSON_FACTORY.enable(Feature.ALLOW_UNQUOTED_CONTROL_CHARS);
//        JSON_FACTORY.enable(Feature.ALLOW_UNQUOTED_FIELD_NAMES);
//    }
//
//    private static final ObjectMapper MAPPER = new ObjectMapper(JSON_FACTORY);
//
//    private static final JavaType MAP_TYPE = TypeFactory.arrayType(Map.class);
//
//    public static void main(String[] args) throws Exception {
//        Map<String, Object>[] arrays = (Map<String, Object>[]) MAPPER.readValue("[{a:1,b:2},]", MAP_TYPE);
//        for (Map<String, Object> r : arrays) {
//            for (Map.Entry<String, Object> entry : r.entrySet()) {
//                System.out.println(entry.getKey() + ":" + entry.getValue());
//            }
//            System.out.println("======================================");
//        }
//    }
//
//    private Map<String, Object>[] arrays;
//
//    @SuppressWarnings("all")
//    @Override
//    public StructObjectInspector initialize(ObjectInspector[] args) throws UDFArgumentException {
//        if (args.length != 1) {
//            throw new UDFArgumentLengthException("UDTFSerial takes only one argument");
//        }
//        if (!args[0].getTypeName().equals("int")) {
//            throw new UDFArgumentException("UDTFSerial only takes an integer as a parameter");
//        }
//        if (args[0].getCategory() != ObjectInspector.Category.PRIMITIVE || !args[0].getTypeName().equals(serdeConstants.STRING_TYPE_NAME)) {
//            throw new UDFArgumentException("json_tuple()'s arguments have to be string type");
//        }
//        try {
//            this.arrays = (Map<String, Object>[]) MAPPER.readValue(args[0].toString(), MAP_TYPE);
//        } catch (Exception e) {
//            throw new UDFArgumentException(e);
//        }
//        Set<String> keys = new HashSet<String>();
//        for (Map<String, Object> r : arrays) {
//            for (String key : r.keySet()) {
//                keys.add(key.toLowerCase());
//            }
//        }
//        ArrayList<String> fieldNames = new ArrayList<String>(keys.size());
//        ArrayList<ObjectInspector> fieldOIs = new ArrayList<ObjectInspector>(keys.size());
//        for (String key : keys) {
//            fieldNames.add(key);
//            fieldOIs.add(PrimitiveObjectInspectorFactory.writableStringObjectInspector);
//        }
//        return ObjectInspectorFactory.getStandardStructObjectInspector(fieldNames, fieldOIs);
//    }
//
//    @Override
//    public void process(Object[] args) throws HiveException {
//    // this.forward(o);
//    }
//
//    @Override
//    public void close() throws HiveException {
//    }
//}
