package com.platform.website.util;

import lombok.Data;

/**
 * 用于计算会话长度的类
 */
@Data
public class TimeChain {

  //储存数据的长度
  private static final int CHAIN_SIZE = 2;
  //储存的数据
  private long[] times;

  private int index;
  private int size;
  private int tmpTime;

  public TimeChain() {
  }

  public TimeChain(long time) {
    this.times = new long[CHAIN_SIZE]
    ;
    this.times[0] = time;
    this.index = 0;
    this.size = 1;
    this.tmpTime = 0;
  }

  /**
   * 添加时间，只保存最小和最大时间
   */
  public void addTime(long time) {
    if (this.size == 1) {
      //表示此时只保存一个数据
      long temp = this.times[this.index];
      if (temp > time) {
        this.times[this.index] = time;
        this.times[1] = temp;
      } else {
        //要加入的时间大于存在的时间
        this.times[1] = time;
      }

    } else if (this.size == 2) {
      //此时表示已经保存了两个时间值
      long first = this.times[0];
      long second = this.times[1];
      if (time < first) {
        //要加入的时间小于最小时间
        this.times[0] = time;
      }
      if (time > second) {
        this.times[1] = time;
      }
    }
  }

  public long getMinTime() {
    return this.times[0];
  }


  public long getMaxTime() {
    return this.times[1];
  }


  public long getTimeOfMillis() {
    return this.getMaxTime() - this.getMinTime();

  }

  /**
   * 获取时间间隔 秒数
   */
  public int getTimeofSecond() {
    return (int) (this.getTimeOfMillis() / 1000);
  }

}
