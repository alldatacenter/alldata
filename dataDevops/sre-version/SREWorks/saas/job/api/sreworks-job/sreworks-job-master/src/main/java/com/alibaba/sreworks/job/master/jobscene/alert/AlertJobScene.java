package com.alibaba.sreworks.job.master.jobscene.alert;

import com.alibaba.sreworks.job.master.jobscene.AbstractJobScene;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@EqualsAndHashCode(callSuper = true)
@Data
@Service
@Slf4j
public class AlertJobScene extends AbstractJobScene<AlertJobSceneConf> {

    public String type = "alert";

    @Override
    public Class<AlertJobSceneConf> getConfClass() {
        return AlertJobSceneConf.class;
    }

    @Override
    public AlertJobSceneConf getConf(Long id) throws Exception {
        return new AlertJobSceneConf();
    }

    @Override
    public void create(Long id, AlertJobSceneConf conf) throws Exception {

    }

    @Override
    public void delete(Long id) throws Exception {

    }

    @Override
    public void modify(Long id, AlertJobSceneConf conf) throws Exception {

    }

}
