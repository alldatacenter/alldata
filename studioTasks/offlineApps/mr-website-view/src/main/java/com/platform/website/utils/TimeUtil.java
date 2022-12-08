package com.platform.website.utils;

import com.platform.website.common.DateEnum;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class TimeUtil {

  public final static String DATE_FORMAT = "yyyy-MM-dd";

  /**
   * 将nginx服务器时间转换成时间戳
   */
  public static long parseNginxServerTime2Long(String input) {
    Date date = parseNginxServerTime2Date(input);
    return date == null ? -1L : date.getTime();

  }


  /**
   * 将nginx服务器时间转换成date
   */
  public static Date parseNginxServerTime2Date(String input) {

    if (StringUtils.isNotBlank(input)) {
      try {
        long timestamp = Double.valueOf(Double.valueOf(input.trim()) * 1000).longValue();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        return calendar.getTime();
      } catch (Exception e) {
        //nothing
      }
    }

    return null;

  }


  public static boolean isValidateRunningDate(String date) {
    Matcher matcher = null;
    boolean result = false;
    String regex = "[0-9]{4}-[0-9]{2}-[0-9]{2}";
    if (date != null && !date.isEmpty()) {
      Pattern pattern = Pattern.compile(regex);
      matcher = pattern.matcher(date);
    }
    if (matcher != null) {
      result = matcher.matches();
    }
    return result;
  }

  public static String getYesterday() {
    return getYesterday(DATE_FORMAT);
  }

  public static String getYesterday(String pattern) {
    SimpleDateFormat sdf = new SimpleDateFormat(pattern);
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DAY_OF_YEAR, -1);
    return sdf.format(calendar.getTime());
  }

  public static long parseString2Long(String input) {
    return parseString2Long(input, DATE_FORMAT)
        ;
  }


  public static long parseString2Long(String input, String pattern) {
    Date date = null;
    try {
      date = new SimpleDateFormat(pattern).parse(input);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
    return date.getTime();
  }


  public static String parseLong2String(long input) {
    return parseLong2String(input, DATE_FORMAT)
        ;
  }


  public static String parseLong2String(long input, String pattern) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(input);
    return new SimpleDateFormat(pattern).format(calendar.getTime());
  }

  /**
   * 根据时间戳获取需要的type对应的信息
   */
  public static int getDateInfo(long time, DateEnum type) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(time);
    if (DateEnum.YEAR.equals(type)) {
      return calendar.get(Calendar.YEAR);
    } else if (DateEnum.SEASON.equals(type)) {
      //季度
      int month = calendar.get(Calendar.MONTH);
      if (month % 3 == 0) {
        return month / 3;
      }
      return month / 3 + 1;
    } else if (DateEnum.MONTH.equals(type)) {
      return calendar.get(Calendar.MONTH) + 1;
    } else if (DateEnum.WEEK.equals(type)) {
      return calendar.get(Calendar.WEEK_OF_YEAR);
    } else if (DateEnum.DAY.equals(type)) {
      return calendar.get(Calendar.DAY_OF_MONTH);
    } else if (DateEnum.HOUR.equals(type)) {
      return calendar.get(Calendar.HOUR_OF_DAY);
    }
    throw new RuntimeException("没有对应的时间类型" + type);
  }


  public static long getFirstDayOfThisWeek(long time) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(time);
    calendar.set(Calendar.DAY_OF_WEEK, 1);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar.getTimeInMillis();
  }

  public static boolean checkDate(String date) {
    boolean convertSuccess = true;
// 指定日期格式为四位年/两位月份/两位日期，注意yyyy/MM/dd区分大小写；
    SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
    try {
// 设置lenient为false. 否则SimpleDateFormat会比较宽松地验证日期，比如2007/02/29会被接受，并转换成2007/03/01
      format.setLenient(false);
      format.parse(date);
    } catch (ParseException e) {
      // e.printStackTrace();
// 如果throw java.text.ParseException或者NullPointerException，就说明格式不对
      convertSuccess = false;
    }
    return convertSuccess;
  }

  public static String getSpecifiedDate(String startDate, int i) throws ParseException {
    SimpleDateFormat dft = new SimpleDateFormat("yyyy-MM-dd");
    Date beginDate = dft.parse(startDate);
    Calendar date = Calendar.getInstance();
    date.setTime(beginDate);
    date.set(Calendar.DATE, date.get(Calendar.DATE) - 1);
    Date endDate = dft.parse(dft.format(date.getTime()));
    return dft.format(endDate);
  }
}
