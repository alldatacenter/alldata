package com.platform.system.dcJobInstance.service;

import com.platform.common.utils.text.Convert;
import com.platform.common.DbTypeEnum;
import com.platform.common.RunUtil;
import com.platform.system.dcDbConfig.domain.Dbconfig;
import com.platform.system.dcDbConfig.mapper.DbconfigMapper;
import com.platform.system.dcJobInstance.domain.Instance;
import com.platform.system.dcJobInstance.mapper.InstanceMapper;
import com.platform.system.dcJobConfig.domain.Jobconfig;
import com.platform.system.dcJobConfig.mapper.JobconfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 岗位信息 服务层处理
 *
 * @author AllDataDC
 */
@Service
public class InstanceServiceImpl implements IInstanceService {

    @Autowired
    private InstanceMapper instanceMapper;

    @Autowired
    private DbconfigMapper dbconfigMapper;

    @Autowired
    private JobconfigMapper jobconfigMapper;

    @Override
    public List<Instance> selectInstanceList(Instance instance) {
        return instanceMapper.selectInstanceList(instance);
    }

    @Override
    public List<Instance> selectInstanceAll() {
        return instanceMapper.selectInstanceAll();
    }

    @Override
    public Instance selectInstanceById(Long id) {
        return instanceMapper.selectInstanceById(id);
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
    public int insertInstance(Instance instance) {
        return instanceMapper.insertInstance(instance);
    }


    @Override
    public int countUserPostById(Long postId) {
        return 0;
    }

    @Override
    public void runJob(String ids) throws Exception {
        Long[] idsArray = Convert.toLongArray(ids);
        for (Long id : idsArray) {
            Jobconfig jobconfig = jobconfigMapper.selectJobconfigById(id);
            Dbconfig dbconfig = dbconfigMapper.selectDbconfigById(jobconfig.getDbConfigId());
            Instance instance = RunUtil.run(dbconfig, jobconfig);
            instance.setJobconfigId(id);
            instanceMapper.insertInstance(instance);
        }
    }

    @Override
    public List<LinkedHashMap<String, String>> getDiffDetail(Long id) throws Exception {
        Instance instance = instanceMapper.selectInstanceById(id);
        Jobconfig jobconfig = jobconfigMapper.selectJobconfigById(instance.getJobconfigId());
        Dbconfig dbconfig = dbconfigMapper.selectDbconfigById(jobconfig.getDbConfigId());
        List<LinkedHashMap<String, String>> list = RunUtil.runDiffDetail(dbconfig, jobconfig);
        return list;
    }
}
