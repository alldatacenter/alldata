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
package com.qlangtech.tis;

import com.qlangtech.tis.ClusterStateCollectManager.HistoryAvarage.CountGetter;
import com.qlangtech.tis.collectinfo.api.ICoreStatistics;
import com.qlangtech.tis.common.utils.Assert;
import com.qlangtech.tis.dataplatform.dao.IClusterSnapshotPreDayDAO;
import com.qlangtech.tis.dataplatform.pojo.ClusterSnapshotCriteria;
import com.qlangtech.tis.dataplatform.pojo.ClusterSnapshotPreDay;
import com.qlangtech.tis.dataplatform.pojo.ClusterSnapshotPreDayCriteria;
import com.qlangtech.tis.email.SendMail;
import com.qlangtech.tis.pubhook.common.RunEnvironment;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年2月15日
 */
public class ClusterStateCollectManager implements InitializingBean {

    private TSearcherClusterInfoCollect collect;

    private IClusterSnapshotPreDayDAO perDayDAO;

    private static final Scheduler scheduler;

    static {
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    private static final SendMail mailSender = new SendMail();

    private static final Logger logger = LoggerFactory.getLogger(ClusterStateCollectManager.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        if (RunEnvironment.getSysRuntime() != RunEnvironment.ONLINE) {
            logger.warn("current runtime is daily,skip");
            return;
        }
        logger.warn("has the responsible to send the daily report email");
        // 每天执行发送邮件
        createTask(DailyReportJob.class, "0 30 0 * * ?");
        // 每天晚上执行一次的聚合操作
        createTask(DailyClusterSnapshot.class, "0 10 0 * * ?");
    }

    private static final String RUN_CONTEXT = "runcontext";

    public TSearcherClusterInfoCollect getCollect() {
        return collect;
    }

    public void setCollect(TSearcherClusterInfoCollect collect) {
        this.collect = collect;
    }

    /**
     * 收集每天聚合的文档数和请求数并存入数据库
     */
    public static class DailyClusterSnapshot implements Job {

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            logger.info("dailyClusterSnapshot start to work");
            try {
                JobDataMap data = context.getJobDetail().getJobDataMap();
                ClusterStateCollectManager runcontext = (ClusterStateCollectManager) data.get(RUN_CONTEXT);
                Assert.assertNotNull("runcontext can not be null", runcontext);
                Assert.assertNotNull("runcontext.collect can not be null", runcontext.collect);
                executeDailyClusterSnapshot(runcontext.collect, new Date());
                logger.info("dailyClusterSnapshot execute successful");
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new JobExecutionException(e);
            }
        }
    }

    public static void executeDailyClusterSnapshot(TSearcherClusterInfoCollect collect, Date date) throws JobExecutionException {
        logger.info("start execute executeDailyClusterSnapshot");
        // 因为是在凌晨执行的，搜集到的数据是昨天的数据，例如，现在是23日凌晨1点执行
        // 将会收集22日零点到23日零点之间的增量数据
        collect.getClusterSnapshotDAO().createTodaySummary(date);
        // 增量数据收集完成之后，需要将内存表中的历史数据删除掉，放置历史数据太大
        // 需要将5天前的历史数据删除掉
        Date time = getOffsetDate(-5);
        ClusterSnapshotCriteria criteria = new ClusterSnapshotCriteria();
        criteria.createCriteria().andGmtCreateLessThan(time);
        collect.getClusterSnapshotDAO().deleteByExample(criteria);
        logger.info("start execute executeDailyClusterSnapshot success");
    }

    protected static Date getOffsetDate(int offset) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, offset);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /**
     * 创建每天任务收集的日志
     */
    private void createTask(Class<? extends Job> clazz, String crontab) {
        try {
            final String jobKey = clazz.getSimpleName();
            final JobKey jobkey = new JobKey(jobKey, "defaultGroup");
            final JobDetail job = JobBuilder.newJob(clazz).withIdentity(jobkey).build();
            job.getJobDataMap().put(RUN_CONTEXT, this);
            Trigger trigger = createTrigger(jobKey, crontab);
            scheduler.scheduleJob(job, trigger);
            logger.info("create job:" + jobKey + ",crontab:" + crontab);
        } catch (ObjectAlreadyExistsException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CronTrigger createTrigger(String name, String crontab) {
        return TriggerBuilder.newTrigger().withIdentity("trigger" + name, "defaultGroup").startNow().withSchedule(CronScheduleBuilder.cronSchedule(crontab)).build();
    }

    public static class DailyReportJob implements Job {

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            logger.info("start to execute the DailyReportJob job");
            try {
                JobDataMap data = context.getJobDetail().getJobDataMap();
                ClusterStateCollectManager collectManager = (ClusterStateCollectManager) data.get(RUN_CONTEXT);
                Objects.requireNonNull(collectManager, "collectManager can not be null");
                collectManager.exportReport((collectManager).collect);
                logger.info("dailyReportJob exec over");
            } catch (JobExecutionException e) {
                logger.error(e.getMessage(), e.getCause());
                throw e;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new JobExecutionException(e);
            }
        }
    }

    static final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");

    // 今天向后推7天的平均时间
    private HistoryAvarageResult getAverage() {
        // JSONObject json = null;
        // <indexName,HistoryAvarage>
        final Map<Integer, HistoryAvarage> /* App ID */
        result = new HashMap<Integer, HistoryAvarage>();
        HistoryAvarageResult averageResult = new HistoryAvarageResult();
        averageResult.average = result;
        HistoryAvarage average = null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        // 第一天应该是昨天
        for (int i = 1; i <= 7; i++) {
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            averageResult.dates[7 - i] = calendar.getTime();
        }
        ClusterSnapshotPreDayCriteria query = new ClusterSnapshotPreDayCriteria();
        query.createCriteria().andGmtCreateGreaterThanOrEqualTo(calendar.getTime());
        List<ClusterSnapshotPreDay> dailyStatList = perDayDAO.selectByExample(query, 1, 500);
        for (ClusterSnapshotPreDay stat : dailyStatList) {
            average = result.get(stat.getAppId());
            if (average == null) {
                average = new HistoryAvarage();
                result.put(stat.getAppId(), average);
            }
            average.addLog(stat.getGmtCreate(), RecordExecType.parse(stat.getDataType()), stat.getIncrNumber());
        }
        return averageResult;
    }

    public static class HistoryAvarageResult {

        private Map<Integer, HistoryAvarage> /* appid */
        average;

        public Log getLog(Integer appId, Date date) {
            HistoryAvarage history = this.average.get(appId);
            if (history == null) {
                Log log = new Log();
                log.day = date;
                return log;
            }
            return history.getHistory(date);
        }

        /**
         * 某一天的数据是否有抖动
         *
         * @param appId
         * @param execType
         * @param date
         * @return
         */
        public int isLastDayShock(Integer appId, final RecordExecType execType, Date date) {
            HistoryAvarage history = average.get(appId);
            if (history == null) {
                return HistoryAvarage.EQUAL;
            }
            return history.isLastDayShock(date, new CountGetter() {

                @Override
                public long get(Log log) {
                    return log.getExecuteSum(execType);
                }
            });
        }

        /**
         * 取得最后一天的访问总数
         *
         * @return
         */
        public long getLastDayRequestCount() {
            long requestCount = 0;
            Log log = null;
            for (HistoryAvarage history : average.values()) {
                log = history.getHistory(dates[6]);
                Integer qp = null;
                if (log != null && (qp = log.getExecuteSum(RecordExecType.QUERY)) != null) {
                    requestCount += qp;
                }
            }
            return requestCount;
        }

        public long getRequestCount(Integer appId, Date date) {
            return getLog(appId, date).getExecuteSum(RecordExecType.QUERY);
        }

        private Date[] dates = new Date[7];
    }

    public IClusterSnapshotPreDayDAO getPerDayDAO() {
        return perDayDAO;
    }

    public void setPerDayDAO(IClusterSnapshotPreDayDAO perDayDAO) {
        this.perDayDAO = perDayDAO;
    }

    public void exportReport() throws JobExecutionException {
        exportReport(collect);
    }

    private static final String mail_to2 = "baisui@2dfire.com";

    public void exportReport(TSearcherClusterInfoCollect collect) throws JobExecutionException {
        logger.info("start send daily summary report");
        final Map<String, List<App>> /* 大BU线 */
        buAppMap = collect.getBuAppMap();
        final Set<String> allHosts = new HashSet<String>();
        final CoreStatisticsReportHistory preStatistics = collect.getCoreStatisticsReportHistory();
        final AtomicLong numberDoc = new AtomicLong();
        final AtomicInteger indexCount = new AtomicInteger();
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        // 统计总量
        iterateAllIndex(buAppMap, preStatistics, new ProcessReport() {

            @Override
            public void execute(ICoreStatistics report) {
                allHosts.addAll(report.getHosts());
                numberDoc.addAndGet(report.getNumDocs());
                indexCount.incrementAndGet();
            }
        }, this.collect);
        logger.info("host size:" + allHosts.size() + ",numberDoc:" + numberDoc.get() + ",indexcount:" + indexCount.get());
        final HistoryAvarageResult historyAverage = getAverage();
        StringBuffer htmlContent = new StringBuffer();
        final ToHtmlPain toHtml = new ToHtmlPain(htmlContent);
        toHtml.format("<html><head><style type=\"text/css\"><!--");
        try {
            final InputStream reader = collect.getClass().getClassLoader().getResourceAsStream("/excelStyle.css");
            toHtml.format(IOUtils.toString(reader, Charset.forName("utf8")));
            toHtml.format(" --> </style></head><body>%n");
            toHtml.printSummary(indexCount.get(), numberDoc.get(), historyAverage.getLastDayRequestCount(), allHosts.size());
            toHtml.out.format("<h2>TIS集群详细：</h2> %n");
            toHtml.out.format("<table width=\"100%%\" border=\"1\" class=\"excelDefaults\">%n");
            toHtml.printHeader(historyAverage);
            toHtml.out.format("<tbody>%n");
            // int colIndex = 3;
            ICoreStatistics report = null;
            // int rowIndex = 3;
            String serviceName = null;
            HistoryAvarage history = null;
            // 遍历业务线
            int regionRowStart;
            String preDptName = null;
            for (Map.Entry<String, List<App>> buApps : buAppMap.entrySet()) {
                if (StringUtils.isEmpty(buApps.getKey())) {
                    continue;
                }
                Collections.sort(buApps.getValue(), new Comparator<App>() {

                    @Override
                    public int compare(App o1, App o2) {
                        return o1.getDpt().compareTo(o2.getDpt());
                    }
                });
                // regionRowStart = rowIndex;
                App app = null;
                List<App> dptApps = getBizAppCount(buApps, preStatistics, this.collect);
                for (int ii = 0; ii < dptApps.size(); ii++) {
                    app = dptApps.get(ii);
                    serviceName = app.getServiceName();
                    // 当前系统快照
                    report = preStatistics.get(this.collect.getAppId(serviceName));
                    if (report == null) {
                        continue;
                    }
                    toHtml.format("<tr>%n");
                    history = historyAverage.average.get(app.getAppid());
                    // 部门
                    if (!StringUtils.equals(app.getDpt(), preDptName)) {
                        int maxSameCount = 0;
                        while ((dptApps.size() > (ii + (++maxSameCount))) && StringUtils.equals(dptApps.get(ii + (maxSameCount)).getDpt(), app.getDpt())) {
                        }
                        toHtml.format("<td rowspan='%s' nowrap='true'>%s</td>", maxSameCount, app.getDpt());
                        preDptName = app.getDpt();
                    }
                    toHtml.format("<td style='max-width:130px;font-size:10px;overflow:hidden' align='left'>%s</td>", serviceName);
                    // 索引，访问次数
                    createAppDimensionAverageView(toHtml, app.getAppid(), RecordExecType.QUERY, historyAverage);
                    // 索引，更新次数
                    createAppDimensionAverageView(toHtml, app.getAppid(), RecordExecType.UPDATE, historyAverage);
                    toHtml.format("<td  align='right' style='font-size:10px;padding:1px;'>%d</td>", report.getHostsCount());
                    toHtml.format("</tr>%n");
                }
            }
            toHtml.out.format("</tbody></table></body></html>");
            Date today = new Date();
            mailSender.send("TIS集群日报VPC(" + datef.format(today) + ")系统自动发送请不要回复此邮件", htmlContent.toString(), mail_to2);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new JobExecutionException(e);
        }
    }

    private void createAppDimensionAverageView(ToHtmlPain toHtml, Integer appid, RecordExecType execType, HistoryAvarageResult historyAverage) {
        int colIndex = 0;
        String bgColor = null;
        int isShock = HistoryAvarage.EQUAL;
        for (Date date : historyAverage.dates) {
            if ((++colIndex) == historyAverage.dates.length) {
                // 最后一天？
                isShock = historyAverage.isLastDayShock(appid, execType, date);
                if (isShock < HistoryAvarage.EQUAL) {
                    bgColor = "background-color:#B0FFAA";
                } else if (isShock > HistoryAvarage.EQUAL) {
                    bgColor = "background-color:pink";
                }
            }
            toHtml.format("<td  align='right' style='font-size:10px;padding:1px;" + (StringUtils.isNotEmpty(bgColor) ? bgColor : StringUtils.EMPTY) + "'>%,d</td>", historyAverage.getLog(appid, date).getExecuteSum(execType));
        }
    }

    private static void iterateAllIndex(Map<String, List<App>> buAppMap, CoreStatisticsReportHistory preStatistics, ProcessReport reportProcess, TSearcherClusterInfoCollect collect) {
        ICoreStatistics report = null;
        for (Map.Entry<String, List<App>> /* bizline:/2dfire/supplyGoods */
        buApps : buAppMap.entrySet()) {
            if (StringUtils.isEmpty(buApps.getKey())) {
                continue;
            }
            App app = null;
            String serviceName = null;
            List<App> dptApps = getBizAppCount(buApps, preStatistics, collect);
            for (int ii = 0; ii < dptApps.size(); ii++) {
                app = dptApps.get(ii);
                serviceName = app.getServiceName();
                // 当前系统快照
                report = preStatistics.get(collect.getAppId(serviceName));
                if (report == null) {
                    continue;
                }
                reportProcess.execute(report);
            }
        }
    }

    private interface ProcessReport {

        void execute(ICoreStatistics report);
    }

    /**
     * @param buApps
     * @return
     */
    private static List<App> getBizAppCount(Map.Entry<String, List<App>> buApps, CoreStatisticsReportHistory preStatistics, TSearcherClusterInfoCollect collect) {
        List<App> result = new ArrayList<App>();
        for (App app : buApps.getValue()) {
            if (preStatistics.get(collect.getAppId(app.getServiceName())) != null) {
                result.add(app);
            }
        }
        return result;
    }

    public static class HistoryAvarage {

        private Integer appId;

        // <日期，日志>
        private final Map<String, Log> /* date */
        logMap = new HashMap<String, Log>();

        public void setServerName(Integer appId) {
            this.appId = appId;
        }

        public static final int EQUAL = 0;

        private static final int DOWN = -1;

        private static final int UP = 1;

        private int isLastDayShock(Date datee, CountGetter getter) {
            final String fmtDate = datef.format(datee);
            long count = 0;
            int size = 0;
            long value = 0;
            for (Map.Entry<String, Log> entry : logMap.entrySet()) {
                value = getter.get(entry.getValue());
                if (value < 1) {
                    continue;
                }
                count += value;
                size++;
            }
            if (size < 1) {
                return EQUAL;
            }
            long average = count / size;
            Log log = logMap.get(fmtDate);
            if (log == null) {
                return EQUAL;
            }
            double percent = (1d * (getter.get(log) - average) / average);
            // 上升的波动
            if (percent > 0.2) {
                return UP;
            }
            // 下降的波动
            if (percent < -0.2) {
                return DOWN;
            }
            return EQUAL;
        }

        public static interface CountGetter {

            public long get(Log log);
        }

        public void addLog(Date day, RecordExecType type, Integer count) {
            Log log = this.getHistory(day);
            log.executeCount.put(type, count);
        // Log log = new Log();
        // // log.recordCount = recordCount;
        // log.requestCount = requestCount;
        // // log.serverCount = serverCount;
        // log.day = day;
        // logMap.put(datef.format(day), log);
        }

        public Log getHistory(Date day) {
            final String key = datef.format(day);
            Log log = logMap.get(key);
            if (log == null) {
                log = new Log();
                log.day = day;
                logMap.put(key, log);
            }
            return log;
        }
    }

    private static final SimpleDateFormat datef = new SimpleDateFormat("MM/dd");

    private static class Log {

        // private long recordCount;
        // private long requestCount;
        // private long updateCount;
        // private long requestErrorCount;
        // private long updateErrorCount;
        final Map<RecordExecType, Integer> /* data type */
        executeCount = new HashMap<RecordExecType, Integer>();

        // private int serverCount;
        private Date day;

        public String getDate() {
            return datef.format(day);
        }

        public Integer getExecuteSum(RecordExecType executeType) {
            Integer value = executeCount.get(executeType);
            return (value != null) ? value : 0;
        }
        // public Set<Map.Entry<RecordExecType, Integer>> getExecuteCountMap() {
        // return executeCount.entrySet();
        // }
        // public int getServerCount() {
        // return this.serverCount;
        // }
    }

    private static class ToHtmlPain {

        // private StringBuffer out = new StringBuffer();
        // = new Formatter(output);
        private final Formatter out;

        public ToHtmlPain(StringBuffer buffer) {
            super();
            this.out = new Formatter(buffer);
        }

        // private void writeDateHeader(final HistoryAvarageResult
        // historyRecord,
        // XSSFSheet sheet) {
        // int colIndex = 3;
        // XSSFRow row;
        // row = sheet.createRow(2);
        // ExcelRow erow = new ExcelRow(row, "");
        // // 写日期title
        // for (int i = 0; i < 2; i++) {
        // for (Date date : historyRecord.dates) {
        // erow.setString(colIndex++, datef.format(date));
        // }
        // }
        // }
        Formatter format(String format, Object... args) {
            return out.format(format, args);
        }

        public void printSummary(int allAppsCount, long allDocCount, long allRequestCount, int allServerCount) {
            out.format("<h2>TIS集群数据汇总：</h2> %n");
            out.format("<table class=\"excelDefaults\" width=\"400\"> ");
            out.format(" <tr><th class=\"rowHeader\" width=\"30%%\">应用总数:</th><td class=\"style2\">%,d</td></tr>  ", allAppsCount);
            out.format("  <tr><td class=\"rowHeader\" >索引总条数:</td> ");
            out.format("  <td align=\"right\" class=\"style2\">%,d</td> ", allDocCount);
            out.format("  </tr> ");
            out.format("  <tr><td class=\"rowHeader\">日QP总数:</td> ");
            out.format("  <td class=\"style2\">%,d</td> ", allRequestCount);
            out.format("  </tr>  ");
            out.format("  <tr><td class=\"rowHeader\">机器总数:</td><td class=\"style2\">%,d</td> ", allServerCount);
            out.format("  </tr> ");
            out.format("</table> %n");
        }

        public void printHeader(final HistoryAvarageResult historyRecord) {
            out.format("<thead class='colHeader'>                                                             ");
            out.format(" <tr>                                                               ");
            // out.format(" <th class=\"style_03\" rowspan=\"2\">业务线</th> ");
            out.format("    <th class=\"style_09\" rowspan=\"2\">部门</th>                  ");
            out.format("    <th class=\"style_09\" rowspan=\"2\">索引名称</th>              ");
            out.format("    <th class=\"style_07\" colspan=\"7\">访问次数</th>                    ");
            out.format("                                                                    ");
            out.format("    <th class=\"style_0b\" colspan=\"7\">更新次数</th>              ");
            out.format("                                                                    ");
            out.format("    <th class=\"style_05\" rowspan=\"2\" nowrap='true'>机器数</th>                ");
            out.format("</tr>                                                               ");
            out.format("<tr> %n                                                          ");
            // region 1
            for (int i = 0; i < 2; i++) {
                for (Date date : historyRecord.dates) {
                    out.format("    <th class=\"style1\" >%s</th>   ", datef.format(date));
                }
            }
            out.format("</tr>                                                               ");
            out.format("</thead>                                                            ");
        }
    }
}
