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

package com.qlangtech.tis.plugins.datax.kafka.reader;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Sets;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.manage.common.ConfigFileContext;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.trigger.util.JsonUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-03-15 20:09
 **/
public class Conver2TISProps {

    private static final String PROP_DESCRIBLE_PREFIX = "Kafka";

    public static void main(String[] args) throws Exception {

//        JSONObject spec = HttpUtils.processContent(new URL("https://raw.githubusercontent.com/airbytehq/airbyte/master/airbyte-integrations/connectors/destination-rabbitmq/destination_rabbitmq/spec.json"),
//                new ConfigFileContext.StreamProcess<JSONObject>() {
//                    @Override
//                    public JSONObject p(int status, InputStream stream, Map headerFields) {
//                        try {
//                            return JSON.parseObject(org.apache.commons.io.IOUtils.toString(stream, TisUTF8.get()));
//                        } catch (IOException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
//                }
//        );

        JSONObject spec = IOUtils.loadResourceFromClasspath(Conver2TISProps.class
                , "/source_spec.json", true, (res) -> {
                    return JSON.parseObject(org.apache.commons.io.IOUtils.toString(res, TisUTF8.get()));
                });

        JSONObject connectionSpecification = spec.getJSONObject("connectionSpecification");

        String pkg = Conver2TISProps.class.getPackage().getName();

        String parentPkgName = StringUtils.substringBeforeLast(pkg, ".");
        String subPkgName = StringUtils.substringAfterLast(pkg, ".");

        CreateDescribleFile describleFile = new CreateDescribleFile(parentPkgName
                , "Test", subPkgName);
        FormBuilder formBuilder = parseFormProps(Optional.of(describleFile), connectionSpecification);


        System.out.println(formBuilder);

    }

//    private static FormBuilder parseFormProps(JSONObject formSpecification) throws IOException {
//        return parseFormProps(Optional.empty(), formSpecification);
//    }

    private static FormBuilder parseFormProps(Optional<CreateDescribleFile> createDescribleFile, JSONObject formSpecification) throws IOException {
        List<Object> required = formSpecification.getJSONArray("required");

        JSONObject properties = formSpecification.getJSONObject("properties");

        JSONObject prop = null;
        String type = null;
        String fieldName = null;

        FormBuilder formBuilder = new FormBuilder(required == null ? Collections.emptyList() : required);
        JSONObject propsMetas = new JSONObject();
        // JSONObject fieldMeta = null;

        CreateDescribleFile describleFileRule = null;
        if (createDescribleFile.isPresent()) {
            describleFileRule = createDescribleFile.get();
            String displayName = StringUtils.replace(formSpecification.getString("title"), " ", "_");
            String clazzName = PROP_DESCRIBLE_PREFIX + StringUtils.capitalize(normalizeExecuteMethod(StringUtils.lowerCase(displayName)));
            describleFileRule.setClazzName(clazzName);
            describleFileRule.setDisplayName(displayName);
            formBuilder.append("package ").append(describleFileRule.pkgName).append(";\n\n");

            formBuilder.append("import com.qlangtech.tis.extension.TISExtension;\n");
            formBuilder.append("import com.qlangtech.tis.plugin.annotation.FormField;\n");
            formBuilder.append("import com.qlangtech.tis.plugin.annotation.FormFieldType;\n");
            formBuilder.append("import com.qlangtech.tis.plugin.annotation.Validator;\n");
            formBuilder.append("import com.qlangtech.tis.extension.Descriptor;\n");


            formBuilder.append("public class ").append(clazzName).append(" extends ").append(describleFileRule.parentClass).append("{\n\n");
        }
        JSONObject fileMeta = null;
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            System.out.println(entry.getKey());
            fieldName = entry.getKey();
            prop = (JSONObject) entry.getValue();

            try {
                fileMeta = createFileMeta(prop);
                propsMetas.put(normalizeExecuteMethod(fieldName), fileMeta);
            } catch (Exception e) {
                throw new RuntimeException("field Key:" + entry.getKey(), e);
            }

            // FormFieldType.ENUM

            switch (type = StringUtils.trimToEmpty(prop.getString("type"))) {


                case "integer":

                    formBuilder.appendProp(fieldName, "INT_NUMBER", "Integer", Validator.integer);
                    // order = writePropAnnotation(required, fieldName, buffer, order);
                    break;
                case StringUtils.EMPTY:
                case "string":
                    final String[] annType = new String[]{"INPUTTEXT"};
                    processEnums(prop, (enums) -> {
                        annType[0] = "ENUM";
                    });
                    formBuilder.appendProp(fieldName, annType[0], "String");
                    break;
                case "object":
                    String parentPkg = Conver2TISProps.class.getPackage().getName();

                    StringBuffer dClazz = new StringBuffer("package ");
                    String title = prop.getString("title");
                    String subPkgName = StringUtils.lowerCase(title);
                    dClazz.append(parentPkg).append(".").append(subPkgName).append(";\n\n");
                    dClazz.append("import com.qlangtech.tis.extension.Describable;\n\n");

                    final String descClassName = PROP_DESCRIBLE_PREFIX + StringUtils.capitalize(title);
                    dClazz.append("public abstract class ").append(descClassName).append(" implements Describable<").append(descClassName).append("> {");
                    dClazz.append("}");
                    writeClassFile(parentPkg, dClazz, null, subPkgName, descClassName);
                    // 生成实现类

                    JSONArray oneOf = prop.getJSONArray("oneOf");
                    for (Object sub : oneOf) {

                        JSONObject subDescrible = (JSONObject) sub;
                        // System.out.println("-------------------------------------");
                        parseFormProps(Optional.of(new CreateDescribleFile(parentPkg, descClassName, subPkgName)), subDescrible);
                        // System.out.println("-------------------------------------");
                    }
                    formBuilder.appendProp(fieldName, null, descClassName);
                    break;
                case "boolean":
                    formBuilder.appendProp(fieldName, "ENUM", "Boolean");

                    JSONObject bool = null;
                    JSONArray boolEnum = new JSONArray();
                    bool = new JSONObject();
                    bool.put("label", "是");
                    bool.put("val", true);
                    boolEnum.add(bool);
                    bool = new JSONObject();
                    bool.put("label", "否");
                    bool.put("val", false);
                    boolEnum.add(bool);
                    fileMeta.put("enum", boolEnum);

                    break;
                default:
                    throw new IllegalStateException("type:" + type);
            }
        }


        if (createDescribleFile.isPresent()) {

            formBuilder.append("@TISExtension\n");
            formBuilder.append("public static class DefaultDescriptor extends Descriptor<").append(describleFileRule.parentClass).append("> {");
            formBuilder.append("@Override\n");
            formBuilder.append(" public String getDisplayName() {\n");
            formBuilder.append("\t\t return \"").append(describleFileRule.displayName).append("\";\n");
            formBuilder.append(" }\n");
            formBuilder.append("}");


            formBuilder.append("\n}");
            writeClassFile(describleFileRule.pkgName, formBuilder.buffer, propsMetas, describleFileRule.subPkgName, describleFileRule.clazzName);
        }

        return formBuilder;
    }

    private static JSONObject createFileMeta(JSONObject prop) {
        JSONObject fieldMeta = new JSONObject();
        fieldMeta.put("label", prop.getString("title"));
        fieldMeta.put("help", prop.getString("description"));

        Object examples = prop.get("examples");
        if (examples != null) {
            if (examples instanceof List) {
                List<Object> example = (List<Object>) examples;
                if (CollectionUtils.isNotEmpty(example)) {
                    fieldMeta.put("placeholder", String.join(" ", example.stream().map((e) -> String.valueOf(e)).collect(Collectors.toList())));
                }
            } else {
                fieldMeta.put("placeholder", examples);
            }
        }

        Object dft = null;
        if ((dft = prop.get("default")) != null || (dft = prop.get("const")) != null) {
            fieldMeta.put("dftVal", dft);
        }

        processEnums(prop, (enums) -> {
            JSONArray propEnum = new JSONArray();
            enums.forEach((e) -> {
                JSONObject option = new JSONObject();
                option.put("label", String.valueOf(e));
                option.put("val", String.valueOf(e));
                propEnum.add(option);
            });
            fieldMeta.put("enum", propEnum);
        });

//        JSONArray enums = prop.getJSONArray("enum");
//        if (enums != null && enums.size() > 1) {
//            JSONArray propEnum = new JSONArray();
//            enums.forEach((e) -> {
//                JSONObject option = new JSONObject();
//                option.put("label", String.valueOf(e));
//                option.put("val", String.valueOf(e));
//                propEnum.add(option);
//            });
//            fieldMeta.put("enum", propEnum);
//        }


        return fieldMeta;
    }

    private static void processEnums(JSONObject prop, Consumer<JSONArray> consumer) {
        JSONArray enums = prop.getJSONArray("enum");
        if (enums != null && enums.size() > 1) {
            consumer.accept(enums);
        }
    }

    private static void writeClassFile(String parentPkg, StringBuffer dClazz, JSONObject propsMetas, String subPkgName, String descClassName) throws IOException {
        if (StringUtils.isEmpty(descClassName)) {
            throw new IllegalArgumentException("param descClassName can not be null");
        }
        File dClassFile = new File("./dist/src/main/java"
                , StringUtils.replace(parentPkg, ".", "/") + "/" + (subPkgName) + "/" + descClassName + ".java");


        FileUtils.write(dClassFile, dClazz.toString(), TisUTF8.get());

        if (propsMetas != null) {
            File propsMetasFile = new File("./dist/src/main/resources"
                    , StringUtils.replace(parentPkg, ".", "/") + "/" + (subPkgName) + "/" + descClassName + ".json");
            FileUtils.write(propsMetasFile, JsonUtil.toString(propsMetas), TisUTF8.get());
        }

    }

    private static class FormBuilder {
        StringBuffer buffer = new StringBuffer();
        private final List<Object> requiredKeys;
        int order = 0;

        public FormBuilder(List<Object> requiredKeys) {
            this.requiredKeys = requiredKeys;
        }

        private void appendProp(String fieldName, String annType, String fieldType, Validator... validators) {
            buffer.append("@FormField(ordinal = " + (order++) + " "
                    + ((annType != null) ? ", type = FormFieldType." + annType : StringUtils.EMPTY)
                    + ", validate = {");
            Set<Validator> validateRules = Sets.newHashSet();
            if (requiredKeys.contains(fieldName)) {
                validateRules.add(Validator.require);
            }
            for (Validator v : validators) {
                validateRules.add(v);
            }
            buffer.append(validateRules.stream().map((v) -> "Validator." + v.name()).collect(Collectors.joining(",")));

            buffer.append("})\n");
            buffer.append("public ").append((fieldType)).append(" ").append(normalizeExecuteMethod(fieldName)).append(";\n\n");
            // return order;
        }

        @Override
        public String toString() {
            return buffer.toString();
        }

        public FormBuilder append(String val) {
            this.buffer.append(val);
            return this;
        }
    }

    private static class CreateDescribleFile {
        public final String pkgName;
        public final String parentClass;

        final String subPkgName;

        public CreateDescribleFile(String pkgName, String parentClass, String subPkgName) {
            this.pkgName = pkgName;
            this.parentClass = parentClass;
            this.subPkgName = subPkgName;
        }

        String clazzName;
        private String displayName;

        public void setClazzName(String clazzName) {
            this.clazzName = clazzName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }


    /**
     * 将 event_submit_do_buildjob_by_server 解析成 doBuildjobByServer
     *
     * @param param
     * @return
     */
    private static String normalizeExecuteMethod(String param) {
        char[] pc = Arrays.copyOfRange(param.toCharArray(), 0, param.length());
        return trimUnderline(pc).toString();
    }

    private static StringBuffer trimUnderline(char[] pc) {
        boolean underline = false;
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < pc.length; i++) {
            if ('_' != pc[i]) {
                result.append(underline ? Character.toUpperCase(pc[i]) : pc[i]);
                underline = false;
            } else {
                underline = true;
            }
        }
        return result;
    }

}
