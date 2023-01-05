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

package com.qlangtech.tis.sql.parser.stream.generate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.tis.datax.IDataxReader;
import com.qlangtech.tis.datax.TableAlias;
import com.qlangtech.tis.datax.TableAliasMapper;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.manage.IBasicAppSource;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.manage.common.incr.StreamContextConstant;
import com.qlangtech.tis.realtime.transfer.UnderlineUtils;
import com.qlangtech.tis.sql.parser.TisGroupBy;
import com.qlangtech.tis.sql.parser.er.ERRules;
import com.qlangtech.tis.sql.parser.er.IERRules;
import com.qlangtech.tis.sql.parser.er.PrimaryTableMeta;
import com.qlangtech.tis.sql.parser.meta.TabExtraMeta;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.sql.parser.tuple.creator.IStreamIncrGenerateStrategy;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.FunctionDataTupleCreator;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.PropGetter;
import com.qlangtech.tis.sql.parser.visitor.FuncFormat;
import com.qlangtech.tis.sql.parser.visitor.IBlockToString;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

//java runtime compiler: https://blog.csdn.net/lmy86263/article/details/59742557

/**
 * 基于SQL的增量组件代码生成器，（scala代码）
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年10月11日
 */
public class StreamComponentCodeGeneratorFlink extends StreamCodeContext {

    private static final Logger logger = LoggerFactory.getLogger(StreamComponentCodeGeneratorFlink.class);

    private final IBasicAppSource streamIncrGenerateStrategy;

    private final List<FacadeContext> daoFacadeList;


    public StreamComponentCodeGeneratorFlink(String collectionName, long timestamp,
                                             List<FacadeContext> daoFacadeList, IBasicAppSource streamIncrGenerateStrategy) {
        super(collectionName, timestamp);
        Objects.requireNonNull(streamIncrGenerateStrategy, "streamIncrGenerateStrategy can not be null");
        // this.erRules = ERRules.getErRule(topology.getName());
        this.streamIncrGenerateStrategy = streamIncrGenerateStrategy;
        this.daoFacadeList = daoFacadeList;

    }

    private List<TableAlias> getTabTriggerLinker() {
        return streamIncrGenerateStrategy.accept(new IBasicAppSource.IAppSourceVisitor<List<TableAlias>>() {
            @Override
            public List<TableAlias> visit(DataxProcessor processor) {
                // Map<IEntityNameGetter, List<IValChain>> tabColsMapper = Maps.newHashMap();
                List<TableAlias> aliases = Lists.newArrayList();
                IDataxReader reader = processor.getReader(null);
                Objects.requireNonNull(reader, "dataXReader can not be null");
                TableAliasMapper tabAlias = processor.getTabAlias();

                tabAlias.forEach((key, alia) -> {
                    aliases.add(alia);
                });
                return aliases;

//                for (ISelectedTab tab : selectedTabs) {
//
//                    tabAlias.getWithCheckNotNull(tab.getName());
//
//                    tabColsMapper.put(() ->
//                                    EntityName.parse((tabAlias.getWithCheckNotNull(tab.getName())).getTo())
//                            , Collections.emptyList());
//                }
//                return tabColsMapper;
            }
        });
    }


    //    @Override
    private IERRules getERRule() {
        ERRules erRules = new ERRules() {
            public List<PrimaryTableMeta> getPrimaryTabs() {
                TabExtraMeta tabMeta = new TabExtraMeta();
                PrimaryTableMeta ptab = new PrimaryTableMeta("tabName", tabMeta);
                return Collections.singletonList(ptab);
            }
        };
        return erRules;
    }

    /**
     * 开始生成增量执行脚本（scala版本）
     *
     * @throws Exception
     */
    public void build() throws Exception {
        // final PrintStream traversesAllNodeOut = new PrintStream(new File("./traversesAllNode.txt"));
        MergeData mergeData = new MergeData(this.collectionName, mapDataMethodCreatorMap,
                getTabTriggerLinker(), getERRule(), this.daoFacadeList, this.streamIncrGenerateStrategy);

        mergeGenerate(streamIncrGenerateStrategy.decorateMergeData(mergeData));
        try {
//            Map<IEntityNameGetter, List<IValChain>> tabTriggers = this.streamIncrGenerateStrategy.getTabTriggerLinker();
//            IERRules erR = streamIncrGenerateStrategy.getERRule();

//            PropGetter last = null;
//            PropGetter first = null;
//            Optional<TableRelation> firstParent = null;
            // Map<IEntityNameGetter, List<IValChain>> tabTriggers = getTabTriggerLinker();

            //  FuncFormat aliasListBuffer = new FuncFormat();


            // for (Map.Entry<IEntityNameGetter, List<IValChain>> e : tabTriggers.entrySet()) {
//                final EntityName entityName = e.getKey().getEntityName();
//                final Set<String> relevantCols = e.getValue().stream()
//                        .map((rr) -> rr.last().getOutputColName().getName()).collect(Collectors.toSet());
//
//                //>>>>>>>>>>>>>
//                // 包括主表的和子表的
//                final Set<String> linkCols = Sets.newHashSet();
//                final List<TableRelation> allParent = erR.getAllParent(entityName);
//                for (TableRelation r : allParent) {
//                    linkCols.addAll(r.getJoinerKeys().stream().map((j) -> j.getChildKey()).collect(Collectors.toList()));
//                }
//
//                final List<TableRelation> allChild = erR.getChildTabReference(entityName);
//                for (TableRelation r : allChild) {
//                    linkCols.addAll(r.getJoinerKeys().stream().map((j) -> j.getParentKey()).collect(Collectors.toList()));
//                }
//                //<<<<<<<<<<<<<<
//
//                traversesAllNodeOut.println("<<<<<<<%%%%%%%%export:" + entityName);
//
//                aliasListBuffer.append("val ").append(entityName.getJavaEntityName())
//                        .append("Builder:AliasList.Builder = builder.add(\"").append(entityName.getTabName())
//                        .append("\")");
//                final boolean isTriggerIgnore = erR.isTriggerIgnore(entityName);
//                if (isTriggerIgnore) {
//                    aliasListBuffer.append(".setIgnoreIncrTrigger()");
//                }
//
//                // 设置是否是主键
//                boolean isPrimaryTable = false;
//                Optional<TableMeta> primaryFind = erR.getPrimaryTab(entityName);
//                PrimaryTableMeta ptab = null;
//                if (primaryFind.isPresent()) {
//                    isPrimaryTable = true;
//                    ptab = (PrimaryTableMeta) primaryFind.get();
//                    aliasListBuffer.append(".setPrimaryTableOfIndex()");
//                }
//
//                aliasListBuffer.returnLine();
//
//                if (!isPrimaryTable) {
//                    // 设置主键
//                    aliasListBuffer
//                            .methodBody(entityName.javaPropTableName() + "ColEnum.getPKs().forEach((r) =>", (r) -> {
//                                r.startLine(entityName.getJavaEntityName()).append("Builder.add(r.getName().PK())");
//                            }).append(")");
//                }
//
//
//                aliasListBuffer.startLine(entityName.getJavaEntityName()).append("Builder.add(").append(" // ").returnLine();
//
//                boolean timestampVerColumnProcessed = false;
//                // 判断out的列是否已经输出
//                Set<String> outCol = Sets.newHashSet();
//                boolean firstAdd = true;
//                boolean hasSetTimestampVerColumn;
//                if (!(hasSetTimestampVerColumn = erR.hasSetTimestampVerColumn(entityName))) {
//                    if (erR.getTimeCharacteristic() != TimeCharacteristic.ProcessTime) {
//                        throw new IllegalStateException("table:" + entityName.getTabName()
//                                + "either have not set timestampVer col name or global timeCharacteristic is not 'ProcessTime'");
//                    }
//                    firstAdd = false;
//                    aliasListBuffer.append("(\"processTime\").processTimeVer()").returnLine();
//                }
//
//                for (IValChain tupleLink : e.getValue()) {
//                    first = tupleLink.first();
//                    last = tupleLink.last();
//                    traversesAllNodeOut.println("last:" + (last == null ? "null" : last.getIdentityName()));
//                    traversesAllNodeOut.println("first:" + (first == null ? "null" : first.getIdentityName()));
//                    traversesAllNodeOut.println(Joiner.on("\n-->").join(tupleLink.mapChainValve((r/* PropGetter */) -> {
//                        return r.getIdentityName();
//                    }).iterator()));
//
//                    traversesAllNodeOut.println("-------------------------------");
//
//                    boolean haveAdd = outCol.add(first.getOutputColName().getAliasName());
//
//                    if (!firstAdd) {
//                        aliasListBuffer.startLine(",");
//                    } else {
//                        firstAdd = false;
//                    }
//
//
//                    if (tupleLink.useAliasOutputName()) {
//                        aliasListBuffer.append("(\"").append(last.getOutputColName().getName()).append("\", \"")
//                                .append(first.getOutputColName().getAliasName()).append("\")");
//                    } else {
//                        aliasListBuffer.append("(\"").append(last.getOutputColName().getName()).append("\")");
//                    }
//
//                    // 如果是主表就在通过单独的列meta配置中的信息来设置主键，在实际例子中发现表中使用了联合主键，在运行的时候会出错
//                    if (isPrimaryTable && ptab.isPK(last.getOutputColName().getName())) {
//                        aliasListBuffer.append(".PK()");
//                    }
//
//                    if (hasSetTimestampVerColumn && erR.isTimestampVerColumn(entityName, last.getOutputColName().getName())) {
//                        // 时间戳字段
//                        aliasListBuffer.append(".timestampVer()");
//                        timestampVerColumnProcessed = true;
//                    }
//
//                    if (!haveAdd) {
//                        aliasListBuffer.append(".notCopy()");
//
//                    } else if (tupleLink.hasFuncTuple()) {
//
//                        AtomicBoolean shallCallableProcess = new AtomicBoolean(false);
//
//                        final FunctionVisitor.IToString shallCallableProcessToken = new FunctionVisitor.IToString() {
//                            @Override
//                            public String toString() {
//                                return shallCallableProcess.get() ? "c" : "t";
//                            }
//                        };
//
//                        final FunctionVisitor.IToString fieldValue = new FunctionVisitor.IToString() {
//                            @Override
//                            public String toString() {
//                                return shallCallableProcess.get() ? StringUtils.EMPTY : ", fieldValue";
//                            }
//                        };
//
//                        aliasListBuffer.append(".").append(shallCallableProcessToken).append("(")//
//                                .append("(" + FunctionVisitor.ROW_KEY).append(fieldValue).append(")")
//                                .methodBody(false, " => ", (rr) -> {
//                                    final AtomicInteger index = new AtomicInteger();
//                                    final AtomicReference<String> preGroupAggrgationName = new AtomicReference<>();
//                                    tupleLink.chainStream()
//                                            .filter((r) -> r.getTupleCreator() != null
//                                                    && r.getTupleCreator() instanceof FunctionDataTupleCreator)
//                                            .forEach((r) -> {
//                                                final FunctionDataTupleCreator tuple = (FunctionDataTupleCreator) r
//                                                        .getTupleCreator();
//                                                final PropGetter propGetter = r;
//                                                MapDataMethodCreator mapDataMethodCreator = null;
//                                                Optional<TisGroupBy> group = tuple.getGroupBy();
//                                                if (propGetter.shallCallableProcess()) {
//                                                    shallCallableProcess.set(true);
//                                                }
//                                                if (index.getAndIncrement() < 1) {
//
//                                                    if (r.isGroupByFunction()) {
//                                                        TisGroupBy groups = group.get();
//                                                        mapDataMethodCreator = addMapDataMethodCreator(entityName, groups, relevantCols);
//
//                                                        rr.startLine("val ").append(groups.getGroupAggrgationName())
//                                                                .append(":Map[GroupKey /*")
//                                                                .append(groups.getGroupsLiteria())
//                                                                .append("*/, GroupValues]  = ")
//                                                                .append(mapDataMethodCreator.getMapDataMethodName())
//                                                                .append("(").append(FunctionVisitor.ROW_KEY).append(")")
//                                                                .returnLine().returnLine();
//
//                                                        preGroupAggrgationName.set(groups.getGroupAggrgationName());
//
//                                                        generateCreateGroupResultScript(rr, propGetter, groups);
//
//                                                    } else {
//                                                        // 不需要反查维表执行函数
//                                                        // 测试
//                                                        generateFunctionCallScript(rr, propGetter);
//                                                    }
//
//                                                } else {
//                                                    if (r.isGroupByFunction()) {
//                                                        TisGroupBy groups = group.get();
//
//                                                        rr.append("val ").append(groups.getGroupAggrgationName())
//                                                                .append(": Map[GroupKey /* ")
//                                                                .append(groups.getGroupsLiteria())
//                                                                .append(" */, GroupValues] ").append(" = reduceData(")
//                                                                .append(preGroupAggrgationName.get()).append(", " + groups.getGroupKeyAsParamsLiteria() + ")\n");
//
//                                                        generateCreateGroupResultScript(rr, propGetter, groups);
//
//                                                        preGroupAggrgationName.set(groups.getGroupsLiteria());
//                                                    } else {
//
//                                                        generateFunctionCallScript(rr, propGetter);
//                                                    }
//
//                                                }
//
//                                            });
//
//                                }).append(")/*end .t()*/");
//
//                    }
//                }
//
//                for (String linkKey : linkCols) {
//                    if (!relevantCols.contains(linkKey)) {
//                        if (!firstAdd) {
//                            aliasListBuffer.appendLine(",");
//                        } else {
//                            firstAdd = false;
//                            aliasListBuffer.returnLine();
//                        }
//                        aliasListBuffer.append("(\"")
//                                .append(linkKey).append("\").notCopy()  ");
//
//                        if (erR.isTimestampVerColumn(entityName, linkKey)) {
//                            aliasListBuffer.append(".timestampVer()");
//                            timestampVerColumnProcessed = true;
//                        }
//
//                        aliasListBuffer.append("// FK or primay key");
//                    }
//                }
//
//
//                // timestampVer标记没有添加，且本表不要监听增量消息
//                if (hasSetTimestampVerColumn && !timestampVerColumnProcessed && !erR.isTriggerIgnore(entityName)) {
//                    if (!firstAdd) {
//                        aliasListBuffer.appendLine(",");
//                    } else {
//                        firstAdd = false;
//                        aliasListBuffer.returnLine();
//                    }
//                    aliasListBuffer.append("(\"")
//                            .append(erR.getTimestampVerColumn(entityName)).append("\").notCopy().timestampVer() //gencode9 ");
//                }
//
//                //>>>>>>>>>>>>>
//                aliasListBuffer.appendLine(");\n");
//                traversesAllNodeOut.println("======================================>>>>>>>>>");
//
//                //>>>>>>>>>>>>>
//                for (TableRelation r : allParent) {
//
//                    aliasListBuffer.append(entityName.getJavaEntityName())
//                            .append("Builder.addParentTabRef(").append(r.getParent().parseEntityName().createNewLiteriaToken())
//                            .append(",").append(JoinerKey.createListNewLiteria(r.getJoinerKeys())).append(")").returnLine();
//                }
//
//                for (TableRelation r : allChild) {
//                    aliasListBuffer.append(entityName.getJavaEntityName())
//                            .append("Builder.addChildTabRef(").append(r.getChild().parseEntityName().createNewLiteriaToken())
//                            .append(",").append(JoinerKey.createListNewLiteria(r.getJoinerKeys())).append(")").returnLine();
//                }
//                //<<<<<<<<<<<<<<
//
//                if (!this.excludeFacadeDAOSupport) {
//
//                    aliasListBuffer.append(entityName.getJavaEntityName())
//                            .append("Builder.setGetterRowsFromOuterPersistence(/*gencode5*/")
//                            .methodBody(" (rowTabName, rvals, pk ) =>", (f) -> {
//                                Set<String> selectCols = Sets.union(relevantCols, linkCols);
//                                if (primaryFind.isPresent()) {
//                                    // 主索引表
//                                    PrimaryTableMeta ptabMeta = (PrimaryTableMeta) primaryFind.get();
//
//                                    f.appendLine(entityName.buildDefineRowMapListLiteria());
//                                    f.appendLine(entityName.buildDefineCriteriaEqualLiteria());
//
//                                    List<PrimaryLinkKey> primaryKeyNames = ptabMeta.getPrimaryKeyNames();
//                                    for (PrimaryLinkKey linkKey : primaryKeyNames) {
//                                        if (!linkKey.isPk()) {
//                                            TisGroupBy.TisColumn routerKey = new TisGroupBy.TisColumn(linkKey.getName());
//                                            f.appendLine(routerKey.buildDefineGetPkRouterVar());
//                                        }
//                                    }
//                                    f.appendLine(entityName.buildCreateCriteriaLiteria());
//                                    for (PrimaryLinkKey linkKey : primaryKeyNames) {
//                                        if (linkKey.isPk()) {
//                                            TisGroupBy.TisColumn pk = new TisGroupBy.TisColumn(linkKey.getName());
//                                            f.append(pk.buildPropCriteriaEqualLiteria("pk.getValue()"));
//                                        } else {
//                                            TisGroupBy.TisColumn pk = new TisGroupBy.TisColumn(linkKey.getName());
//                                            f.append(pk.buildPropCriteriaEqualLiteria());
//                                        }
//                                    }
//                                    f.appendLine(entityName.buildAddSelectorColsLiteria(selectCols));
//                                    f.appendLine(entityName.buildExecuteQueryDAOLiteria());
//                                    f.appendLine(entityName.entities());
//                                } else {
//                                    if (allChild.size() > 0) {
//                                        f.appendLine(entityName.buildDefineRowMapListLiteria());
//                                        f.appendLine(entityName.buildDefineCriteriaEqualLiteria()).returnLine();
//                                        f.appendLine(entityName.buildAddSelectorColsLiteria(selectCols));
//                                        f.methodBody("rowTabName match ", (ff) -> {
//                                            for (TableRelation rel : allChild) {
//                                                EntityName childEntity = rel.getChild().parseEntityName();
//
//                                                ff.methodBody("case \"" + childEntity.getTabName() + "\" =>", (fff) -> {
//                                                    fff.appendLine(entityName.buildCreateCriteriaLiteria());
//                                                    for (JoinerKey jk : rel.getJoinerKeys()) {
//                                                        TisGroupBy.TisColumn k = new TisGroupBy.TisColumn(jk.getParentKey());
//                                                        fff.append(k.buildPropCriteriaEqualLiteria("rvals.getColumn(\"" + jk.getChildKey() + "\")"));
//                                                    }
//                                                    fff.returnLine();
//
//                                                    fff.appendLine(entityName.buildExecuteQueryDAOLiteria());
//                                                    fff.appendLine(entityName.entities());
//                                                });
//                                            }
//                                            ff.appendLine("case unexpected => null");
//                                        });
//                                    } else {
//                                        f.appendLine(" null");
//                                    }
//                                }
//                            }).appendLine(") // end setGetterRowsFromOuterPersistence").returnLine().returnLine();
//                }
            // }


        } finally {
//            IOUtils.closeQuietly(traversesAllNodeOut, (ex) -> {
//                logger.error(ex.getMessage(), ex);
//            });
        }

    }

    /**
     * 生成spring等增量应用启动需要的配置文件
     */
    public void generateConfigFiles() throws Exception {


        MergeData mergeData = new MergeData(this.collectionName, mapDataMethodCreatorMap, Collections.emptyList()
                , getERRule(), this.daoFacadeList, this.streamIncrGenerateStrategy);

        File parentDir =
                new File(getSpringConfigFilesDir()
                        , "com/qlangtech/tis/realtime/transfer/" + this.collectionName);

        FileUtils.forceMkdir(parentDir);

        this.mergeGenerate(mergeData
                , IStreamIncrGenerateStrategy.IStreamTemplateResource.createClasspathResource("app-context.xml.vm", true)
                , new File(parentDir, "app-context.xml"));

        this.mergeGenerate(mergeData
                , IStreamIncrGenerateStrategy.IStreamTemplateResource.createClasspathResource("field-transfer.xml.vm", true)
                , new File(parentDir, "field-transfer.xml"));
    }

    /**
     * Spring config file root dir
     *
     * @return
     */
    public File getSpringConfigFilesDir() {
        return new File(StreamContextConstant.getStreamScriptRootDir(this.collectionName, this.timestamp), "scriptconfig");
    }

    private final Map<EntityName, MapDataMethodCreator> mapDataMethodCreatorMap = Maps.newHashMap();

    private MapDataMethodCreator addMapDataMethodCreator(EntityName entityName, TisGroupBy groups,
                                                         Set<String> relevantCols) {

        MapDataMethodCreator creator = mapDataMethodCreatorMap.get(entityName);
        if (creator == null) {

            creator = new MapDataMethodCreator(entityName, groups, this.streamIncrGenerateStrategy, relevantCols);
            mapDataMethodCreatorMap.put(entityName, creator);
        }

        return creator;
    }

    private void generateCreateGroupResultScript(FuncFormat rr, final PropGetter propGetter,
                                                 TisGroupBy groups) {
        if (propGetter.isLastFunctInChain()) {
            rr.startLine("var result:Any = null");
        }
        final AtomicBoolean hasBreak = new AtomicBoolean();
        rr.startLine(new IBlockToString() {
            public String toString() {
                return hasBreak.get() ? "breakable {" : StringUtils.EMPTY;
            }
        });

        rr.methodBody("for ((k:GroupKey, v:GroupValues)  <- " + groups.getGroupAggrgationName() + ")", (r) -> {
            final PropGetter pgetter = propGetter;

            if (propGetter.isLastFunctInChain()) {
                rr.startLine("result = ");
                hasBreak.set(true);
                propGetter.getGroovyScript(rr, true);
                rr.startLine("break");
                rr.returnLine();
            } else {

                PropGetter prev = propGetter.getPrev();
                while (prev != null) {
                    if (prev.getTupleCreator() instanceof FunctionDataTupleCreator) {
                        break;
                    }
                    prev = prev.getPrev();
                }

                boolean shallCallableProcess = (prev != null && prev.shallCallableProcess());
                if (shallCallableProcess) {
                    rr.startLine("putMediaResult(\"" + pgetter.getOutputColName().getAliasName() + "\", //");
                } else {
                    rr.startLine("v.putMediaData( // \n");
                    rr.startLine("\"").append(propGetter.getOutputColName().getAliasName()).append("\" // \n");
                    rr.startLine(", //\n");
                }

                propGetter.getGroovyScript(rr, true);// );

                if (shallCallableProcess) {
                    rr.startLine(") // end putMediaResult");
                    hasBreak.set(true);
                    rr.startLine("break");
                } else {
                    rr.startLine(") // end putMediaData\n");
                }
            }
        });

        rr.startLine(new IBlockToString() {
            public String toString() {
                return hasBreak.get() ? "} //end breakable" : StringUtils.EMPTY;
            }
        });

        if (propGetter.isLastFunctInChain()) {
            rr.startLine(" result //return");
        }

        rr.returnLine();
    }


    private VelocityContext createContext(IStreamIncrGenerateStrategy.IStreamTemplateData mergeData) {

        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("config", mergeData);

        return velocityContext;
    }

    private void mergeGenerate(IStreamIncrGenerateStrategy.IStreamTemplateData mergeData) throws IOException {
        IStreamIncrGenerateStrategy.IStreamTemplateResource tplResource
                = this.streamIncrGenerateStrategy.getFlinkStreamGenerateTplResource();
        Objects.requireNonNull(tplResource, "tplResource can not be null");
//        if (StringUtils.isEmpty(tplFileName)) {
//            throw new IllegalStateException("tplFileName can not be empty");
//        }

        this.mergeGenerate(mergeData
                , tplResource
                , getIncrScriptMainFile());

//        OutputStreamWriter writer = null;
//        // Reader tplReader = null;
//        try {
//
//            VelocityContext context = createContext(mergeData);
//
//            FileUtils.forceMkdir(this.incrScriptDir);
//
//            //File parent = getScalaStreamScriptDir(this.collectionName, this.timestamp);
//
//
//            File f = getIncrScriptMainFile();// new File(this.incrScriptDir, (mergeData.getJavaName()) + "Listener.scala");
//            //  System.out.println(f.getAbsolutePath());
//
//            writer = new OutputStreamWriter(FileUtils.openOutputStream(f), "utf8");
//
//
//            Template tpl = velocityEngine.getTemplate("/com/qlangtech/tis/classtpl/mq_listener_scala.vm", "utf8");
//            tpl.merge(context, writer);
//            // velocityEngine.evaluate(context, writer, "listener", tplReader);
//
//            writer.flush();
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        } finally {
//            IOUtils.closeQuietly(writer);
//            //IOUtils.closeQuietly(tplReader);
//        }
    }

    private void mergeGenerate(IStreamIncrGenerateStrategy.IStreamTemplateData mergeData
            , IStreamIncrGenerateStrategy.IStreamTemplateResource tplResource, File createdFile) {
        OutputStreamWriter writer = null;
        try {
            VelocityContext context = this.createContext(mergeData);

            FileUtils.forceMkdir(this.incrScriptDir);
            writer = new OutputStreamWriter(FileUtils.openOutputStream(createdFile), TisUTF8.get());


            if (tplResource instanceof IStreamIncrGenerateStrategy.StringTemplateResource) {

                try (Reader reader = ((IStreamIncrGenerateStrategy.StringTemplateResource) tplResource).getContentReader()) {
                    velocityEngine.evaluate(context, writer, "tis", reader);
                }
            } else if (tplResource instanceof IStreamIncrGenerateStrategy.ClasspathTemplateResource) {
                Template tpl = velocityEngine.getTemplate(((IStreamIncrGenerateStrategy.ClasspathTemplateResource) tplResource).getTplPath(), TisUTF8.getName());
                //tpl = new Template();
                tpl.merge(context, writer);
            }


            writer.flush();

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    public final File getIncrScriptMainFile() {
        return new File(this.incrScriptDir, (UnderlineUtils.getJavaName(this.collectionName)) + "Listener.scala");
    }


    private static final VelocityEngine velocityEngine;

    static {
        try {
            velocityEngine = new VelocityEngine();
            Properties prop = new Properties();
            prop.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogChute");
            prop.setProperty("resource.loader", "classpath");
            prop.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
            velocityEngine.init(prop);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


//    public TableTupleCreator parseFinalSqlTaskNode() throws Exception {
//        return topology.parseFinalSqlTaskNode();
//    }


    // public void testRootNode() throws Exception {
    //
    // SqlTaskNode taskNode =
    // SqlTaskNode.findTerminatorTaskNode(SqlTaskNode.parseTaskNodes());
    // Assert.assertNotNull(taskNode);
    //
    // Assert.assertEquals(totalpay_summary, taskNode.getExportName());
    //
    // }
    //
    // public void testParseNode() throws Exception {
    //
    // TableTupleCreator totalpaySummaryTuple =
    // this.parseSqlTaskNode(totalpay_summary);
    //
    // ColRef colRef = totalpaySummaryTuple.getColsRefs();
    //
    // for (Map.Entry<ColName /* colName */, IDataTupleCreator> entry :
    // colRef.colRefMap.entrySet()) {
    // // System.out.println(entry.getKey() + ":" + entry.getValue());
    // }
    // System.out.println("base===================================================");
    // for (Map.Entry<String /* base */, IDataTupleCreator> entry :
    // colRef.baseRefMap.entrySet()) {
    // // System.out.println(entry.getKey() + ":" + entry.getValue());
    // }
    //
    // }

}
