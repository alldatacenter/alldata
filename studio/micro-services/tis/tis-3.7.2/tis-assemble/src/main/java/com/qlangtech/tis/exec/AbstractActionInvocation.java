
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
package com.qlangtech.tis.exec;

import com.google.common.collect.Maps;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.exec.datax.DataXExecuteInterceptor;
import com.qlangtech.tis.exec.impl.DefaultChainContext;
import com.qlangtech.tis.exec.impl.TrackableExecuteInterceptor;
import com.qlangtech.tis.exec.impl.WorkflowDumpAndJoinInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2014年9月6日下午5:03:00
 */
public class AbstractActionInvocation implements ActionInvocation {

    // 由数据中心触发的直接進入索引build階段
    public static final String COMMAND_KEY_DIRECTBUILD = "directbuild";

    private static final Logger logger = LoggerFactory.getLogger(AbstractActionInvocation.class);

//    private static final IExecuteInterceptor[] directBuild = new IExecuteInterceptor[]{ // /////
//            new IndexBuildWithHdfsPathInterceptor(), new IndexBackFlowInterceptor()};

    // 工作流執行方式
//    public static final IExecuteInterceptor[] workflowBuild = new IExecuteInterceptor[]{ // new WorkflowTableJoinInterceptor(),
//            new WorkflowDumpAndJoinInterceptor(), new WorkflowIndexBuildInterceptor(), new IndexBackFlowInterceptor()};

    public static final IExecuteInterceptor[] dataXBuild = new IExecuteInterceptor[]{new DataXExecuteInterceptor()};

//    private static IIndexMetaData createIndexMetaData(DefaultChainContext chainContext) {
//        if (!chainContext.hasIndexName()) {
//            return new DummyIndexMetaData();
//        }
//        try {
//            SnapshotDomain domain = HttpConfigFileReader.getResource(chainContext.getIndexName(), RunEnvironment.getSysRuntime(), ConfigFileReader.FILE_SCHEMA);
//            return SolrFieldsParser.parse(() -> {
//                return ConfigFileReader.FILE_SCHEMA.getContent(domain);
//            });
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

    /**
     * 创建执行链
     *
     * @param chainContext
     * @return
     */
    public static ActionInvocation createExecChain(DefaultChainContext chainContext) throws Exception {
        IExecuteInterceptor[] ints = null;

        if (!chainContext.hasIndexName() && chainContext.getWorkflowId() != null) {


            ints = new IExecuteInterceptor[]{new WorkflowDumpAndJoinInterceptor()};


            //console中直接用workflow触发
//            throw new NotImplementedException("console中直接用workflow触发");

        } else if (chainContext.hasIndexName()) {

            // IBasicAppSource appSource = chainContext.getAppSource();

            ints = dataXBuild;
            // appSource.accept(new IBasicAppSource.IAppSourceVisitor<IExecuteInterceptor[]>() {
//                @Override
//                public IExecuteInterceptor[] visit(DataxProcessor app) {
//                    return dataXBuild;
//                }
//
//                @Override
//                public IExecuteInterceptor[] visit(ISingleTableAppSource single) {
//                    //   return visitSolrAppSource(single);
//                    throw new UnsupportedOperationException();
//                }
//
//                @Override
//                public IExecuteInterceptor[] visit(IDataFlowAppSource dataflow) {
//                    // return visitSolrAppSource(dataflow);
//                    throw new UnsupportedOperationException();
//                }
//
//                // private IExecuteInterceptor[] visitSolrAppSource(ISolrAppSource appSource) {
//
////                    Objects.requireNonNull(chainContext.getIndexBuildFileSystem(), "IndexBuildFileSystem of chainContext can not be null");
////                    Objects.requireNonNull(chainContext.getTableDumpFactory(), "tableDumpFactory of chainContext can not be null");
////                    // chainContext.setIndexMetaData(createIndexMetaData(chainContext));
////
////                    EntityName targetEntity = appSource.getTargetEntity();
////                    chainContext.setAttribute(IExecChainContext.KEY_BUILD_TARGET_TABLE_NAME, targetEntity);
////                    IPrimaryTabFinder pTabFinder = appSource.getPrimaryTabFinder();
////                    chainContext.setAttribute(IFullBuildContext.KEY_ER_RULES, pTabFinder);
////                    return workflowBuild;
//                //  throw new UnsupportedOperationException();
//                //}
//
//
//            });


        } else {
//            if ("true".equalsIgnoreCase(chainContext.getString(COMMAND_KEY_DIRECTBUILD))) {
//                ints = directBuild;
//            } else {
            // ints = fullints;
            throw new UnsupportedOperationException();
            //}
        }
        Integer taskid = chainContext.getTaskId();
        TrackableExecuteInterceptor.initialTaskPhase(taskid);
        return createInvocation(chainContext, ints);
    }


    public static ActionInvocation createInvocation(IExecChainContext chainContext, IExecuteInterceptor[] ints) {
        final ComponentOrders componentOrders = new ComponentOrders();
        AbstractActionInvocation preInvocation = new AbstractActionInvocation();
        preInvocation.setContext(chainContext);
        preInvocation.setComponentOrders(componentOrders);
        AbstractActionInvocation invocation = null;
        for (int i = (ints.length - 1); i >= 0; i--) {
            for (FullbuildPhase phase : ints[i].getPhase()) {
                componentOrders.put(phase, i);
            }
            invocation = new AbstractActionInvocation();
            invocation.setComponentOrders(componentOrders);
            invocation.setContext(chainContext);
            invocation.setInterceptor(ints[i]);
            invocation.setSuccessor(preInvocation);
            preInvocation = invocation;
        }
        logger.info("component description:");
        for (Map.Entry<FullbuildPhase, Integer> componentEntry : componentOrders.entrySet()) {
            logger.info(componentEntry.getKey() + ":" + componentEntry.getValue());
        }
        logger.info("description end");
        return preInvocation;
    }

    private ComponentOrders componentOrders;

    public static class ComponentOrders {

        private final Map<FullbuildPhase, Integer> orders;

        public ComponentOrders() {
            super();
            this.orders = Maps.newHashMap();
        }

        public Integer get(FullbuildPhase key) {
            Integer index = this.orders.get(key);
            if (index == null) {
                throw new IllegalStateException("key:" + key + " can not find relevant map keys["
                        + orders.keySet().stream().map((r) -> r.getName()).collect(Collectors.joining(",")) + "]");
            }
            return index;
        }

        public Set<Entry<FullbuildPhase, Integer>> entrySet() {
            return orders.entrySet();
        }

        public Integer put(FullbuildPhase key, Integer value) {
            return orders.put(key, value);
        }
    }

    @Override
    public ExecuteResult invoke() throws Exception {
        if (componentOrders == null) {
            throw new IllegalStateException("componentOrders can not be null");
        }
        ExecutePhaseRange phaseRange = chainContext.getExecutePhaseRange();
        // String start = chainContext.getString(IFullBuildContext.COMPONENT_START);
        // String end = chainContext.getString(IFullBuildContext.COMPONENT_END);
        int startIndex = Integer.MIN_VALUE;
        int endIndex = Integer.MAX_VALUE;
        startIndex = componentOrders.get(phaseRange.getStart());
        endIndex = componentOrders.get(phaseRange.getEnd());
        if (interceptor == null) {
            return ExecuteResult.SUCCESS;
        } else {
            int current;
            try {
                current = componentOrders.get(FullbuildPhase.getFirst(interceptor.getPhase()));
            } catch (Throwable e) {
                throw new IllegalStateException("component:" + FullbuildPhase.desc(interceptor.getPhase()) + " can not find value in componentOrders," + componentOrders.toString());
            }
            if (current >= startIndex && current <= endIndex) {
                logger.info("execute " + FullbuildPhase.desc(interceptor.getPhase()) + ":" + current + "[" + startIndex + "," + endIndex + "]");
                return interceptor.intercept(successor);
            } else {
                // 直接跳过
                return successor.invoke();
            }
        }
    }

    public ComponentOrders getComponentOrders() {
        return componentOrders;
    }

    public void setComponentOrders(ComponentOrders componentOrders) {
        this.componentOrders = componentOrders;
    }


    @Override
    public IExecChainContext getContext() {
        return this.chainContext;
    }

    private IExecChainContext chainContext;

    private IExecuteInterceptor interceptor;

    private ActionInvocation successor;

    public void setSuccessor(ActionInvocation successor) {
        this.successor = successor;
    }

    public IExecuteInterceptor getSuccessor() {
        return interceptor;
    }

    public void setInterceptor(IExecuteInterceptor successor) {
        this.interceptor = successor;
    }

    public void setContext(IExecChainContext action) {
        this.chainContext = action;
    }


}
