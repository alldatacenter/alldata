/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.jobmanager.manager.util

import org.slf4j.LoggerFactory

import java.text.SimpleDateFormat
import java.util.Date

object DateUtils extends Serializable {

  private val logger = LoggerFactory.getLogger(DateUtils.getClass)
  private val serialVersionUID = 1L
  /**
   * Default short date format(默认短日期格式)
   * yyyy-MM-dd
   */
  val DATE_DEFAULT_FORMAT = "yyyy-MM-dd"

  /**
   * Order-number prefix(订单号前缀) yyyyMMddHHmmss
   */
  val DATETIME_ORDER_FORMAT = "yyyyMMddHHmmss"
  /**
   * Default datetime format(默认日期时间格式)
   * yyyy-MM-dd HH:mm:ss
   */
  val DATETIME_DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss"
  /**
   * Default time format(默认时间格式)
   * HH:mm:ss
   */
  val TIME_DEFAULT_FORMAT = "HH:mm:ss"
  /**
   * Default date short format(默认日期短格式)
   * yyyyMMdd
   */
  val DATE_DEFAULT_SHORT_FORMAT = "yyyyMMdd"
  /**
   * Default Datetime format(默认日期时间格式化)
   */
  val dateTimeFormat = new SimpleDateFormat(DATETIME_DEFAULT_FORMAT)

  def intervals(start:Date,end:Date): String ={
    if(start == null || end == null) return ""
    val nm = 1000 * 60
    val diff = end.getTime - start.getTime
    (diff/nm)+"分钟"
  }

  def formatDate(dateTime:Date):String = {
    if(dateTime == null) return ""
    dateTimeFormat.format(dateTime)
  }

}
