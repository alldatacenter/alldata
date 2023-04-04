import com.vince.xq.common.constant.Constants;
import com.vince.xq.project.common.DbTypeEnum;
import com.vince.xq.project.common.RunUtil;
import com.vince.xq.project.system.dbconfig.domain.Dbconfig;
import com.vince.xq.project.system.jobconfig.domain.Jobconfig;
import org.junit.jupiter.api.Test;

import java.sql.*;

public class XqTest {
    @Test
    public void testSql() throws Exception {
        Dbconfig dbconfig = new Dbconfig();
        dbconfig.setType(DbTypeEnum.MySQL.getType());
        dbconfig.setUrl("jdbc:mysql://ip:3306");
        dbconfig.setUserName("root");
        dbconfig.setPwd("123456");
        Jobconfig jobconfig = new Jobconfig();
        jobconfig.setOriginTableName("test_db.user_info");
        jobconfig.setOriginTablePrimary("id");
        jobconfig.setOriginTableFields("name,age");
        jobconfig.setToTableName("test_db.user_info_copy");
        jobconfig.setToTablePrimary("id");
        jobconfig.setToTableFields("name,age");

        RunUtil.run(dbconfig, jobconfig);
    }

    @Test
    public void strFormate() {
        String str = "%sxx%sxx%s";
        String str1 = String.format(str, "a", "b", "c");
        System.out.println(str1);
    }

    @Test
    public void testHive() throws Exception {
        String url="jdbc:hive2://ip:10000/default";

        try {
            Class.forName(DbTypeEnum.Hive.getConnectDriver());
        } catch (ClassNotFoundException e) {
            throw new Exception("注册驱动失败");
        }
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, "", "");
            Statement stat = conn.createStatement();
            ResultSet re = stat.executeQuery(Constants.TEST_CONNECT_SQL);
            int i = 0;
            while (re.next()) {
                i++;
                System.out.println(re.getString(1));
            }
            re.close();
            stat.close();
            conn.close();
            if (i == 0) {
                throw new Exception("该连接下没有库");
            }
        } catch (SQLException e) {
            throw new Exception("连接数据库失败");
        }
    }
}
