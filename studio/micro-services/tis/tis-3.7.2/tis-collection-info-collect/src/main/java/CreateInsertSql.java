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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class CreateInsertSql {

    private static final Pattern p = Pattern.compile("(.*?),(.*?),(\\d*?),(.+?),(.*?),(.+?)");

    private static final Connection conn;

    static {
        try {
            // MySQ
            String user = "terminatorhome";
            String password = "terminatorhome";
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://10.232.31.36:3306/terminatorhome?useUnicode=yes&amp;characterEncoding=GBK", user, password);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] arg) throws Exception {
        // if (matcher.matches()) {
        // System.out.println(matcher.group(1));
        // System.out.println(matcher.group(2));
        // System.out.println(matcher.group(3));
        // System.out.println(matcher.group(4));
        // System.out.println(matcher.group(5));
        // System.out.println(matcher.group(6));
        // }
        Statement statement = null;
        PreparedStatement prep = null;
        ResultSet result = null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(CreateInsertSql.class.getResourceAsStream("terminator.csv")));
        String line = null;
        Matcher matcher = null;
        String appName = null;
        Integer group = null;
        String hostName = null;
        Integer appid = null;
        Integer groupid = null;
        String ipAddress = null;
        int appcount = 0;
        while ((line = reader.readLine()) != null) {
            matcher = p.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            if (isNotEmpty(matcher.group(2))) {
                appcount++;
                appName = matcher.group(2);
                statement = conn.createStatement();
                result = statement.executeQuery("select app_id from application where project_name = '" + appName + "'");
                if (result.next()) {
                    appid = result.getInt(1);
                } else {
                    throw new IllegalStateException("appName:" + appName + " has not match any app record");
                }
                result.close();
                statement.close();
            }
            group = isNotEmpty(matcher.group(3)) ? Integer.parseInt(matcher.group(3)) : 0;
            hostName = matcher.group(4);
            ipAddress = matcher.group(6);
            statement = conn.createStatement();
            result = statement.executeQuery("select gid from server_group where app_id =" + appid + " and runt_environment = 2 and group_index =" + group);
            if (result.next()) {
                groupid = result.getInt(1);
            }
            statement.close();
            result.close();
            if (groupid == null) {
                // create group
                prep = conn.prepareStatement("insert server_group(app_id,runt_environment,group_index,create_time)values(?,2,?,now())");
                prep.setInt(1, appid);
                prep.setInt(2, group);
                prep.execute();
                // if (result.next()) {
                groupid = getInsertId(conn);
                // }
                prep.close();
            // result.close();
            }
            if (groupid == null) {
                throw new IllegalStateException("appid:" + appid + " group:" + group + " can not create group index");
            }
            prep = conn.prepareStatement("insert server(gid,server_name,ip_address,create_time)values(?,?,?,now())");
            prep.setInt(1, groupid);
            prep.setString(2, hostName);
            prep.setString(3, ipAddress);
            prep.execute();
            prep.close();
            System.out.println(appName + group + " " + hostName + " " + ipAddress);
        }
        System.out.println("appcount:" + appcount);
        reader.close();
    }

    private static Integer getInsertId(Connection conn) {
        Statement statement = null;
        ResultSet result = null;
        try {
            statement = conn.createStatement();
            result = statement.executeQuery("select LAST_INSERT_ID();");
            if (result.next()) {
                return result.getInt(1);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                result.close();
            } catch (Throwable e) {
            }
            try {
                statement.close();
            } catch (Throwable e) {
            }
        }
    }

    private static boolean isNotEmpty(String value) {
        return value != null && value.length() > 0;
    }
}
