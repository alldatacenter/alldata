package com.vince.xq.project.system.instance.service;

import com.vince.xq.common.utils.text.Convert;
import com.vince.xq.project.common.DbTypeEnum;
import com.vince.xq.project.common.RunUtil;
import com.vince.xq.project.system.dbconfig.domain.Dbconfig;
import com.vince.xq.project.system.dbconfig.mapper.DbconfigMapper;
import com.vince.xq.project.system.instance.domain.Instance;
import com.vince.xq.project.system.instance.mapper.InstanceMapper;
import com.vince.xq.project.system.jobconfig.domain.Jobconfig;
import com.vince.xq.project.system.jobconfig.mapper.JobconfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 岗位信息 服务层处理
 *
 * @author ruoyi
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
