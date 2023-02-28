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
package com.qlangtech.tis.manage.common;

import com.qlangtech.tis.pubhook.common.Nullable;
import com.qlangtech.tis.pubhook.common.RunEnvironment;
import com.qlangtech.tis.realtime.utils.NetUtils;
import com.qlangtech.tis.runtime.module.action.BasicModule;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ManageUtils {

  public static final int DELETE = 1;

  public static final int UN_DELETE = 0;

  private RunContext daoContext;

  // private IServerPoolDAO serverPoolDAO;
  private HttpServletRequest request;

  private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

  private static final SimpleDateFormat TIME_FORMAT2 = new SimpleDateFormat("yyyy-MM-dd");

  public static final ThreadLocal<SimpleDateFormat> dateFormatyyyyMMddHHmmss = new ThreadLocal<SimpleDateFormat>() {

    @Override
    protected SimpleDateFormat initialValue() {
      return new SimpleDateFormat("yyyyMMddHHmmss");
    }
  };

  public static String formatNowYyyyMMddHHmmss() {
    return dateFormatyyyyMMddHHmmss.get().format(new Date());
  }

  public static long formatNowYyyyMMddHHmmss(Date date) {
    return Long.parseLong(dateFormatyyyyMMddHHmmss.get().format(date));
  }


  public static Date getOffsetDate(int offset) {
    Calendar c = Calendar.getInstance();
    c.add(Calendar.DAY_OF_YEAR, offset);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTime();
  }

  private static final Pattern SERVLET_PATH = Pattern.compile("/([^\\.^/]+?)\\.[^\\.]+$");

  // 是否是开发模式
  public static boolean isDaily() {
    return RunEnvironment.DAILY == RunEnvironment.getSysRuntime();
  }


  public boolean isEmpty(String value) {
    return StringUtils.isEmpty(value);
  }

  public String defaultIfEmpty(String value, String defaultValue) {
    if (StringUtils.isBlank(value)) {
      return defaultValue;
    }
    return value;
  }

  public static boolean isInPauseState(TriggerCrontab crontab) {
    return isInPauseState(crontab.getFjobId() != null, (crontab.isFstop()), crontab.getIjobId() != null, (crontab.isIstop()));
  }


  public static String getServerIp() throws UnknownHostException {
    return NetUtils.getHost();
  }

  public static boolean isInPauseState(boolean hasfulldump, boolean fulljobStop, boolean hasincrdump, boolean incrjobStop) {
    if (hasfulldump) {
      return fulljobStop;

    } else if (hasincrdump) {
      return incrjobStop;

    }
    // 默认为停止状态
    return true;
  }

  public boolean isNullable(Object o) {
    return o instanceof Nullable;
  }

  @Autowired
  public void setRequest(HttpServletRequest request) {
    this.request = request;
  }

  @Autowired
  public void setDaoContext(RunContext daoContext) {
    this.daoContext = daoContext;
  }

  public static String formatDateYYYYMMdd(Date time) {
    if (time == null) {
      return StringUtils.EMPTY;
    }
    return TIME_FORMAT.format(time);
  }


  private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d{1,})\\*(\\d{1,})");

  public static String md5(String value) {
    return DigestUtils.md5Hex(value);
  }

  public int getPopLength(String size, boolean w) {
    if (StringUtils.isEmpty(size)) {
      return w ? 500 : 400;
    }
    Matcher m = SIZE_PATTERN.matcher(size);
    if (m.matches()) {
      return Integer.parseInt(w ? m.group(1) : m.group(2));
    }
    return 0;
  }


  public static boolean isValidAppDomain(HttpServletRequest request) {
    AppDomainInfo domain = getAppDomain(request);
    if (domain == null) {
      return false;
    }
    return !(domain instanceof Nullable);
  }

  public static AppDomainInfo getAppDomain(HttpServletRequest request) {
    AppDomainInfo domain = (AppDomainInfo) request.getAttribute(BasicModule.REQUEST_DOMAIN_KEY);
    return domain;

  }


  public AppDomainInfo getAppDomain() {
    return getAppDomain(this.request);
  }

  public static boolean isDaily(HttpServletRequest request) {
    return isValidAppDomain(request) && (getAppDomain(request).getRunEnvironment() == RunEnvironment.DAILY);
  }


  public boolean isNotNull(Object o) {
    return o != null;
  }


}
