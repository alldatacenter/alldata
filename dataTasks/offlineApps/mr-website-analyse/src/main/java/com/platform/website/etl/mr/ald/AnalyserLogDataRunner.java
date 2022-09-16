package com.platform.website.etl.mr.ald;

import com.platform.website.common.EventLogConstants;
import com.platform.website.common.GlobalConstants;
import com.platform.website.util.TimeUtil;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

public class AnalyserLogDataRunner implements Tool {

  private static Configuration conf;
  private static String ClusterName = "nn";
  private static final String HADOOP_URL = "hdfs://"+ClusterName;
  static {
    conf = new Configuration();
    conf.set("fs.defaultFS", HADOOP_URL);
    conf.set("dfs.nameservices", ClusterName);
    conf.set("dfs.ha.namenodes."+ClusterName, "Master,Master2");
    conf.set("dfs.namenode.rpc-address."+ClusterName+".Master", "192.168.52.156:9000");
    conf.set("dfs.namenode.rpc-address."+ClusterName+".Master2", "192.168.52.147:9000");
    //conf.setBoolean(name, value);
    conf.set("dfs.client.failover.proxy.provider."+ClusterName,
        "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider");
  }

  private static final Logger logger = Logger.getLogger(AnalyserLogDataRunner.class);


  public static void main(String[] args) {
    try {
      ToolRunner.run(new Configuration(), new AnalyserLogDataRunner(), args);
    } catch (Exception e) {
      logger.error("执行日志解析job异常" , e );
      throw new RuntimeException(e);
    }
  }

  @Override
  public int run(String[] args) throws Exception {
    Configuration conf = this.getConf();
    this.processArgs(conf, args);

    Job job = Job.getInstance(conf, "analyser_logdata");
    job.setJarByClass(AnalyserLogDataRunner.class);
    job.setMapperClass(AnalyserLogDataMapper.class);
    job.setMapOutputKeyClass(NullWritable.class);
    job.setMapOutputValueClass(Put.class);

    //设置reducer配置
    //1集群上运行 打成jar运行  (要求addDependencyJars为true(默认true)
//    TableMapReduceUtil.initTableReducerJob(EventLogConstants.HBASE_NAME_EVENT_LOGS, null, job);
    TableMapReduceUtil.initTableReducerJob(EventLogConstants.HBASE_NAME_EVENT_LOGS, null, job,null,
        null,null,null,true);
    //2本地运行 打成jar运行  (要求addDependencyJars为true(默认true)
//    TableMapReduceUtil
//        .initTableReducerJob(EventLogConstants.HBASE_NAME_EVENT_LOGS, null, job, null, null, null,
//            null, false);
    //设置输入路径
    job.setNumReduceTasks(0);
    this.setJobInputPaths(job);
    return job.waitForCompletion(true) ? 0 : -1;
  }

  private void processArgs(Configuration conf, String[] args) {
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
    if(StringUtils.isBlank(date) || !TimeUtil.isValidateRunningDate(date)){
      //date无效
      date  = TimeUtil.getYesterday();
    }
    conf.set(GlobalConstants.RUNNING_DATE_PARAMS, date);
  }

  @Override
  public void setConf(Configuration configuration) {
  }

  @Override
  public Configuration getConf() {
    return this.conf;
  }

  public void setJobInputPaths(Job job) {
    Configuration conf = job.getConfiguration();
    FileSystem fs = null;

    try{
      fs = FileSystem.get(conf);
      String date = conf.get(GlobalConstants.RUNNING_DATE_PARAMS);
//      Path inputPath = new Path("/logs/" + TimeUtil
//          .parseLong2String(TimeUtil.parseString2Long(date), "yyyy/MM/dd/"));
      Path inputPath = new Path(
          "/logs/" + TimeUtil.parseLong2String(TimeUtil.parseString2Long(date), "MM/dd/"));
      if (fs.exists(inputPath)) {
        FileInputFormat.addInputPath(job, inputPath);
      }else{
        throw new RuntimeException("文件不存在:" + inputPath);
      }
    } catch (IOException e) {
      throw new RuntimeException("设置Job的mapreduce输入路径出现异常", e);
    }finally {
      if (fs != null){
        try{
          fs.close();
        }catch (IOException e){
          //nothing
        }
      }
    }
  }
}
