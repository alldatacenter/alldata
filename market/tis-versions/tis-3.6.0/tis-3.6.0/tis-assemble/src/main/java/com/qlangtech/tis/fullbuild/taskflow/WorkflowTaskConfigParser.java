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
package com.qlangtech.tis.fullbuild.taskflow;

import com.google.common.collect.Lists;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.fullbuild.phasestatus.impl.JoinPhaseStatus;
import com.qlangtech.tis.fullbuild.taskflow.impl.EndTask;
import com.qlangtech.tis.fullbuild.taskflow.impl.ForkTask;
import com.qlangtech.tis.fullbuild.taskflow.impl.StartTask;
import com.qlangtech.tis.git.GitUtils;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 全量构建中打宽表，任务下发
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
@Deprecated
public class WorkflowTaskConfigParser {

    private static final DocumentBuilderFactory schemaDocumentBuilderFactory = DocumentBuilderFactory.newInstance();

    private static final XPathFactory xpathFactory = XPathFactory.newInstance();

    public static final MessageFormat PT_DATE_FORMAT = new MessageFormat("{0}-{1}");

    // private static final Connection HIVE_Connection =
    // HiveDBUtils.getInstance().createConnection();
    private static final Logger log = LoggerFactory.getLogger(WorkflowTaskConfigParser.class);

    static {
        schemaDocumentBuilderFactory.setValidating(false);
    }

    // 总共保留多少个历史partition
    private int partitionSaveCount = 4;

    private final ITaskFactory taskFactory;

    private final IJoinRuleStreamGetter joinRuleGetter;

    private TemplateContext tplContext;

    // work功能点执行日志收集
    private final JoinPhaseStatus joinPhaseStatus;

    WorkflowTaskConfigParser(ITaskFactory hiveTaskFactory, IJoinRuleStreamGetter joinRuleGetter, JoinPhaseStatus joinPhaseStatus) {
        super();
        this.taskFactory = hiveTaskFactory;
        this.joinRuleGetter = joinRuleGetter;
        this.joinPhaseStatus = joinPhaseStatus;
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static WorkflowTaskConfigParser getInstance(IExecChainContext context, final JoinPhaseStatus joinPhaseStatus) {
        throw new UnsupportedOperationException();
    // if (joinPhaseStatus == null) {
    // throw new IllegalArgumentException("param joinPhaseStatus can not be null");
    // }
    // return new WorkflowTaskConfigParser(new HiveTaskFactory(context.getERRules().getTabFieldProcessorMap()), (execContext) -> {
    // // protected String getdJoinRuleStream(IExecChainContext
    // // execContext) {
    // if (execContext == null || StringUtils.isEmpty(execContext.getIndexName())) {
    // throw new IllegalArgumentException("param index can not be null");
    // }
    //
    // // 通过workflowid 得到workflow的配置信息
    // String url = WorkflowDumpAndJoinInterceptor.WORKFLOW_CONFIG_URL_FORMAT
    // .format(new Object[]{"fullbuild_workflow_action", "do_get_join_task",
    // String.format("&index_name=%s", execContext.getIndexName())});
    // try {
    // JSONObject jsonObject = ConfigFileContext.processContent(new URL(url),
    // new ConfigFileContext.StreamProcess<JSONObject>() {
    // @Override
    // public JSONObject p(int status, InputStream stream, Map<String, List<String>> headerFields) {
    // try {
    // return new JSONObject(IOUtils.toString(stream, "utf8"));
    // } catch (IOException e) {
    // throw new RuntimeException(e);
    // }
    // }
    // });
    // return jsonObject.getString("task");
    // } catch (MalformedURLException e) {
    // throw new RuntimeException(e);
    // }
    // // }
    // }, joinPhaseStatus);
    }

    /**
     * 通过workflow名称取得wf實例
     *
     * @param workflowName
     * @param branch
     * @return
     */
    public static WorkflowTaskConfigParser getInstance(String workflowName, GitUtils.GitBranchInfo branch) {
        // }, joinPhaseStatus);
        throw new UnsupportedOperationException();
    }

    /**
     * 开始打宽表任务
     *
     * @param
     * @throws Exception the exception
     */
    public void startJoinSubTables(TemplateContext tplContext) throws Exception {
    // HiveTaskFactory.startTaskInitialize(tplContext);
    // Map<String, Object> params = new HashMap<>();
    // try {
    // this.setTplContext(tplContext);
    //
    // String joinTask = this.joinRuleGetter.getTaskContent(tplContext.getParams());
    //
    // TaskWorkflow taskList = this.parseTask(joinTask);
    //
    // taskList.startExecute(params);
    // } finally {
    // // 关闭连接
    // this.taskFactory.postReleaseTask(tplContext);
    // }
    }

    /**
     * 取得工作流模型
     *
     * @return
     * @throws Exception
     */
    public TaskWorkflow getWorkflow() throws Exception {
        String joinTask = this.joinRuleGetter.getTaskContent(tplContext.getExecContext());
        Pattern p = Pattern.compile("\\$\\{.?+\\}");
        Matcher m = p.matcher(joinTask);
        TaskWorkflow taskList = this.parseTask(m.replaceAll("test"));
        return taskList;
    }

    public static interface ProcessTask {

        public abstract void process(ITask task);
    }

    /**
     * Traverse task.
     */
    private void traverseTask(IExecChainContext execContext, ProcessTask taskProcess) throws Exception {
        TaskWorkflow tasks = parseTask(this.joinRuleGetter.getTaskContent(execContext));
        for (ITask task : tasks.getAllTask()) {
            taskProcess.process(task);
        }
    }

    private TaskWorkflow parseTask(String taskContent) throws Exception {
        DocumentBuilder builder = schemaDocumentBuilderFactory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> {
            InputSource source = new InputSource();
            source.setCharacterStream(new StringReader(""));
            return source;
        });
        Document document = builder.parse(new ByteArrayInputStream(taskContent.getBytes()));
        final XPath xpath = xpathFactory.newXPath();
        setPartitionSaveCount(document, xpath);
        final String expression = "/execute/task|/execute/fork|/execute/start|/execute/end" + "|/execute/joinTask|/execute/unionTask";
        NodeList nodes = (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);
        return parse(nodes);
    // return null;
    }

    /**
     * @param document
     * @param xpath
     * @throws XPathExpressionException
     */
    private void setPartitionSaveCount(Document document, final XPath xpath) throws XPathExpressionException {
        Node execNode = (Node) xpath.evaluate("/execute", document, XPathConstants.NODE);
        String partitionSaveCount = getAttr(execNode, "partitionSaveCount", null, true);
        if (StringUtils.isNotBlank(partitionSaveCount)) {
            this.partitionSaveCount = Integer.parseInt(partitionSaveCount);
        }
    }

    // private static final Pattern SPACE = Pattern.compile("\\s+");
    /**
     * Parse list.
     *
     * @param nodes the nodes
     * @return the list
     * @throws Exception the exception
     */
    public TaskWorkflow parse(NodeList nodes) throws Exception {
        // List<ITask> tasks = new ArrayList<ITask>();
        TaskWorkflow taskManager = new TaskWorkflow();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            final String taskname = getTaskName(node);
            // final String successTo = this.getSuccessTo(node);
            BasicTask task = null;
            if ("start".equals(node.getNodeName())) {
                task = new StartTask();
            } else if ("end".equals(node.getNodeName())) {
                task = new EndTask();
            } else if ("fork".equals(node.getNodeName())) {
                task = new ForkTask();
            } else if ("task".equals(node.getNodeName())) {
            // task = createTask(node, taskname);
            } else if ("joinTask".equals(node.getNodeName())) {
            // task = taskFactory.createJoinTask(node.getTextContent(), this.getTplContext(),
            // joinPhaseStatus.getTaskStatus(taskname));
            } else if ("unionTask".equals(node.getNodeName())) {
            // task = taskFactory.createUnionTask(node, this.getTplContext(), joinPhaseStatus.getTaskStatus(taskname));
            } else {
                throw new IllegalStateException("illegal node name:" + node.getNodeName());
            }
            // task.setDependencyTables(getRequiredTables(node));
            task.setName(taskname);
            // task.setSuccessTo(successTo);
            taskManager.addTask(task);
        }
        taskManager.init();
        return taskManager;
    }

    private List<EntityName> getRequiredTables(Node node) {
        List<EntityName> dumpTables = Lists.newArrayList();
        String requiredTables = getAttr(node, "requiredTables", "requiredTables", true);
        String[] tables = StringUtils.split(requiredTables, ",");
        for (String t : tables) {
            dumpTables.add(EntityName.parse(t));
        }
        return dumpTables;
    }

    protected String getTaskName(Node node) {
        return getAttr(node, "name", "name", true);
    }

    private String getSuccessTo(Node node) {
        return getAttr(node, "to", "to", true);
    }

    /**
     * @param
     * @param task
     */
    private void setTaskName(BasicTask task, String taskname) {
        task.setName(taskname);
    }

    /**
     * Gets attr.
     *
     * @param node        the node
     * @param name        the name
     * @param missing_err the missing err
     * @return the attr
     */
    public static String getAttr(Node node, String name, String missing_err) {
        return getAttr(node, name, missing_err, false);
    }

    /**
     * Gets attr.
     *
     * @param node        the node
     * @param name        the name
     * @param missing_err the missing err
     * @param ignoreNull  the ignore null
     * @return the attr
     */
    public static String getAttr(Node node, String name, String missing_err, boolean ignoreNull) {
        NamedNodeMap attrs = node.getAttributes();
        Node attr = attrs == null ? null : attrs.getNamedItem(name);
        if (!ignoreNull && attr == null) {
            if (missing_err == null)
                return null;
            throw new RuntimeException(missing_err + ": missing mandatory attribute '" + name + "'");
        }
        if (attr == null) {
            return null;
        }
        return attr.getNodeValue();
    }

    // /**
    // * @param node
    // * @return
    // */
    // private BasicTask createTask(Node node, String taskname) {
    // return taskFactory.createTask(node.getTextContent(), this.getTplContext(),
    // this.joinPhaseStatus.getTaskStatus(taskname));
    // }
    /**
     * Gets tpl context.
     *
     * @return the tpl context
     */
    public TemplateContext getTplContext() {
        return tplContext;
    }

    /**
     * Joiner 配置文件获取
     */
    public void setTplContext(TemplateContext tplContext) {
        this.tplContext = tplContext;
    }

    public static interface IJoinRuleStreamGetter {

        public String getTaskContent(IExecChainContext execContext);
    }

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     * @throws Exception the exception
     */
    public static void main(String[] args) throws Exception {
    }
}
