package com.platform.system.dcDbConfig.service;

import com.platform.common.constant.Constants;
import com.platform.common.constant.UserConstants;
import com.platform.common.utils.StringUtils;
import com.platform.common.utils.text.Convert;
import com.platform.common.DbTypeEnum;
import com.platform.system.dcDbConfig.domain.Dbconfig;
import com.platform.system.dcDbConfig.mapper.DbconfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 岗位信息 服务层处理
 *
 * @author AllDataDC
 */
@Service
public class DbconfigServiceImpl implements IDbconfigService {

    @Autowired
    private DbconfigMapper dbconfigMapper;

    @Override
    public List<Dbconfig> selectDbconfigList(Dbconfig dbconfig) {
        return dbconfigMapper.selectDbconfigList(dbconfig);
    }

    @Override
    public List<Dbconfig> selectDbconfigAll() {
        return dbconfigMapper.selectDbconfigAll();
    }


    @Override
    public Dbconfig selectDbconfigById(Long id) {
        return dbconfigMapper.selectDbconfigById(id);
    }

    @Override
    public List<String> selectDbTypesAll() {
        List<String> list = new ArrayList<>();
        for (DbTypeEnum dbTypeEnum : DbTypeEnum.values()) {
            list.add(dbTypeEnum.getType());
        }
        return list;
    }

    @Override
    public int deleteDbconfigByIds(String ids) {
        Long[] idsArray = Convert.toLongArray(ids);
        return dbconfigMapper.deleteDbconfigByIds(idsArray);
    }


    @Override
    public int insertDbconfig(Dbconfig dbconfig) {
        dbconfig.setCreateBy("admin");
        return dbconfigMapper.insertDbconfig(dbconfig);
    }


    @Override
    public int updateDbconfig(Dbconfig dbconfig) {
        dbconfig.setCreateBy("admin");
        return dbconfigMapper.updateDbconfig(dbconfig);
    }

    @Override
    public int countUserPostById(Long postId) {
        return 0;
    }


    @Override
    public String checkConnectNameUnique(Dbconfig dbconfig) {
        Dbconfig info = dbconfigMapper.checkConnectNameUnique(dbconfig.getConnectName());
        if (StringUtils.isNotNull(info)) {
            return UserConstants.DBCONFIG_NAME_NOT_UNIQUE;
        }
        return UserConstants.DBCONFIG_NAME_UNIQUE;
    }

    @Override
    public void testConnection(Dbconfig dbconfig) throws Exception {
        DbTypeEnum dbTypeEnum = DbTypeEnum.findEnumByType(dbconfig.getType());
        if (dbTypeEnum == null) {
            throw new Exception("不识别的类型");
        } else {
            testConnectionDriver(dbconfig, dbTypeEnum.getConnectDriver());
        }
    }


    private void testConnectionDriver(Dbconfig dbconfig, String connectDriver) throws Exception {
        try {
            Class.forName(connectDriver);
        } catch (ClassNotFoundException e) {
            throw new Exception("注册驱动失败");
        }
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(dbconfig.getUrl(), dbconfig.getUserName(), dbconfig.getPwd());
            Statement stat = conn.createStatement();
            ResultSet re = stat.executeQuery(Constants.TEST_CONNECT_SQL);
            int i = 0;
            while (re.next()) {
                i++;
                //System.out.println(re.getString(1));
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
