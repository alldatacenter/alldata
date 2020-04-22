package com.platform.website.transformer.mr;

import com.google.common.collect.Lists;
import com.platform.website.common.EventLogConstants;
import com.platform.website.common.GlobalConstants;
import com.platform.website.util.TimeUtil;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.MultipleColumnPrefixFilter;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

public abstract class TransformBaseRunner implements Tool {

  private static final Logger logger = Logger.getLogger(TransformBaseRunner.class);
  private static String ClusterName = "nn";
  private static final String HADOOP_URL = "hdfs://" + ClusterName;
  private static Configuration conf = new Configuration();
  protected String jobName;

  private Class<?> runnerClass;
  private Class<? extends TableMapper<?, ?>> mapperClass;
  private Class<? extends Reducer<?, ?, ?, ?>> reducerClass;
  private Class<? extends OutputFormat<?, ?>> outputFormatClass;
  private Class<? extends WritableComparable<?>> mapOutputKeyClass;
  private Class<? extends Writable> mapOutputValueClass;
  private Class<?> outputKeyClass;
  private Class<?> outputValueClass;
  private long startTime;
  private boolean isCallSetUpRunnerMethod = false;

  static {
    conf.set("fs.defaultFS", HADOOP_URL);
    conf.set("dfs.nameservices", ClusterName);
    conf.set("dfs.ha.namenodes." + ClusterName, "Master,Master2");
    conf.set("dfs.namenode.rpc-address." + ClusterName + ".Master", "192.168.52.156:9000");
    conf.set("dfs.namenode.rpc-address." + ClusterName + ".Master2", "192.168.52.147:9000");
    conf.set("dfs.client.failover.proxy.provider." + ClusterName,
        "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider");

//    conf.addResource("hbase-site.xml");
    conf.addResource("output-collector.xml");
    conf.addResource("query-mapping.xml");
    conf.addResource("transformer-env.xml");
  }

  public void setupRunner(String jobName, Class<?> runnerClass,
      Class<? extends TableMapper<?, ?>> mapperClass, Class<? extends Reducer<?, ?, ?, ?>> reducerClass,
      Class<? extends WritableComparable<?>> outputKeyClass,
      Class<? extends Writable> outputValueClass) {
    this.setupRunner(jobName, runnerClass, mapperClass, reducerClass, outputKeyClass, outputValueClass, TransformerOutputFormat.class);
  }

  public void setupRunner(String jobName, Class<?> runnerClass,
      Class<? extends TableMapper<?, ?>> mapperClass, Class<? extends Reducer<?, ?, ?, ?>> reducerClass,
      Class<? extends WritableComparable<?>> outputKeyClass,
      Class<? extends Writable> outputValueClass,
      Class<? extends OutputFormat<?, ?>> outputFormatClass) {
      this.setupRunner(jobName, runnerClass, mapperClass, reducerClass, outputKeyClass, outputValueClass, outputKeyClass, outputValueClass, outputFormatClass);

  }


  public void setupRunner(String jobName, Class<?> runnerClass,
      Class<? extends TableMapper<?, ?>> mapperClass, Class<? extends Reducer<?, ?, ?, ?>> reducerClass,
      Class<? extends WritableComparable<?>> mapOutputKeyClass,
      Class<? extends Writable> mapOutputValueClass,
      Class<? extends WritableComparable<?>> outputKeyClass,
      Class<? extends Writable> outputValueClass) {
    this.setupRunner(jobName, runnerClass, mapperClass, reducerClass, mapOutputKeyClass, mapOutputValueClass, outputKeyClass, outputValueClass, TransformerOutputFormat.class);
  }


  public void setupRunner(String jobName, Class<?> runnerClass,
      Class<? extends TableMapper<?, ?>> mapperClass, Class<? extends Reducer<?, ?, ?, ?>> reducerClass,
      Class<? extends WritableComparable<?>> mapOutputKeyClass,
      Class<? extends Writable> mapOutputValueClass,
      Class<? extends WritableComparable<?>> outputKeyClass,
      Class<? extends Writable> outputValueClass,
      Class<? extends OutputFormat<?, ?>> outputFormatClass) {

    this.jobName = jobName;
    this.runnerClass = runnerClass;
    this.mapperClass = mapperClass;
    this.reducerClass = reducerClass;
    this.mapOutputKeyClass = mapOutputKeyClass;
    this.mapOutputValueClass = mapOutputValueClass;
    this.outputKeyClass = outputKeyClass;
    this.outputValueClass = outputValueClass;
    this.outputFormatClass = outputFormatClass;
    this.isCallSetUpRunnerMethod = true;
  }

  /**
   * 代码执行函数
   * @param args
   * @throws Exception
   */
  public void startRunner(String []args)throws  Exception{
    ToolRunner.run(new Configuration(), this, args);
  }

  @Override
  public int run(String[] args) throws Exception {
    if (!this.isCallSetUpRunnerMethod){
      throw new RuntimeException("必须调用setupRunner方法进行参数设置");
    }
    Configuration conf = this.getConf();
    this.processArgs(conf, args);
    Job job = this.initJob(conf);//创建job
    //执行job
    this.beforeRunJob(job);
    Throwable error = null;
    try {
      this.startTime = System.currentTimeMillis();
      return job.waitForCompletion(true) ? 0 : -1;
    } catch (Throwable e) {
      error = e;
      logger.error("执行" + this.jobName + "job出现异常", e);
      throw new RuntimeException(e);
    } finally {
      this.afterRunJob(job, error);
    }
  }

  protected void beforeRunJob(Job job) throws IOException {
  }

  protected void afterRunJob(Job job, Throwable e) throws IOException {
    try {

      long endTime = System.currentTimeMillis();
      logger.info("Job" + job.getJobName() + "是否执行成功:" + (e == null ? job.isSuccessful() : false)
          + "; 开始时间:" + startTime
          + "; 结束时间:" + endTime + "; 用时" + (endTime - startTime) + "ms" + (e == null ? ""
          : "; 异常信息为:" + e));
    } catch (Throwable e1) {
      //nothing
    }
  }

  protected Job initJob(Configuration conf) throws IOException {
    Job job = Job.getInstance(conf, this.jobName);

    job.setJarByClass(this.runnerClass);
    // 本地运行
//    TableMapReduceUtil.initTableMapperJob(initScans(job), this.mapperClass, this.mapOutputKeyClass, this.mapOutputValueClass, job, false);
    TableMapReduceUtil.initTableMapperJob(initScans(job), this.mapperClass, this.mapOutputKeyClass, this.mapOutputValueClass, job, true);
    // 集群运行：本地提交和打包(jar)提交
    // TableMapReduceUtil.initTableMapperJob(initScans(job),
    // this.mapperClass, this.mapOutputKeyClass, this.mapOutputValueClass,
    // job);
    job.setReducerClass(this.reducerClass);
    job.setOutputKeyClass(this.outputKeyClass);
    job.setOutputValueClass(this.outputValueClass);
    job.setOutputFormatClass(this.outputFormatClass);
    return job;
  }


  @Override
  public void setConf(Configuration conf) {
  }

  @Override
  public Configuration getConf() {
    return this.conf;
  }

  /**
   * 初始化scan集合
   *
   * @param job
   * @return
   */
  protected List<Scan> initScans(Job job) {
    Configuration conf = job.getConfiguration();
    // 获取运行时间: yyyy-MM-dd
    String date = conf.get(GlobalConstants.RUNNING_DATE_PARAMS);
    long startDate = TimeUtil.parseString2Long(date);
    long endDate = startDate + GlobalConstants.DAY_OF_MILLISECONDS;

    Scan scan = new Scan();
    // 定义hbase扫描的开始rowkey和结束rowkey
    scan.setStartRow(Bytes.toBytes("" + startDate));
    scan.setStopRow(Bytes.toBytes("" + endDate));

    scan.setAttribute(Scan.SCAN_ATTRIBUTES_TABLE_NAME, Bytes.toBytes(EventLogConstants.HBASE_NAME_EVENT_LOGS));
    Filter filter = this.fetchHbaseFilter();
    if (filter != null) {
      scan.setFilter(filter);
    }

    // 优化设置cache
//    scan.setBatch(500);
//    scan.setCacheBlocks(true); // 启动cache blocks
//    scan.setCaching(1000); // 设置每次返回的行数，默认值100，设置较大的值可以提高速度(减少rpc操作)，但是较大的值可能会导致内存异常。
    return Lists.newArrayList(scan);
  }

  protected Filter getColumnFilter(String[] columns) {
    int length = columns.length;
    byte[][] filter = new byte[length][];
    for (int i = 0; i < length; i++) {
      filter[i] = Bytes.toBytes(columns[i]);
    }
    return new MultipleColumnPrefixFilter(filter);
  }

  protected void processArgs(Configuration conf, String[] args) {
    String date = null;
    for (int i = 0; i < args.length; i++) {
      if ("-d".equals(args[i])) {
        if (i + 1 < args.length) {
          date = args[++i];
          break;
        }
      }
    }

    //要求date格式为yyyy-MM-dd
    if (StringUtils.isBlank(date) || !TimeUtil.isValidateRunningDate(date)) {
      //date无效
      date = TimeUtil.getYesterday();
    }
    conf.set(GlobalConstants.RUNNING_DATE_PARAMS, date);
  }

  /**
   * 获取hbase操作的过滤filter对象
   * @return
   */
  protected Filter fetchHbaseFilter() {
    return null;
  }


}
