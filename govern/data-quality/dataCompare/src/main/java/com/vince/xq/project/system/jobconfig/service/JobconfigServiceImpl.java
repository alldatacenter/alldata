package com.vince.xq.project.system.jobconfig.service;

import com.vince.xq.common.constant.Constants;
import com.vince.xq.common.utils.security.ShiroUtils;
import com.vince.xq.common.utils.text.Convert;
import com.vince.xq.project.system.jobconfig.domain.Jobconfig;
import com.vince.xq.project.system.jobconfig.mapper.JobconfigMapper;
import com.vince.xq.project.common.DbTypeEnum;
import com.vince.xq.project.system.dbconfig.domain.Dbconfig;
import com.vince.xq.project.system.dbconfig.mapper.DbconfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 岗位信息 服务层处理
 *
 * @author ruoyi
 */
@Service
public class JobconfigServiceImpl implements IJobconfigService {

    @Autowired
    private JobconfigMapper jobconfigMapper;

    @Autowired
    private DbconfigMapper dbconfigMapper;

    @Override
    public List<Jobconfig> selectJobconfigList(Jobconfig dbconfig) {
        return jobconfigMapper.selectJobconfigList(dbconfig);
    }

    @Override
    public List<Jobconfig> selectJobconfigAll() {
        return jobconfigMapper.selectJobconfigAll();
    }

    /*@Override
    public List<Jobconfig> selectJobconfigsByUserId(Long userId)
    {
        List<Jobconfig> userJobconfigs = jobconfigMapper.selectJobconfigsByUserId(userId);
        List<Jobconfig> dbconfigs = jobconfigMapper.selectJobconfigAll();
        for (Jobconfig dbconfig : dbconfigs)
        {
            for (Jobconfig userJobconfig : userJobconfigs)
            {
                if (dbconfig.getId().longValue() == userJobconfig.getId().longValue())
                {
                    dbconfig.setFlag(true);
                    break;
                }
            }
        }
        return dbconfigs;
    }*/


    @Override
    public Jobconfig selectJobconfigById(Long id) {
        return jobconfigMapper.selectJobconfigById(id);
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
    public int deleteJobconfigByIds(String ids) {
        Long[] idsArray = Convert.toLongArray(ids);
        /*for (Long id : idsArray)
        {
            Jobconfig dbconfig = selectJobconfigById(id);
            if (countUserPostById(dbconfig) > 0)
            {
                throw new ServiceException(String.format("%1$s已分配,不能删除", post.getPostName()));
            }
        }*/
        return jobconfigMapper.deleteJobconfigByIds(idsArray);
    }


    @Override
    public int insertJobconfig(Jobconfig dbconfig) {
        dbconfig.setCreateBy(ShiroUtils.getLoginName());
        return jobconfigMapper.insertJobconfig(dbconfig);
    }


    @Override
    public int updateJobconfig(Jobconfig dbconfig) {
        dbconfig.setCreateBy(ShiroUtils.getLoginName());
        return jobconfigMapper.updateJobconfig(dbconfig);
    }

    @Override
    public int countUserPostById(Long postId) {
        return 0;
    }

    @Override
    public void checkTableName(Jobconfig jobconfig) throws Exception {
        Dbconfig dbconfig = dbconfigMapper.selectDbconfigById(jobconfig.getDbConfigId());
        checkTableNameSql(dbconfig, DbTypeEnum.findEnumByType(dbconfig.getType()).getConnectDriver(), jobconfig);
    }


    private void checkTableNameSql(Dbconfig dbconfig, String connectDriver, Jobconfig jobconfig) throws Exception {
        try {
            Class.forName(connectDriver);
        } catch (ClassNotFoundException e) {
            throw new Exception("注册驱动失败");
        }
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(dbconfig.getUrl(), dbconfig.getUserName(), dbconfig.getPwd());
            Statement stat = conn.createStatement();
            String sql = String.format(Constants.CHECK_TABLE_SQL, jobconfig.getOriginTablePrimary() + "," + jobconfig.getOriginTableFields(), jobconfig.getOriginTableName());

            ResultSet re = stat.executeQuery(sql);
            int i = 0;
            while (re.next()) {
                i++;
                //System.out.println(re.getString(1));
            }
            re.close();
            String targetSql = String.format(Constants.CHECK_TABLE_SQL, jobconfig.getToTablePrimary() + "," + jobconfig.getToTableFields(), jobconfig.getToTableName());
            ResultSet targetRe = stat.executeQuery(targetSql);
            int j = 0;
            while (targetRe.next()) {
                j++;
                //System.out.println(re.getString(1));
            }
            targetRe.close();
            stat.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
    }
}
