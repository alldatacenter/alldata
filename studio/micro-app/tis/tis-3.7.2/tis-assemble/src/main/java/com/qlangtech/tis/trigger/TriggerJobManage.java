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

package com.qlangtech.tis.trigger;

import com.google.common.collect.Sets;
import com.qlangtech.tis.ajax.AjaxResult;
import com.qlangtech.tis.cloud.ITISCoordinator;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.ConfigFileContext;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.pubhook.common.IPreTriggerProcess;
import com.qlangtech.tis.pubhook.common.RunEnvironment;
import com.qlangtech.tis.realtime.utils.NetUtils;
import com.qlangtech.tis.trigger.biz.dal.dao.AppTrigger;
import com.qlangtech.tis.trigger.biz.dal.dao.IJobMetaDataDAO;
import com.qlangtech.tis.trigger.biz.dal.dao.ITerminatorTriggerBizDalDAOFacade;
import com.qlangtech.tis.trigger.biz.dal.dao.JobConstant;
import com.qlangtech.tis.trigger.biz.dal.pojo.TriggerJob;
import com.qlangtech.tis.trigger.biz.dal.pojo.TriggerJobCriteria;
import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.SessionExpiredException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;

//import com.qlangtech.tis.TisZkClient;

public class TriggerJobManage implements TriggerJobConsole, InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(TriggerJobManage.class);
    public static final int JETTY_TRIGGER_SERVER_PORT = 8080;
    private IJobMetaDataDAO jobMetaDataDAO;
    private ITerminatorTriggerBizDalDAOFacade triggerBizDAO;
    private IPreTriggerProcess preTriggerProcess;

    public static final String JOB_SCHEDULE = "jobSchedule";
    public static final String JOB_TRIGGER_SERVER = "jobTriggerServer";

    public static final String GROUP_PREFIX = "group";

    private static final String YEAR_OF_2099 = "2099";

    public static final String NERVER_WILL_TRIGGER_CRONTAB = "0 0 10 * * ? " + YEAR_OF_2099;

    @Override
    public void afterPropertiesSet() throws Exception {

        shnchronizeCrontabConfig();
    }

    public ITerminatorTriggerBizDalDAOFacade getTriggerBizDAO() {
        return this.triggerBizDAO;
    }

    public void setTriggerBizDAO(ITerminatorTriggerBizDalDAOFacade triggerBizDAO) {
        this.triggerBizDAO = triggerBizDAO;
    }

    // public void setTriggerServerRegister(
    // TriggerServerRegister triggerServerRegister) {
    // this.triggerServerRegister = triggerServerRegister;
    // }
    //
    public ITISCoordinator getZookeeper() {
        // return this.zkClientGetter.getSolrZkClient().unwrap();
        return null;
    }

    private static final Scheduler scheduler;

    static {
        try {
            if (RunEnvironment.isOnlineMode()) {
                scheduler = StdSchedulerFactory.getDefaultScheduler();
                scheduler.start();
            } else {
                scheduler = null;
            }
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 创建一个触发器
     *
     * @param triggerIndexName
     * @param schedule
     * @return
     */
    private CronTrigger createTrigger(String triggerIndexName, JobSchedule schedule) {

        final String triggerGroup = getGroupName(schedule.getIndexName());
        final String crontab = schedule.isPaused() ? NERVER_WILL_TRIGGER_CRONTAB
                : schedule.getCrobexp();

        return TriggerBuilder.newTrigger().withIdentity(triggerIndexName, triggerGroup).startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule(crontab)).build();
    }

    private static final String TRIGGER_JOB_KEY_PREFIX = "trigger_";

    /**
     * 添加一个job连接管道
     *
     * @param
     * @param
     */
    private void createNewFullDumpTrigger(JobSchedule schedule// , Connection
                                          // conn
    ) {
        try {

            final JobKey jobkey = createJobKey(schedule);

            final JobDetail job = JobBuilder
                    .newJob(QuartzTriggerJob.class)
                    .withIdentity(jobkey).build();

            log.debug("job.getKey():" + job.getKey());

            // job.getJobDataMap().put(CONNECTION, conn);
            job.getJobDataMap().put(JOB_SCHEDULE, schedule);
            job.getJobDataMap().put(JOB_TRIGGER_SERVER, this);

            // Trigger the job to run now, and then repeat every 40 seconds
            Trigger trigger = createTrigger(TRIGGER_JOB_KEY_PREFIX + schedule.getJobid(), schedule);

            Map<JobDetail, Set<? extends Trigger>> triggersAndJobs = new HashMap<>();
            triggersAndJobs.put(job, Sets.newHashSet(trigger));
            scheduler.scheduleJobs(triggersAndJobs, true);

        } catch (ObjectAlreadyExistsException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private JobKey createJobKey(JobSchedule schedule) {
        return createJobKey(schedule.getIndexName());
    }

    private JobKey createJobKey(String indexName) {
        return new JobKey("job" + indexName, getGroupName(indexName));
    }

    public String getGroupName(String indexName) {
        return GROUP_PREFIX + indexName;
    }

    /**
     * 取得最近一次的执行时间的timestamp
     *
     * @return
     */
    private long parseLatestExecuteTimeStamp(byte[] content) throws JSONException {
        JSONTokener tokener = new JSONTokener(new String(content));
        JSONObject json = new JSONObject(tokener);
        return Long.parseLong(json.getString(TIMESTAMP_PROPERTY));
    }

    private final String TIMESTAMP_PROPERTY = "timestamp";

    private byte[] parseCurrnetTimeStamp(Date date) throws JSONException {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmm");
        JSONObject result = new JSONObject();
        result.put("view_time", format.format(date));
        result.put(TIMESTAMP_PROPERTY, String.valueOf(date.getTime()));
        result.put("execute_ip", NetUtils.getHost());

        return result.toString().getBytes();
    }

    private final int COLLECT_STATE_INTERVAL = 1;

    /**
     * 判断之前是否有任何任务正在执行,两分钟锁失效
     *
     * @return
     */
    private boolean hasAnyDumpTaskExecuting(String indexName) {
        return false;
    }

    /**
     * 取得任务执行信息
     *
     * @param indexName
     * @return
     * @throws MalformedURLException
     * @throws SessionExpiredException
     */
    public TriggerTaskConfig getTaskConfigFromRepository(final String indexName)
            throws MalformedURLException, SessionExpiredException {

        TriggerTaskConfig config = new TriggerTaskConfig();
        config.setAppName(indexName);
        config.setTaskId(123);

        if (!isTest() && hasAnyDumpTaskExecuting(config.getAppName())) {
            addFeedbackInfo(config, InfoType.ERROR, "indexname:" + config.getAppName()
                    + " task center has same task is executing。");
            return null;
        }

        if (!isTest() && !hasGrantCollectLock(config.getAppName())) {
            addFeedbackInfo(config, InfoType.ERROR,
                    "indexname:" + config.getAppName() + " has not get the execute task。");
            // 是否拿到执行锁
            return null;
        }

        log.info("get json from tis\n taskid:" + config.getTaskId() + "\n"
                + TriggerTaskConfig.serialize(config));
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {

        }

        addFeedbackInfo(config, InfoType.INFO,
                "indexname:" + config.getAppName() + "get json from tis\n taskid:"
                        + config.getTaskId() + "\n" + TriggerTaskConfig.serialize(config));

        return config;
    }

    /**
     * 执行全量任务
     *
     * @param indexName
     * @param execType
     * @param context
     * @throws Exception
     */
    public final void triggerFullDump(String indexName, ExecType execType, ITriggerContext context)
            throws Exception {
        TriggerTaskConfig triggerConfig = getTaskConfigFromRepository(indexName);
        if (triggerConfig == null) {
            log.error("triggerConfig == null,so exist");
            return;
        }

        if (preTriggerProcess != null) {
            log.info("preTriggerProcess.process()");
            preTriggerProcess.process();
        }

        log.info("start to execute triggerFullDump,indexName:" + indexName);
        triggerFullDump(triggerConfig, execType, context);
    }

    public void triggerFullDump(final TriggerTaskConfig config, ExecType execType,
                                ITriggerContext triggerContext) throws Exception {
        log.info("trigger index " + config.getAppName() + " start");


        LockResult lock = getFullDumpNode(this.getZookeeper(), config.getAppName());
        if (lock == null) {
            log.info("trigger task is not exist " + config.getAppName());
            return;
        }

        final URL url = new URL(
                "http://" + lock.getContent() + "/trigger?appname=" + config.getAppName());
        log.info("trigger url:" + url);
        HttpUtils.processContent(url, new ConfigFileContext.StreamProcess<Long>() {
            @Override
            public Long p(int status, InputStream stream, Map<String, List<String>> headerFields) {
                log.info("trigger index " + config.getAppName() + " success");
                return 0l;
            }
        });
    }

    private static final MessageFormat GRANTED_LOCK_CLIENT_IP = new MessageFormat(
            "/tis-lock/dumpindex/{0}/dumper");

    public static LockResult getFullDumpNode(ITISCoordinator zk, String collection) throws Exception {
        final String ipLockPath = GRANTED_LOCK_CLIENT_IP.format(new Object[]{collection});
        if (!zk.exists(ipLockPath, true)) {
            return null;
        }

        LockResult lock = getNodeInfo(zk, true/* hasChild */, ipLockPath, false, "全量Dump节点");

        if (lock.childValus.isEmpty()) {
            return null;
        }

        return lock;
    }

    public static LockResult getNodeInfo(ITISCoordinator zk, boolean hasChild, String path,
                                         boolean editable, String desc)
            throws KeeperException, InterruptedException, UnsupportedEncodingException {

        return getNodeInfo(zk, hasChild/* haChild */, path, editable, new PathValueProcess() {
            @Override
            public String process(String path, ITISCoordinator zk, LockResult lock)
                    throws KeeperException, InterruptedException {

                String child = new String(zk.getData(path, true));
                lock.addChildValue(child);
                return child;
            }
        }, desc);
    }

    public static final LockResult NULL_LOCK;

    static {
        NULL_LOCK = new LockResult(false);
        NULL_LOCK.setContent("不存在。。");
    }

    public static LockResult getNodeInfo(ITISCoordinator zk, boolean haChild, String path,
                                         boolean editable, PathValueProcess process, String desc)
            throws KeeperException, InterruptedException, UnsupportedEncodingException {

        LockResult lock = new LockResult(editable);
        lock.setPath(path);
        // lock.setZkAddress(String.valueOf(zk.getZkServerAddress()));
        lock.setDesc(desc);

        if (zk.exists(path, true)) {
            if (!haChild) {
                lock.setContent(process.process(path, zk, lock));// (path,
            } else {
                List<String> child = zk.getChildren(path, true);
                StringBuffer buffer = new StringBuffer();
                for (String n : child) {
                    buffer.append(new String(process.process(path + "/" + n, zk, lock)));
                }
                lock.setContent(buffer.toString());
            }
        } else {
            lock.setContent(NULL_LOCK.getContent());
        }
        return lock;
    }

    public interface PathValueProcess {
        String process(String path, ITISCoordinator zk, LockResult lock)
                throws KeeperException, InterruptedException;
    }

    public void addFeedbackInfo(TriggerTaskConfig triggerConfig, InfoType infoType, String info) {

    }

    private static boolean isTest() {
        return "true".equalsIgnoreCase(System.getProperty("test"));
    }

    /*
     * 从配置中心同步crontab的定义
     */
    protected void shnchronizeCrontabConfig() {

        if (RunEnvironment.isDevelopMode()) {
            return;
        }

        Thread synchronizeConfig = new Thread(new Runnable() {

            @Override
            public void run() {

                while (true) {

                    // 二分钟同步一次
                    try {
                        // final long timestamp = System.currentTimeMillis();

                        final Map<String/* group name */, Crontab> crontabList = getAllAvailableCrontabs();

                        log.info("start shnchronizeCrontabConfig ,update job size:"
                                + crontabList.size());

                        List<String> allTriggerGroup = scheduler.getTriggerGroupNames();

                        for (String triggerGroup : allTriggerGroup) {

                            Set<TriggerKey> triggerKeySet = scheduler
                                    .getTriggerKeys(GroupMatcher.triggerGroupEquals(triggerGroup));

                            final Crontab crontab = crontabList.get(triggerGroup);

                            if (triggerKeySet == null || triggerKeySet.size() < 1) {
                                // 说明之前没有没有的，定时任务是新加的
                                // 添加定时任务到本地scheduler
                                // createNewFullDumpTrigger(new JobSchedule(tab
                                // .getServiceName(), tab.getCrontab()));
                                continue;
                            }

                            log.info("match trigger in scheduler size:" + triggerKeySet.size());

                            for (TriggerKey triggerKey : triggerKeySet) {
                                CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
                                if (crontab == null) {
                                    // 说明这个tab已经被删除掉了
                                    scheduler.deleteJob(trigger.getJobKey());
                                    continue;
                                }

                                crontab.setOldCron();
                                log.debug("the trigger key:" + triggerKey
                                        + " update now, with new crontab:" + crontab.getCrontab());

                                // 判断两次的crontab是否相同？
                                if (StringUtils.equals(trigger.getCronExpression(),
                                        crontab.getCrontab())) {

                                    log.debug("job id:" + crontab.getServiceName() + " old crontab:"
                                            + trigger.getCronExpression() + " new crontab:"
                                            + crontab.getCrontab() + " is equal ,so ignor");

                                    continue;
                                }

                                final JobSchedule jobschedule = new JobSchedule(
                                        crontab.getServiceName(), crontab.getJobId(),
                                        crontab.getCrontab());

                                scheduler.rescheduleJob(triggerKey,
                                        createTrigger(triggerKey.getName(), jobschedule));

                                log.info("job id:" + crontab.getServiceName() + " old crontab:"
                                        + trigger.getCronExpression() + " new crontab:"
                                        + crontab.getCrontab() + " has been update");

                            }

                        }

                        for (Crontab cronTab : crontabList.values()) {

                            if (!cronTab.isNewCron()) {
                                continue;
                            }

                            log.info("create new trigger:" + cronTab.getServiceName() + ",crontab:"
                                    + cronTab.getCrontab());
                            // 创建定时任务
                            createNewFullDumpTrigger(new JobSchedule(cronTab.getServiceName(),
                                    cronTab.getJobId(), cronTab.getCrontab()));

                        }
                        // 两分钟同步一次
                        Thread.sleep(1000 * 120);
                    } catch (Throwable e) {
                        log.error(e.getMessage(), e);
                        try {
                            Thread.sleep(3000);
                        } catch (Exception e1) {

                        }
                    }

                }
            }

        });
        //
        synchronizeConfig.start();
    }

    /**
     * 取得索引定时任务的时间配置
     *
     * @param
     * @return
     */
    protected Map<String/* group name */, Crontab> getAllAvailableCrontabs() throws Exception {


        URL allCrontabsUrl = new URL(Config.getTisHost() + "/config/config.ajax?action=crontab_list_action&event_submit_do_get_list=true&resulthandler=advance_query_result");

        Map<String/* group name */, Crontab> crontabMap = new HashMap<String/*
         * group
         * name
         */, Crontab>();
        AjaxResult<com.alibaba.fastjson.JSONArray> result = HttpUtils.soapRemote(allCrontabsUrl.toString(), com.alibaba.fastjson.JSONArray.class);
        Crontab crontab = null;
        com.alibaba.fastjson.JSONArray cronlist = result.getBizresult();
        com.alibaba.fastjson.JSONObject o = null;
        for (int i = 0; i < cronlist.size(); i++) {
            o = cronlist.getJSONObject(i);
            crontab = new Crontab(o.getString("name"), o.getString("fulldump"),
                    o.getLong("fulljobid"));
            // result.add(crontab);
            crontabMap.put(getGroupName(crontab.getServiceName()), crontab);
        }
        return crontabMap;
    }

    /**
     * @param indexName 索引名称
     * @return
     * @throws SessionExpiredException
     */
    private boolean hasGrantCollectLock(String indexName) throws SessionExpiredException {
        throw new UnsupportedOperationException("indexName:" + indexName);
//        // 睡一个随机数
//        try {
//            Thread.sleep((long) (Math.random() * 1000));
//        } catch (InterruptedException e1) {
//            e1.printStackTrace();
//        }
//
//        try {
//            String COLLECT_STATE_PATH = "/terminator-lock/jst_full_dump_trigger_lock/" + indexName;
//            ITISCoordinator zokeeper = this.getZookeeper();
//
//            // 判断是否要执行收集流程
//            final Date now = new Date();
//            if (!zokeeper.exists(COLLECT_STATE_PATH, false)) {
//                // 当前节点为空，创建节点立即返回
//                zokeeper.create(COLLECT_STATE_PATH, parseCurrnetTimeStamp(now), CreateMode.EPHEMERAL, true);
//                log.info("create new lock path:" + COLLECT_STATE_PATH);
//                return true;
//            }
//            final Stat stat = new Stat();
//
//            final byte[] content = zokeeper.getData(COLLECT_STATE_PATH, null, stat, true);
//
//            final long lastExecuteTimeStamp = parseLatestExecuteTimeStamp(content);
//
//            if ((lastExecuteTimeStamp + (COLLECT_STATE_INTERVAL * 30 * 1000)) <= now.getTime()) {
//
//                // 取得锁，将现在的时间写回锁
//                zokeeper.setData(COLLECT_STATE_PATH, parseCurrnetTimeStamp(now), stat.getVersion(),
//                        true);
//
//                log.info("update the lock path:" + COLLECT_STATE_PATH);
//                return true;
//            }
//
//            return false;
//
//        } catch (SessionExpiredException e) {
//            // zookeeper客户端会话超时
//            throw e;
//        } catch (KeeperException e) {
//            log.warn("zookeeper error", e);
//            return false;
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
    }

    public static class Crontab {
        private final String serviceName;
        private final String crontab;
        private final long jobId;
        private boolean newCron = true;

        /**
         * @param serviceName
         * @param crontab
         */
        public Crontab(String serviceName, String crontab, long jobId) {
            super();
            this.serviceName = serviceName;
            this.crontab = crontab;
            this.jobId = jobId;
        }

        public long getJobId() {
            return jobId;
        }

        public boolean isNewCron() {
            return newCron;
        }

        public void setOldCron() {
            this.newCron = false;
        }

        public String getServiceName() {
            return serviceName;
        }

        public String getCrontab() {
            return crontab;
        }

    }

    @Override
    public List<JobDesc> getAllJobsInServer() {

        try {

            final Set<TriggerKey> triggerkeys = scheduler
                    .getTriggerKeys(GroupMatcher.triggerGroupStartsWith(GROUP_PREFIX));
            return getJobDesc(triggerkeys);

        } catch (SchedulerException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    private List<JobDesc> getJobDesc(final Set<TriggerKey> triggerkeys) throws SchedulerException {
        List<JobDesc> result = new ArrayList<JobDesc>();
        for (TriggerKey key : triggerkeys) {

            JobDesc desc = new JobDesc(Long
                    .parseLong(StringUtils.substringAfter(key.getName(), TRIGGER_JOB_KEY_PREFIX)));

            RTriggerKey trigerkey = new RTriggerKey();
            trigerkey.setGroup(key.getGroup());
            trigerkey.setName(key.getName());
            desc.setTriggerKey(trigerkey);

            Trigger trigger = scheduler.getTrigger(key);
            if (trigger == null) {
                continue;
            }

            desc.setPreviousFireTime(trigger.getPreviousFireTime());

            if (trigger instanceof CronTrigger) {
                desc.setCrontabExpression(((CronTrigger) trigger).getCronExpression());
            }

            result.add(desc);
        }

        return result;
    }

    @Override
    public List<JobDesc> getJob(String indexName, Long jobid) {

        try {
            TriggerKey triggerKey = TriggerKey.triggerKey(TRIGGER_JOB_KEY_PREFIX + jobid,
                    getGroupName(indexName));
            return getJobDesc(Collections.singleton(triggerKey));
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }

    }

    // @Override
    // public void resume(String coreName) throws RemoteException {
    // }

    public IJobMetaDataDAO getJobMetaDataDAO() {
        return jobMetaDataDAO;
    }

    public void setJobMetaDataDAO(IJobMetaDataDAO jobMetaDataDAO) {
        this.jobMetaDataDAO = jobMetaDataDAO;
    }

    /**
     * 设置
     *
     * @param coreName
     * @param isStop
     * @return
     */
    private List<Long> setAppState(String coreName, boolean isStop) {
        // 数据库持久层设置
        final AppTrigger trigger = this.getJobMetaDataDAO().queryJob(coreName);
        final List<Long> jobs = trigger.getJobsId();

        if (jobs.size() < 1) {
            log.warn("coreName:" + coreName + " has not define any dump job");
            return jobs;
        }

        final IJobMetaDataDAO metaDataDAO = this.getJobMetaDataDAO();
        metaDataDAO.setStop(coreName, isStop);

        TriggerJobCriteria tcriteria = createTriggerJobCriteria(jobs, isStop);

        TriggerJob record = new TriggerJob();
        record.setIsStop(isStop ? JobConstant.STOPED : JobConstant.STOPED_NOT);

        // 更新触发器表
        this.triggerBizDAO.getTriggerJobDAO().updateByExampleSelective(record, tcriteria);

        return jobs;
    }

    public void dispose() throws Exception {
        scheduler.shutdown();
    }

    private TriggerJobCriteria createTriggerJobCriteria(List<Long> jobs, boolean isStop) {
        TriggerJobCriteria tcriteria = new TriggerJobCriteria();
        tcriteria.createCriteria().andJobIdIn(jobs)
                .andIsStopEqualTo(isStop ? JobConstant.STOPED_NOT : JobConstant.STOPED)
                .andDomainEqualTo(JobConstant.DOMAIN_TIS);
        return tcriteria;
    }

    // 重新启动定时任务
    @Override
    public void resume(String coreName) {
        // 启动
        switchFullDumpSwitch(coreName, false);
    }

    @Override
    public void pause(String coreName) {
        // 停止
        switchFullDumpSwitch(coreName, true);
    }

    /**
     * @param coreName
     */
    private void switchFullDumpSwitch(String coreName, boolean isStop) {
        log.info("execute resume for app:" + coreName);

        try {
            this.setAppState(coreName, isStop);
            scheduler.deleteJob(this.createJobKey(coreName));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
        // 将scheduleer中的任务直接删除
    }

    @Override
    public boolean isServing(String coreName) throws RemoteException {

        return false;
    }

    @Override
    public boolean isPause(String coreName) {

        final AppTrigger trigger = this.getJobMetaDataDAO().queryJob(coreName);
        if (!trigger.isPause()) {
            return false;
        }

        if (trigger.getJobsId().size() < 1) {
            // 该应用还没有定义dump job 算作是停止状态
            return true;
        }

        if (this.triggerBizDAO.getTriggerJobDAO()
                .countByExample(createTriggerJobCriteria(trigger.getJobsId(), true)) > 0) {
            // 有dump，无论是增量还是全量有没有停止的
            return false;
        }

        List<JobDesc> triggerlist = new ArrayList<JobDesc>();

        for (Long jobid : trigger.getJobsId()) {
            triggerlist.addAll(getJob(coreName, jobid));
        }

        for (JobDesc desc : triggerlist) {
            if (!StringUtils.contains(desc.getCrontabExpression(), YEAR_OF_2099)) {
                return true;
            }
        }
        return true;
    }

    public IPreTriggerProcess getPreTriggerProcess() {
        return preTriggerProcess;
    }

    public void setPreTriggerProcess(IPreTriggerProcess preTriggerProcess) {
        this.preTriggerProcess = preTriggerProcess;
    }

    /**
     *
     */
    public TriggerJobManage() {
        super();
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

}
