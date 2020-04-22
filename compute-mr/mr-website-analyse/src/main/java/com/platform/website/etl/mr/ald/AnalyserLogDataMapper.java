package com.platform.website.etl.mr.ald;

import com.platform.website.common.EventLogConstants;
import com.platform.website.util.LoggerUtil;
import java.io.IOException;
import java.util.Map;
import java.util.zip.CRC32;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.log4j.Logger;

public class AnalyserLogDataMapper extends Mapper<Object, Text, NullWritable, Put> {

  private static final Logger logger = Logger.getLogger(AnalyserLogDataMapper.class);

  private int inputRecords, filterRecords, outputRecords;

  private byte[]family = Bytes.toBytes(EventLogConstants.EVENT_LOGS_FAMILY_NAME);
  private CRC32 crc32 = new CRC32();
  @Override
  protected void map(Object key, Text value, Context context) {
    this.inputRecords++;
    this.logger.debug("Analyse data of :" + value);

    try {
      //解析日志
      Map<String, String> clientInfo = LoggerUtil.handleLog(value.toString());

      //过滤解析失败的数据
      if (clientInfo.isEmpty()) {
        this.filterRecords++;
        return;
      }
      //获取事件别名
      String eventAliasName = clientInfo.get(EventLogConstants.LOG_COLUMN_NAME_EVENT_NAME);
      EventLogConstants.EventEnum event = EventLogConstants.EventEnum.valueOfAlias(eventAliasName);
      switch (event) {
        case LAUNCH:
        case PAGEVIEW:
        case CHARGEREQUEST:
        case CHARGESUCCESS:
        case CHARGEREFUND:
        case EVENT:
          this.outputRecords++;
          this.handleData(clientInfo, event, context);
          break;
        default:
          this.filterRecords++;
          this.logger.warn("该事件无法解析,事件名称为:" + eventAliasName);
      }

    } catch (Exception e) {
      this.logger.error("处理数据发出异常,数据: " + value, e);
    }
  }


  @Override
  protected void cleanup(Context context) throws IOException, InterruptedException {
    super.cleanup(context);
    logger.info("输入数据: " + this.inputRecords + "; 输出数据: " + outputRecords + "; 过滤数据: " + filterRecords);
  }

  private void handleData(Map<String,String> clientInfo, EventLogConstants.EventEnum eventEnum, Context context)
      throws IOException, InterruptedException {
    String uuid = clientInfo.get(EventLogConstants.LOG_COLUMN_NAME_UUID);
    String memberId = clientInfo.get(EventLogConstants.LOG_COLUMN_NAME_MEMBER_ID);
    String serverTime = clientInfo.get(EventLogConstants.LOG_COLUMN_NAME_SERVER_TIME);
    if (StringUtils.isNotBlank(serverTime))
    {
      clientInfo.remove(EventLogConstants.LOG_COLUMN_NAME_USER_AGENT);
      String rowKey = this.generateRowKey(uuid, memberId, eventEnum.alias, serverTime);//timestamp + (uuid+memberid+event).crc
      Put put = new Put(Bytes.toBytes(rowKey));
      for (Map.Entry<String, String> entry : clientInfo.entrySet()){
        if (StringUtils.isNotBlank(entry.getKey()) && StringUtils.isNotBlank(entry.getValue())){
          put.addColumn(family, Bytes.toBytes(entry.getKey()), Bytes.toBytes(entry.getValue()));
        }
      }

      context.write(NullWritable.get(), put);

    }else{
      this.filterRecords++;
    }
  }


  private String generateRowKey(String uuid, String memberId,  String eventAliasName, String serverTime){
    StringBuilder sb = new StringBuilder();
    sb.append(serverTime).append("_");
    this.crc32.reset();

    if (StringUtils.isNotBlank(uuid)){
      this.crc32.update(uuid.getBytes());
    }


    if (StringUtils.isNotBlank(memberId)){
      this.crc32.update(memberId.getBytes());
    }

    this.crc32.update(eventAliasName.getBytes());

    sb.append(this.crc32.getValue() % 100000000L);
    return sb.toString();

  }
}
