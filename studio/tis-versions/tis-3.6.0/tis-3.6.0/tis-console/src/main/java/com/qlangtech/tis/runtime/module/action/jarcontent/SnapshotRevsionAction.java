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
//package com.qlangtech.tis.runtime.module.action.jarcontent;
//
//import com.alibaba.citrus.turbine.Context;
//import com.koubei.web.tag.pager.Pager;
//import com.qlangtech.tis.coredefine.module.action.CoreAction;
//import com.qlangtech.tis.manage.PermissionConstant;
//import com.qlangtech.tis.manage.biz.dal.pojo.*;
//import com.qlangtech.tis.manage.common.AppDomainInfo;
//import com.qlangtech.tis.manage.common.DefaultOperationDomainLogger;
//import com.qlangtech.tis.manage.common.RunContext;
//import com.qlangtech.tis.manage.common.SnapshotDomain;
//import com.qlangtech.tis.manage.spring.aop.Func;
//import com.qlangtech.tis.pubhook.common.Nullable;
//import com.qlangtech.tis.pubhook.common.RunEnvironment;
//import com.qlangtech.tis.runtime.module.action.BasicModule;
//import com.qlangtech.tis.runtime.pojo.ResSynManager;
//import com.qlangtech.tis.runtime.pojo.ResSynManager.CompareResult;
//import junit.framework.Assert;
//import org.apache.commons.lang.StringUtils;
//import org.apache.solr.common.cloud.DocCollection;
////import org.apache.solr.common.cloud.TISZkStateReader;
//import org.json.JSONArray;
//import org.json.JSONObject;
//import java.text.SimpleDateFormat;
//import java.util.Arrays;
//import java.util.List;
//
///**
// * @author 百岁（baisui@qlangtech.com）
// * @date 2020/04/13
// */
//public class SnapshotRevsionAction extends BasicModule {
//
//    private static final long serialVersionUID = 1L;
//
//    public SnapshotRevsionAction() {
//        super("selectRevsion");
//    }
//
//    /**
//     * @param context
//     */
//    public void doGetSnapshotList(Context context) {
//        AppDomainInfo domain = this.getAppDomain();
//        if (domain instanceof Nullable) {
//            this.addErrorMessage(context, "has not select app");
//            // 是否已经选择了域
//            return;
//        }
//        context.put("hasselectdomain", true);
//        Pager pager = this.createPager();
//        SnapshotCriteria query = new SnapshotCriteria();
//        // 一览先不考虑有 版本设置
//        query.createCriteria().andAppidEqualTo(domain.getAppid());
//        query.setOrderByClause("snapshot.sn_id desc");
//        query.setPageSize(PAGE_SIZE);
//        pager.setTotalCount(this.getSnapshotDAO().countByExample(query));
//        ServerGroup group = getAppServerGroup();
//        SnapshotList snapshotList = new SnapshotList();
//        snapshotList.publishSnapshotid = group.getPublishSnapshotId();
//        snapshotList.snapshotList = this.getSnapshotDAO().selectByExample(query, pager.getCurPage(), PAGE_SIZE);
//        this.setBizResult(context, new PaginationResult(pager, snapshotList.snapshotList, snapshotList.publishSnapshotid));
//        Assert.assertNotNull("group can not be null,app:" + domain.getAppName() + ", runtime:" + domain.getRunEnvironment().getKeyName(), group);
//    // context.put("selectgroup", group);
//    }
//
//    public static class SnapshotList {
//
//        List<Snapshot> snapshotList;
//
//        Integer publishSnapshotid;
//
//        public List<Snapshot> getSnapshotList() {
//            return snapshotList;
//        }
//
//        public Integer getSnapshotid() {
//            return publishSnapshotid;
//        }
//    }
//
//    /*
//   * 与doSelectRevsion的区别在于，源参数是页面传递， doSelectRevsionByContext的参数是有http接口传入
//   */
//    public void doSelectRevsionByContext(Context context) {
//        Integer snapshotid = (Integer) context.get("selectedSnapshotid");
//        final String memo = "drds配置更新";
//        final ServerGroup group = getAppServerGroup();
//        Assert.assertNotNull(group);
//        final AppDomainInfo domain = this.getAppDomain();
//        if (!StringUtils.equals(this.getString("appname"), domain.getAppName())) {
//            this.addErrorMessage(context, "执行的应用：“" + this.getString("appname") + "”，与当前系统应用“" + domain.getAppName() + "”不一致,请关闭当前页面重新打开");
//            return;
//        }
//        if (group.getPublishSnapshotId() != null && snapshotid == (int) group.getPublishSnapshotId()) {
//            this.addErrorMessage(context, "SNAPSHOT已经设置为:" + snapshotid + ",请重新设置");
//            return;
//        }
//        Application app = new Application();
//        app.setAppId(domain.getAppid());
//        app.setProjectName(domain.getAppName());
//        change2newSnapshot(snapshotid, memo, group, app, domain.getRunEnvironment(), this);
//        addActionMessage(context, "已经将<strong style='background-color:yellow'>" + domain.getRunEnvironment().getDescribe() + "</strong>的应用<strong style='background-color:yellow'>" + domain.getAppName() + "</strong>的第<strong style='background-color:yellow'>" + group.getGroupIndex() + "组</strong>服务器，的发布快照设置成了snapshot：<strong style='background-color:yellow'>" + snapshotid + "</strong>");
//    }
//
//    @Func(value = PermissionConstant.CONFIG_SNAPSHOT_CHANGE)
//    public void doSelectRevsion(Context context) throws Exception {
//        this.errorsPageShow(context);
//        Integer snapshotid = this.getInt("selectedSnapshotid");
//        // 只作数据库保存，不推送到远到引擎中
//        boolean justSave = this.getBoolean("justSave");
//        // final Integer groupid = this.getInt("groupid");
//        final String memo = this.getString("memo");
//        if (snapshotid == null) {
//            throw new IllegalArgumentException("snapshotid can not be null");
//        }
//        if (StringUtils.isBlank(memo)) {
//            this.addErrorMessage(context, "请填写操作日志");
//            return;
//        }
//        SnapshotDomain toSnapshot = this.getSnapshotViewDAO().getView(snapshotid);
//        if (toSnapshot == null) {
//            this.addErrorMessage(context, "版本号:" + snapshotid + "对应的配置已经被删除");
//            return;
//        }
//        final ServerGroup group = getAppServerGroup();
//        Assert.assertNotNull(group);
//        final AppDomainInfo domain = this.getAppDomain();
//        if (domain.getAppid() != (toSnapshot.getAppId() + 0)) {
//            this.addErrorMessage(context, "执行的实例与当前实例“" + domain.getAppName() + "”不一致,请关闭当前页面重新打开");
//            return;
//        }
//        if (group.getPublishSnapshotId() != null && snapshotid == (int) group.getPublishSnapshotId()) {
//            this.addErrorMessage(context, "SNAPSHOT已经设置为:" + snapshotid + ",请重新设置");
//            return;
//        }
//        Application app = new Application();
//        app.setAppId(domain.getAppid());
//        app.setProjectName(domain.getAppName());
//        change2newSnapshot(snapshotid, memo, group, app, domain.getRunEnvironment(), this);
//        if (!justSave) {
//            DocCollection docCollection = TISZkStateReader.getCollectionLive(this.getZkStateReader(), this.getCollectionName());
//            CoreAction.pushConfig2Engine(this, context, docCollection, snapshotid, false);
//        }
//        addActionMessage(context, "已经将" + domain.getRunEnvironment().getDescribe() + "" + domain.getAppName() + "的配置切换成了snapshot:" + snapshotid);
//    }
//
//    /**
//     * 取得最新的快照记录
//     *
//     * @param context
//     */
//    @Func(PermissionConstant.CONFIG_SNAPSHOT_CHANGE)
//    public void doGetLatestSnapshot(Context context) throws Exception {
//        Integer snapshotId = this.getInt("maxsnapshotid");
//        SnapshotCriteria criteria = new SnapshotCriteria();
//        criteria.createCriteria().andSnIdGreaterThan(snapshotId).andAppidEqualTo(this.getAppDomain().getAppid());
//        final SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
//        JSONArray jsonArray = new JSONArray();
//        JSONObject obj = null;
//        for (Snapshot snapshot : this.getSnapshotDAO().selectByExample(criteria, 1, 20)) {
//            obj = new JSONObject();
//            obj.put("snid", snapshot.getSnId());
//            obj.put("createtime", format.format(snapshot.getCreateTime()));
//            obj.put("creator", snapshot.getCreateUserName());
//            obj.put("parent", snapshot.getPreSnId());
//            obj.put("memo", StringUtils.trimToEmpty(snapshot.getMemo()));
//            jsonArray.put(obj);
//        }
//        context.put("query_result", jsonArray.toString(1));
//    }
//
//    /**
//     * 比较两个snapshot快照之间的区别
//     *
//     * @param context
//     * @throws Exception
//     */
//    public void doGetCompareResult(Context context) throws Exception {
//        this.disableNavigationBar(context);
//        String[] snid = this.getRequest().getParameterValues("comparesnapshotid");
//        if (snid.length != 2) {
//            throw new IllegalArgumentException("param comparesnapshotid lenght is not equal to 2," + snid.length);
//        }
//        int[] snids = new int[2];
//        snids[0] = Integer.parseInt(snid[0]);
//        snids[1] = Integer.parseInt(snid[1]);
//        Arrays.sort(snids);
//        ResSynManager synManager = // /////// old
//        ResSynManager.create(// /////// old
//        this.getAppDomain().getAppName(), // new
//        this.getSnapshotViewDAO().getView(snids[0]), this.getSnapshotViewDAO().getView(snids[1]), this);
//        // context.put("synManager", synManager);
//        // context.put("differ", synManager.diff());
//        SnapshotCompareResult result = new SnapshotCompareResult(synManager, synManager.diff());
//        this.setBizResult(context, result);
//    }
//
//    public static class SnapshotCompareResult {
//
//        private final ResSynManager synManager;
//
//        private final List<CompareResult> results;
//
//        /**
//         * @param synManager
//         * @param results
//         */
//        public SnapshotCompareResult(ResSynManager synManager, List<CompareResult> results) {
//            super();
//            this.synManager = synManager;
//            this.results = results;
//        }
//
//        public boolean isHasDifferentRes() {
//            return synManager.isHasDifferentRes();
//        }
//
//        public Integer getSnapshotId() {
//            return this.synManager.getDailyRes().getSnapshot().getSnId();
//        }
//
//        public Integer getSnapshotOtherId() {
//            return this.synManager.getOnlineResDomain().getSnapshot().getSnId();
//        }
//
//        public List<CompareResult> getResults() {
//            return results;
//        }
//    }
//
//    public static // final AppDomainInfo domain,
//    void change2newSnapshot(// final AppDomainInfo domain,
//    Integer snapshotid, // final AppDomainInfo domain,
//    final String memo, // final AppDomainInfo domain,
//    ServerGroup group, // final AppDomainInfo domain,
//    Application app, RunEnvironment runEnvironment, RunContext runContext) {
//        // 更新group的PublishSnapshotId
//        ServerGroupCriteria groupCriteria = new ServerGroupCriteria();
//        groupCriteria.createCriteria().andAppIdEqualTo(app.getAppId()).andRuntEnvironmentEqualTo(runEnvironment.getId()).andGidEqualTo(group.getGid());
//        ServerGroup ugroup = new ServerGroup();
//        ugroup.setPublishSnapshotId(snapshotid);
//        DefaultOperationDomainLogger logger = new DefaultOperationDomainLogger();
//        logger.setAppName(app.getProjectName());
//        logger.setMemo("将Snapshot由" + group.getPublishSnapshotId() + "切换成" + snapshotid + "," + memo);
//        logger.setRuntime(runEnvironment.getId());
//        ugroup.setLogger(logger);
//        runContext.getServerGroupDAO().updateByExampleSelective(ugroup, groupCriteria);
//    }
//}
