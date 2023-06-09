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
package com.qlangtech.tis.solrdao;

import com.qlangtech.tis.exec.IIndexMetaData;
import com.qlangtech.tis.solrdao.pojo.PSchemaField;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-6-7
 */
public class SolrPojoBuilder {

    final IBuilderContext buildContext;

    public SolrPojoBuilder(IBuilderContext buildContext) {
        super();
        this.buildContext = buildContext;
    }

    public void create() throws Exception {
        PrintWriter writer = null;
        InputStream reader = null;
        try {
            writer = new PrintWriter((buildContext.getOutputStream()));
            IIndexMetaData metaData = SolrFieldsParser.parse(() -> buildContext.getResourceInputStream(), ISchemaPluginContext.NULL);
            ArrayList<PSchemaField> fields = metaData.getSchemaParseResult().dFields;
            writer.print("package ");
            writer.println(this.buildContext.getTargetNameSpace() + ";");
            writer.println();
            writer.println();
            if (SolrFieldsParser.hasMultiValuedField(fields)) {
                writer.println("import java.util.List;");
            }
            writer.println("import org.apache.solr.client.solrj.beans.Field;");
            writer.println();
            writer.println("public class " + this.buildContext.getPojoName() + "{");
            for (PSchemaField f : fields) {
                if (f.isStored()) {
                    writer.println("\t@Field(\"" + f.getName() + "\")");
                    writer.println("\tprivate " + f.getFileTypeLiteria() + " " + f.getPropertyName() + ";");
                }
                writer.println();
                // System.out.println("name:" + f.getName() + ", type:"
                // + f.getPropertyName() + ", get:"
                // + f.getGetterMethodName() + " set:"
                // + f.getSetMethodName());
            }
            // 生成get set方法
            for (PSchemaField f : fields) {
                // setter mehod
                if (!f.isStored()) {
                    continue;
                }
                writer.println("\tpublic void " + f.getSetMethodName() + "(" + f.getFileTypeLiteria() + " " + f.getPropertyName() + "){");
                writer.println("\t   this." + f.getPropertyName() + " = " + f.getPropertyName() + ";");
                writer.println("\t}");
                writer.println();
                // getter mehod
                writer.println("\tpublic " + f.getFileTypeLiteria() + " " + f.getGetterMethodName() + "(){");
                writer.println("\t   return this." + f.getPropertyName() + ";");
                writer.println("\t}");
                writer.println();
                // writer.println("\t@Field(\"" + f.getName() + "\")");
                // writer.println("\tprivate " + f.getFileTypeLiteria() +
                // " "
                // + f.getPropertyName() + ";");
                // writer.println();
                // System.out.println("name:" + f.getName() + ", type:"
                // + f.getPropertyName() + ", get:"
                // + f.getGetterMethodName() + " set:"
                // + f.getSetMethodName());
            }
            writer.println("}");
            // File targetFile = getNewFileName();
            System.out.println(" successful create new pojo file  ");
        } finally {
            IOUtils.closeQuietly(reader);
            writer.flush();
            buildContext.closeWriter(writer);
        }
    }

    private static class Assert {

        private static void assertNotNull(String msg, Object o) {
            if (o == null) {
                throw new NullPointerException(msg);
            }
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        String serverAddress = System.getProperty("server_address");
        String indexName = System.getProperty("index_name");
        String pojoName = System.getProperty("pojo_name");
        String pkg = System.getProperty("pkg");
        String outdir = System.getProperty("outdir");
        Assert.assertNotNull("serverAddress", serverAddress);
        Assert.assertNotNull("indexName", indexName);
        Assert.assertNotNull("pojoName", pojoName);
        Assert.assertNotNull("pkg", pkg);
        Assert.assertNotNull("outdir", outdir);
        BuilderContext buildContext = new BuilderContext();
        buildContext.setServerAddress(serverAddress);
        buildContext.setAppName(indexName);
        buildContext.setPojoName(pojoName);
        buildContext.setTargetNameSpace(pkg);
        buildContext.setTargetDir(outdir);
        SolrPojoBuilder builder = new SolrPojoBuilder(buildContext);
        builder.create();
    }
}
