package com.vince.xq.project.system.instance.service;

import com.vince.xq.project.system.instance.domain.Instance;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 岗位信息 服务层
 *
 * @author ruoyi
 */
public interface IInstanceService {

    public List<Instance> selectInstanceList(Instance instance);

    public List<Instance> selectInstanceAll();

    public Instance selectInstanceById(Long id);

    public List<String> selectDbTypesAll();

    public int insertInstance(Instance instance);

    public int countUserPostById(Long postId);

    public void runJob(String ids) throws Exception;

    public List<LinkedHashMap<String, String>> getDiffDetail(Long id) throws Exception;

}
