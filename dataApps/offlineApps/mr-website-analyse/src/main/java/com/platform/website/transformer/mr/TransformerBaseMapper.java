package com.platform.website.transformer.mr;

import com.platform.website.common.EventLogConstants;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;

public class TransformerBaseMapper<KEYOUT, VALUEOUT> extends TableMapper<KEYOUT, VALUEOUT> {
  private static final Logger logger = Logger.getLogger(TransformerBaseMapper.class);
  private long startTime = System.currentTimeMillis();
  public static final byte[] family = Bytes.toBytes(EventLogConstants.EVENT_LOGS_FAMILY_NAME); // hbase的family名称
  protected Configuration conf = null;
  protected int inputRecords = 0; // 输入记录数
  protected int filterRecords = 0; // 过滤的记录数, 要求输入的记录没有进行任何输出
  protected int outputRecords = 0; // 输出的记录条数

  /**
   * 初始化方法
   */
  @Override
  protected void setup(Context context) throws IOException, InterruptedException {
    super.setup(context);
    this.conf = context.getConfiguration();
  }

  @Override
  protected void cleanup(Context context) throws IOException, InterruptedException {
    super.cleanup(context);
    // 不希望这块代码影响整体功能，所以用try-catch括起来
    try {
      // 打印提示信息格式为: jobid 运行时间 输入记录数 过滤记录数 输出记录数
      long endTime = System.currentTimeMillis();
      StringBuilder sb = new StringBuilder();
      sb.append("job_id:").append(context.getJobID().toString());
      sb.append("; start_time:").append(this.startTime);
      sb.append("; end_time:").append(endTime);
      sb.append("; using_time:").append((endTime - this.startTime)).append("ms");
      sb.append("; input records:").append(this.inputRecords);
      sb.append("; filter records:").append(this.filterRecords);
      sb.append("; output records:").append(this.outputRecords);

      logger.info(sb.toString());
    } catch (Throwable e) {
      // nothing
    }
  }

  /**
   * 获取uuid
   *
   * @param value
   * @return
   */
  public String getUuid(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_UUID);
  }

  /**
   * 获取平台名称对应的value
   *
   * @param value
   * @return
   */
  public String getPlatform(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_PLATFORM);
  }

  /**
   * 获取服务器时间戳
   *
   * @param value
   * @return
   */
  public String getServerTime(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_SERVER_TIME);
  }

  /**
   * 获取浏览器名称
   *
   * @param value
   * @return
   */
  public String getBrowserName(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_BROWSER_NAME);
  }

  /**
   * 获取浏览器版本号
   *
   * @param value
   * @return
   */
  public String getBrowserVersion(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_BROWSER_VERSION);
  }

  /**
   * 获取ip地址
   *
   * @param value
   * @return
   */
  public String getIp(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_IP);
  }

  /**
   * 获取事件名称
   *
   * @param value
   * @return
   */
  public String getEventName(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_EVENT_NAME);
  }

  /**
   * 获取版本号
   *
   * @param value
   * @return
   */
  public String getVersion(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_VERSION);
  }

  /**
   * 获取会员id
   *
   * @param value
   * @return
   */
  public String getMemberId(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_MEMBER_ID);
  }

  /**
   * 获取会话时间
   *
   * @param value
   * @return
   */
  public String getSessionId(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_SESSION_ID);
  }

  /**
   * 获取客户端时间
   *
   * @param value
   * @return
   */
  public String getClientTime(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_CLIENT_TIME);
  }

  /**
   * 获取浏览器语言信息
   *
   * @param value
   * @return
   */
  public String getLanguage(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_LANGUAGE);
  }

  /**
   * 获取浏览器分辨率大小
   *
   * @param value
   * @return
   */
  public String getBrowserResolution(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_RESOLUTION);
  }

  /**
   * 获取当前页面url
   *
   * @param value
   * @return
   */
  public String getCurrentUrl(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_CURRENT_URL);
  }

  /**
   * 获取前一个页面
   *
   * @param value
   * @return
   */
  public String getReferrerUrl(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_REFERER_URL);
  }

  /**
   * 获取网页title
   *
   * @param value
   * @return
   */
  public String getTitle(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_TITLE);
  }

  /**
   * 获取订单id
   *
   * @param value
   * @return
   */
  public String getOrderId(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_ORDER_ID);
  }

  /**
   * 获取订单名称
   *
   * @param value
   * @return
   */
  public String getOrderName(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_ORDER_NAME);
  }

  /**
   * 获取定义金额
   *
   * @param value
   * @return
   */
  public String getOrderCurrencyAmount(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_ORDER_CURRENCY_AMOUNT);
  }

  /**
   * 获取订单货币类型
   *
   * @param value
   * @return
   */
  public String getOrderCurrencyType(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_ORDER_CURRENCY_TYPE);
  }

  /**
   * 获取订单支付方式
   *
   * @param value
   * @return
   */
  public String getOrderPaymentType(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_ORDER_PAYMENT_TYPE);
  }

  /**
   * 获取事件category值
   *
   * @param value
   * @return
   */
  public String getEventCategory(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_EVENT_CATEGORY);
  }

  /**
   * 获取事件action
   *
   * @param value
   * @return
   */
  public String getEventAction(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_EVENT_ACTION);
  }

  /**
   * 获取事件传递的map对象
   *
   * @param value
   * @return
   */
  public Map<String, String> getEventMap(Result value) {
    Map<String, String> map = new HashMap<String, String>();
    for (Map.Entry<byte[], byte[]> entry : value.getFamilyMap(family).entrySet()) {
      String column = Bytes.toString(entry.getKey());
      if (column.startsWith(EventLogConstants.LOG_COLUMN_NAME_EVENT_KV_START)) {
        map.put(column, Bytes.toString(entry.getValue()));
      }
    }
    return map;
  }

  /**
   * 获取事件持续时间
   *
   * @param value
   * @return
   */
  public String getEventDuration(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_EVENT_DURATION);
  }

  /**
   * 获取os名称
   *
   * @param value
   * @return
   */
  public String getOsName(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_OS_NAME);
  }

  /**
   * 获取os版本号
   *
   * @param value
   * @return
   */
  public String getOsVersion(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_OS_VERSION);
  }

  /**
   * 获取地域国家
   *
   * @param value
   * @return
   */
  public String getCountry(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_COUNTRY);
  }

  /**
   * 获取地域省份
   *
   * @param value
   * @return
   */
  public String getProvince(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_PROVINCE);
  }

  /**
   * 获取地域城市
   *
   * @param value
   * @return
   */
  public String getCity(Result value) {
    return this.fetchValue(value, EventLogConstants.LOG_COLUMN_NAME_CITY);
  }

  /**
   * 公用方法，提取value中的column列的值
   *
   * @param value
   * @param column
   * @return
   */
  private String fetchValue(Result value, String column) {
    return Bytes.toString(value.getValue(family, Bytes.toBytes(column)));
  }

}
