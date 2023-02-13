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
import com.qlangtech.tis.exec.lifecycle.hook.IIndexBuildLifeCycleHook;
import com.qlangtech.tis.exec.lifecycle.hook.impl.AdapterIndexBuildLifeCycleHook;
import com.qlangtech.tis.fullbuild.indexbuild.LuceneVersion;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.ConfigFileContext;
import com.qlangtech.tis.manage.common.ConfigFileContext.StreamProcess;
import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.solr.common.DOMUtil;
import com.qlangtech.tis.solrdao.extend.IndexBuildHook;
import com.qlangtech.tis.solrdao.extend.ProcessorSchemaField;
import com.qlangtech.tis.solrdao.impl.ParseResult;
import com.qlangtech.tis.solrdao.pojo.PSchemaField;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-6-6
 */
public class SolrFieldsParser {
    public static final String KEY_PLUGIN = "plugin";
    static final XPathFactory xpathFactory = XPathFactory.newInstance();

    public static IFieldTypesVisit fieldTypeVisitor = (typeNodes) -> {
    };

    private static final DocumentBuilder solrConfigDocumentbuilder;

    private static SolrFieldsParser solrFieldsParser = new SolrFieldsParser();

    static {
        try {
            DocumentBuilderFactory solrConfigBuilderFactory = DocumentBuilderFactory.newInstance();
            solrConfigBuilderFactory.setValidating(false);
            solrConfigDocumentbuilder = solrConfigBuilderFactory.newDocumentBuilder();
            solrConfigDocumentbuilder.setEntityResolver(new EntityResolver() {

                public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                    InputSource source = new InputSource();
                    source.setCharacterStream(new StringReader(""));
                    return source;
                }
            });
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private SolrFieldsParser() {
    }

    public static IIndexMetaData parse(ISolrConfigGetter configGetter) throws Exception {
        return parse(configGetter, (fieldType) -> false);
    }

    public static IIndexMetaData parse(ISolrConfigGetter configGetter, ISchemaFieldTypeContext schemaPlugin) throws Exception {
        return parse(configGetter, schemaPlugin, false);
    }

    public static IIndexMetaData parse(ISolrConfigGetter configGetter, ISchemaFieldTypeContext schemaPlugin, boolean validateSchema) throws Exception {
        ParseResult schemaParseResult;
        try (ByteArrayInputStream reader = new ByteArrayInputStream(configGetter.getSchema())) {
            schemaParseResult = solrFieldsParser.parseSchema(reader, schemaPlugin, validateSchema);
        }
        return new IIndexMetaData() {
            @Override
            public ParseResult getSchemaParseResult() {
                return schemaParseResult;
            }

            @Override
            public IIndexBuildLifeCycleHook getIndexBuildLifeCycleHook() {
                return AdapterIndexBuildLifeCycleHook.create(schemaParseResult);
            }

            @Override
            public LuceneVersion getLuceneVersion() {
                return LuceneVersion.LUCENE_7;
            }
        };
    }

    // private String getTISLuceneVersion(ISolrConfigGetter configGetter) {
    // try {
    // // =getLuceneVersion===============================================================
    // String luceneVersion = Version.LUCENE_7_6_0;
    // byte[] solrConfigContent = configGetter.getConfig();
    // ByteArrayInputStream solrReader = new ByteArrayInputStream(solrConfigContent);
    // try {
    // Document document = solrConfigDocumentbuilder.parse(solrReader);
    // Node luceneMatchVersionNode = (Node) SolrFieldsParser.createXPath()
    // .evaluate("config/luceneMatchVersion", document, XPathConstants.NODE);
    // if (luceneMatchVersionNode != null) {
    // // luceneVersion = Version.parse(luceneMatchVersionNode.getTextContent());
    // return luceneMatchVersionNode.getTextContent();
    // }
    // } finally {
    // IOUtils.closeQuietly(solrReader);
    // }
    // //logger.info("luceneMatchVersionNode:{}", luceneVersion);
    // // ================================================================================
    // return luceneVersion;
    // } catch (Exception e) {
    // throw new RuntimeException(e);
    // }
    // }
    public static XPath createXPath() {
        return xpathFactory.newXPath();
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        // SolrFieldsParser parse = new SolrFieldsParser();
        // File f = new File("D:/workspace/solrhome/supplyGoods/conf/ccc.xml");
        // InputStream is = new FileInputStream(f);
        // ParseResult result = parse.parseSchema(is, false);
        // System.out.println(result.getIndexBuilder());
    }

    public static boolean hasMultiValuedField(ArrayList<PSchemaField> fields) {
        for (PSchemaField field : fields) {
            if (field.isMltiValued()) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<PSchemaField> readSchemaFields(InputStream is, ISchemaPluginContext schemaPlugin) throws Exception {
        ParseResult result = this.parseSchema(is, schemaPlugin);
        if (!result.isValid()) {
            throw new IllegalStateException(result.getErrorSummary());
        }
        return result.dFields;
    }

    public ParseResult readSchema(InputStream is, ISchemaPluginContext schemaPlugin) throws Exception {
        ParseResult result = this.parseSchema(is, schemaPlugin);
        if (!result.isValid()) {
            throw new IllegalStateException(result.getErrorSummary());
        }
        return result;
    }

    public ParseResult parseSchema(InputStream is, ISchemaFieldTypeContext schemaPlugin) throws Exception {
        return parseSchema(is, schemaPlugin, true);
    }

    public ParseResult parseSchema(InputStream is, ISchemaFieldTypeContext schemaPlugin, boolean shallValidate) throws Exception {
        DocumentBuilderFactory schemaDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
        // 只是读取schema不作校验
        schemaDocumentBuilderFactory.setValidating(shallValidate);
        final ParseResult result = new ParseResult(shallValidate);
        DocumentBuilder builder = schemaDocumentBuilderFactory.newDocumentBuilder();
        InputSource input = new InputSource(is);
        if (!shallValidate) {
            builder.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                    InputSource source = new InputSource();
                    source.setCharacterStream(new StringReader(""));
                    return source;
                }
            });
        } else {
            final DefaultHandler mh = new DefaultHandler() {
                public void error(SAXParseException e) throws SAXException {
                    result.errlist.add("行号:" + e.getLineNumber() + " " + e.getMessage() + "<br/>");
                }

                public void fatalError(SAXParseException e) throws SAXException {
                    this.error(e);
                }
            };
            builder.setErrorHandler(mh);
            builder.setEntityResolver(new EntityResolver() {
                @Override
                public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                    // final String tisrepository = TSearcherConfigFetcher.get().getTisConsoleHostAddress();
                    final String tisrepository = Config.getConfigRepositoryHost();
                    final URL url = new URL(tisrepository + "/dtd/solrschema.dtd");
                    return new InputSource(new ByteArrayInputStream(ConfigFileContext.processContent(url, new StreamProcess<byte[]>() {

                        @Override
                        public byte[] p(int status, InputStream stream, Map<String, List<String>> headerFields) {
                            try {
                                return IOUtils.toByteArray(stream);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    })));
                }
            });
        }
        Document document = null;
        try {
            document = builder.parse(input);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        if (!result.isValid()) {
            return result;
        }
        return parse(document, schemaPlugin, shallValidate);
    }

    private static final Pattern SPACE = Pattern.compile("\\s+");

    public static ParseResult parseDocument(Document document, ISchemaPluginContext schemaPlugin, boolean shallValidate) throws Exception {
        return solrFieldsParser.parse(document, schemaPlugin, shallValidate);
    }

    public ParseResult parse(Document document, ISchemaFieldTypeContext schemaPlugin, boolean shallValidate) throws Exception {
        // .newXPath();
        final XPath xpath = createXPath();
        ParseResult parseResult = new ParseResult(shallValidate);
        parseResult.dFieldsNames = new HashSet<>();
        final ArrayList<PSchemaField> dFields = parseResult.dFields;
        // 取得fields type
        String expression = "/schema/types/fieldType|/schema/types/fieldtype";
        NodeList nodes = (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);
        // Map<String, SolrType> types = new HashMap<String, SolrType>();
        String typeName = null;
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            NamedNodeMap attrs = node.getAttributes();
            typeName = DOMUtil.getAttr(attrs, "name");
            if (shallValidate && parseResult.types.containsKey(typeName)) {
                parseResult.errlist.add("重复定义了字段类型‘" + typeName + "’");
                return parseResult;
            }
            parseResult.types.put(typeName
                    , parseFieldType(typeName, DOMUtil.getAttr(attrs, "class", "class definition"), schemaPlugin.isTokenizer(typeName)));
        }
        fieldTypeVisitor.visit(nodes);
        addExtenionProcessor(parseResult, xpath, document);
        // addColumnProcessor(parseResult, xpath, document);
        // 取得fields
        expression = "/schema/fields/field";
        ParseResult result = parseField(document, shallValidate, xpath, parseResult, dFields, expression, false);
        if (!result.isValid()) {
            return result;
        }
        expression = "/schema/fields/dynamicField";
        result = parseField(document, shallValidate, xpath, parseResult, dFields, expression, true);
        if (!result.isValid()) {
            return result;
        }
        if (parseResult.dFieldsNames == null) {
            throw new IllegalStateException("parseResult.dFieldsNames can not be null");
        }
        parseResult.dFieldsNames = Collections.unmodifiableSet(parseResult.dFieldsNames);
        final String uniqueKey = (String) xpath.evaluate("/schema/uniqueKey", document, XPathConstants.STRING);
        final String sharedKey = (String) xpath.evaluate("/schema/sharedKey", document, XPathConstants.STRING);
        boolean uniqueKeyDefined = false;
        boolean shareKeyDefined = false;
        // final String defaultSearchField = (String) xpath.evaluate("/schema/defaultSearchField", document, XPathConstants.STRING);
        if (shallValidate) {
            for (PSchemaField f : dFields) {
                if (f.getName().equals(uniqueKey)) {
                    uniqueKeyDefined = true;
                    if (!f.isIndexed()) {
                        // parseResult.errlist.add(
                        // "defined uniqueKey:" + defaultSearchField + " field
                        // property 'indexed' shall be true");
                        parseResult.errlist.add("主键:‘" + uniqueKey + "’属性'indexed'必须为true");
                        return result;
                    }
                    // 主键不能为多值
                    if (f.isMultiValue()) {
                        parseResult.errlist.add("主键:‘" + uniqueKey + "’属性'multiValued'必须为false");
                    }
                }
                if (f.getName().equals(sharedKey)) {
                    shareKeyDefined = true;
                }
                // if (f.getName().equals(defaultSearchField)) {
                // defaultSearchFieldDefined = true;
                // if (!f.isIndexed()) {
                // parseResult.errlist.add("默认查询键:‘" + defaultSearchField + "’属性'indexed'必须为true");
                // return result;
                // }
                // }
            }
            if (!shareKeyDefined) {
                // parseResult.errlist.add("shareKey have not been define in sub
                // element of schema/fields");
                parseResult.errlist.add("请设置分区键");
            }
            if (!uniqueKeyDefined) {
                // parseResult.errlist.add("uniqueKey have not been define in
                // sub element of schema/fields");
                parseResult.errlist.add("请设置主键");
                return result;
            }
            // 判断定义了defaultSearchField 但是没有在schema中找到
            // if (StringUtils.isNotBlank(defaultSearchField) && !defaultSearchFieldDefined) {
            // // result.errlist.add(
            // // "defined defaultSearchField:" + defaultSearchField + " can
            // // not be found in the fields list");
            // result.errlist.add("已定义的默认查询字段:" + defaultSearchField + "在fields中不存在");
            // return result;
            // }
        }
        parseResult.setUniqueKey(uniqueKey);
        parseResult.setSharedKey(sharedKey);
        final String indexBuilder = getIndexBuilder((NodeList) xpath.evaluate("/schema", document, XPathConstants.NODESET));
        if (!StringUtils.isBlank(indexBuilder)) {
            parseResult.setIndexBuilder(indexBuilder);
        }
        final NodeList schemaNodes = (NodeList) xpath.evaluate("/schema", document, XPathConstants.NODESET);
        final String indexMakerClassName = getIndexMakerClassName(schemaNodes);
        if (!StringUtils.isBlank(indexMakerClassName)) {
            parseResult.setIndexMakerClassName(indexMakerClassName);
        }
        // 构建全量索引doc构建工厂
        parseResult.setDocumentCreatorType(getDocMaker(schemaNodes));
        return parseResult;
    }

    private static final Pattern PATTERN_COMMENT_PROCESSOR = Pattern.compile("^\\{(\\w+?) (.*)\\}$");

    private static final Pattern PATTERN_INDEX_BUILD_HOOK = Pattern.compile("^\\{\\s*buildhook (.*)\\}$");

    private void addExtenionProcessor(ParseResult parseResult, XPath xPath, Document document) throws XPathExpressionException {
        Node fieldsNode = (Node) xPath.evaluate("/schema/fields", document, XPathConstants.NODE);
        NodeList nodes = fieldsNode.getChildNodes();
        aa:
        for (int i = 0; i < nodes.getLength(); i++) {
            Node fieldNode = nodes.item(i);
            String comment = fieldNode.getTextContent();
            if (fieldNode.getNodeType() == Node.COMMENT_NODE && !StringUtils.isBlank(comment)) {
                Matcher matcher = PATTERN_INDEX_BUILD_HOOK.matcher(comment);
                if (matcher.find() && matcher.groupCount() == 1) {
                    parseResult.addIndexBuildHook(IndexBuildHook.create(matcher.group(1)));
                    continue aa;
                }
                matcher = PATTERN_COMMENT_PROCESSOR.matcher(comment);
                if (matcher.find() && matcher.groupCount() == 2) {
                    parseResult.addProcessorSchema(ProcessorSchemaField.create(matcher.group(1), matcher.group(2)));
                }
            }
        }
    }

    /**
     * 设置 com.qlangtech.tis.indexbuilder.index.indexMaker的扩展类
     *
     * @param nodes
     * @return
     */
    private String getIndexMakerClassName(NodeList nodes) {
        return getFirstNodeAtt(nodes, "indexMaker");
    }

    /**
     * 取得docMaker实现类
     *
     * @param nodes
     * @return
     */
    private String getDocMaker(NodeList nodes) {
        return getFirstNodeAtt(nodes, "docMaker");
    }

    private String getFirstNodeAtt(NodeList nodes, String attriName) {
        String indexBuilder = null;
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            String name = DOMUtil.getAttr(node.getAttributes(), attriName, null);
            if (!StringUtils.isBlank(name)) {
                indexBuilder = name;
                break;
            }
        }
        return indexBuilder;
    }

    private String getIndexBuilder(NodeList nodes) {
        String indexBuilder = null;
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            String name = DOMUtil.getAttr(node.getAttributes(), "indexBuilder", null);
            if (!StringUtils.isBlank(name)) {
                indexBuilder = name;
                break;
            }
        }
        return indexBuilder;
    }

    private ParseResult parseField(Document document, boolean shallValidate, final XPath xpath, ParseResult parseResult
            , final ArrayList<PSchemaField> dFields, String expression, boolean dynamicField) throws Exception {

        NodeList nodes;
        nodes = (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);
        PSchemaField field = null;
        Matcher matcher = null;
        for (int i = 0; i < nodes.getLength(); i++) {
            // new PSchemaField();
            field = new PSchemaField();
            field.setDynamic(dynamicField);
            Node node = nodes.item(i);
            NamedNodeMap attrs = node.getAttributes();
            String name = DOMUtil.getAttr(attrs, "name", "field definition");
            matcher = SPACE.matcher(name);
            if (shallValidate && matcher.find()) {
                // parseResult.errlist.add("name:" + name + " can not contain
                // space word");
                parseResult.errlist.add("字段名称:‘" + name + "’中不能有空格");
                return parseResult;
            }
            field.setName(name);
            String typeKey = DOMUtil.getAttr(attrs, "type", "field " + name);
            SolrType type = parseResult.types.get(typeKey);
            // + " is not define in fieldtypes", type);
            if (shallValidate && type == null) {
                // parseResult.errlist.add("typeKey:" + typeKey + " is not
                // define in fieldtypes");
                // parseResult.errlist.add("typeKey:" + typeKey + " is not
                // define in fieldtypes");
                parseResult.errlist.add("字段类型:‘" + typeKey + "’在已有类型中不存在");
                return parseResult;
            }
            if (!shallValidate && type == null) {
                type = parseFieldType(typeKey, typeKey);
            }

            field.setType(type);
            Map<String, String> args = DOMUtil.toMapExcept(attrs, "name", "type");
            if (args.get("required") != null) {
                field.setRequired(Boolean.valueOf(args.get("required")));
            }
            if (args.get("indexed") != null) {
                field.setIndexed(Boolean.valueOf(args.get("indexed")));
            }
            if (args.get("stored") != null) {
                field.setStored(Boolean.valueOf(args.get("stored")));
            }
            if (args.get("multiValued") != null) {
                field.setMltiValued(Boolean.valueOf(args.get("multiValued")));
            }
            if (args.get("docValues") != null) {
                field.setDocValue(Boolean.valueOf(args.get("docValues")));
            }
            if (args.get("useDocValuesAsStored") != null) {
                field.setUseDocValuesAsStored(Boolean.valueOf(args.get("useDocValuesAsStored")));
            }
            if (args.get("default") != null) {
                field.setDefaultValue(args.get("default"));
            }
            parseResult.dFieldsNames.add(field.getName());
            dFields.add(field);
        }
        return parseResult;
    }

    public static SolrType parseFieldType(String name, String fieldType) {
        return parseFieldType(name, fieldType, false);
    }


    /**
     * 解析字段类型
     *
     * @param name
     * @param fieldType
     * @param splittable 是否可分词
     * @return
     */
    public static SolrType parseFieldType(String name, String fieldType, boolean splittable) {
        SolrType type = new SolrType(splittable, StringUtils.startsWith(fieldType, KEY_PLUGIN + ":"));
        Type t = new Type(name);
        t.setSolrType(fieldType);
        type.setSolrType(t);
        type.setJavaType(SolrJavaType.parse(fieldType));
        return type;
    }

    public static class Type {

        private String solrType;

        private final String name;

        public String getName() {
            return name;
        }

        public Type(String name) {
            super();
            this.name = name;
        }

        public String getSolrType() {
            return solrType;
        }

        public void setSolrType(String solrType) {
            this.solrType = solrType;
        }
    }

    public static class SolrType {
        public final boolean tokenizerable;
        // 是否是插件类型
        public final boolean plugin;

        private SolrType(boolean tokenizerable, boolean plugin) {
            this.tokenizerable = tokenizerable;
            this.plugin = plugin;
        }

        public String getPluginName() {
            if (!plugin) {
                return null;
            }
            return StringUtils.substringAfter(this.getSolrType(), SolrFieldsParser.KEY_PLUGIN + ":");
        }

        private SolrJavaType javaType;

        private Type solrType;

        private Method valueof;

        public SolrJavaType getJavaType() {
            return this.javaType;
        }

        public Object valueOf(Object val) throws Exception {
            return valueof.invoke(null, val);
        }

        public void setJavaType(SolrJavaType javaType) {
//            try {
//                if (javaType == String.class) {
//                    valueof = javaType.getMethod("valueOf", Object.class);
//                } else {
//                    valueof = javaType.getMethod("valueOf", String.class);
//                }
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
            this.javaType = javaType;
        }

        public String getSolrType() {
            return solrType.getSolrType();
        }

        public Type getSType() {
            return solrType;
        }

        public void setSolrType(Type solrType) {
            this.solrType = solrType;
        }
    }


    public static class SchemaFields extends ArrayList<PSchemaField> {

        private static final long serialVersionUID = 1L;

        private final Map<String, PSchemaField> fieldMap = new HashMap<>();

        @Override
        public boolean add(PSchemaField e) {
            fieldMap.put(e.getName(), e);
            return super.add(e);
        }

        public PSchemaField getField(String fieldName) {
            return fieldMap.get(fieldName);
        }
    }



    public interface ParseResultCallback {
        /**
         * @param cols   topology 宽表中解析出来的宽表字段
         * @param result
         */
        public void process(List<ColumnMetaData> cols, ParseResult result);
    }



    public interface IFieldTypesVisit {
        public void visit(NodeList typeNodes);
    }
}
