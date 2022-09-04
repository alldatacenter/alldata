package com.alibaba.sreworks.job.master.jobscene.incident;

import com.alibaba.sreworks.job.master.jobscene.AbstractJobScene;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@EqualsAndHashCode(callSuper = true)
@Data
@Service
@Slf4j
public class IncidentJobScene extends AbstractJobScene<IncidentJobSceneConf> {

    public String type = "incident";

    @Override
    public Class<IncidentJobSceneConf> getConfClass() {
        return IncidentJobSceneConf.class;
    }

    @Override
    public IncidentJobSceneConf getConf(Long id) throws Exception {
        return new IncidentJobSceneConf();
    }

    @Override
    public void create(Long id, IncidentJobSceneConf conf) throws Exception {

    }

    @Override
    public void delete(Long id) throws Exception {

    }

    @Override
    public void modify(Long id, IncidentJobSceneConf conf) throws Exception {

    }

}
