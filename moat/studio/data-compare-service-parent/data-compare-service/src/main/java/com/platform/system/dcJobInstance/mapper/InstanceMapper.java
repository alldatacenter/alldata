package com.platform.system.dcJobInstance.mapper;

import com.platform.system.dcJobInstance.domain.Instance;

import java.util.List;


public interface InstanceMapper {

    public List<Instance> selectInstanceList(Instance Instance);

    public List<Instance> selectInstanceAll();

    public Instance selectInstanceById(Long id);

    public int insertInstance(Instance Instance);

    public List<Instance> selectInstancesByUser(String createBy);
}
