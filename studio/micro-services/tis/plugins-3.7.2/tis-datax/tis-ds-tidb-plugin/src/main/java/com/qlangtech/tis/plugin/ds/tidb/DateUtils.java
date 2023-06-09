/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugin.ds.tidb;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author: baisui 百岁
 * @create: 2020-12-07 12:23
 **/
public class DateUtils {
    static final Date base = new Date(0);
    private static final ThreadLocal<SimpleDateFormat> dateFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            //return super.initialValue();
            //YYYY-MM-DD HH:MM:SS.fffffffff
            return new SimpleDateFormat("yyyy-MM-dd");
        }
    };
    private static final ThreadLocal<SimpleDateFormat> timestampFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            //return super.initialValue();
            //YYYY-MM-DD HH:MM:SS.fffffffff
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        }
    };

    public static void main(String[] args) {
        // convert(0);
        System.out.println(formatDate(-6144));
        System.out.println(timestampFormat.get().format(new Date()));

        //1603463884000000

        Date d = new Date(1603463884000l);
        ;
        System.out.println(timestampFormat.get().format(d));
    }

  //  public static long formatTimestamp(long baseOffset) {
//        Calendar instance = Calendar.getInstance();
//        instance.setTime(base);
//        instance.add(Calendar.MILLISECOND, (int) baseOffset);
//        //return timestampFormat.get().format(instance.getTime());
//        return instance.getTimeInMillis();
   // }

    public static long formatDate(long baseOffset) {
//        +--------+------------+------------+-----------+--------+------------+
//        | emp_no | birth_date | first_name | last_name | gender | hire_date  |
//        +--------+------------+------------+-----------+--------+------------+
//        | 499996 | 1953-03-07 | Zito       | Baaz      | M      | 1990-09-27 |
//        +--------+------------+------------+-----------+--------+------------+

//        -6144
//        7574

        //https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Types#LanguageManualTypes-date
        Calendar instance = Calendar.getInstance();
        instance.setTime(base);
        instance.add(Calendar.DATE, (int) baseOffset);
        //  System.out.println(instance.getTime());
        // return dateFormat.get().format(instance.getTime());

        return instance.getTimeInMillis();
    }

}
