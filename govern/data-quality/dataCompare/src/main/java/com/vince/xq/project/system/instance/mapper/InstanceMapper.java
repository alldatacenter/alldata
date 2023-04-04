package com.vince.xq.project.system.instance.mapper;

import com.vince.xq.project.system.instance.domain.Instance;

import java.util.List;


public interface InstanceMapper {

    public List<Instance> selectInstanceList(Instance Instance);

    public List<Instance> selectInstanceAll();

    public Instance selectInstanceById(Long id);

    public int insertInstance(Instance Instance);

    public List<Instance> selectInstancesByUser(String createBy);
}
