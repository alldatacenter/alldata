package com.platform.devops.service;

import com.platform.devops.entity.Application;
import com.platform.devops.entity.Cluster;
import com.platform.devops.entity.Machine;
import com.platform.devops.entity.Project;

import java.util.List;

public interface MachineService {
    List<Machine> getAllMachine();
}
