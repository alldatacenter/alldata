package com.platform.website.transformer.model.value;

import com.platform.website.common.KpiType;
import org.apache.hadoop.io.Writable;

/**
 * 自定义的顶级输出value类
 */
public abstract class BaseStatsValueWritable implements Writable {

  /**
   * 获取当前value对应的kpi
   * @return
   */
  public abstract KpiType getKpi();

}
