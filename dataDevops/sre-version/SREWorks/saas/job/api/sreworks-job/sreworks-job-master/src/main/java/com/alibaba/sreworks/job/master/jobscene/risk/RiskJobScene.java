package com.alibaba.sreworks.job.master.jobscene.risk;

import com.alibaba.sreworks.job.master.jobscene.AbstractJobScene;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@EqualsAndHashCode(callSuper = true)
@Data
@Service
@Slf4j
public class RiskJobScene extends AbstractJobScene<RiskJobSceneConf> {

    public String type = "risk";

    @Override
    public Class<RiskJobSceneConf> getConfClass() {
        return RiskJobSceneConf.class;
    }

    @Override
    public RiskJobSceneConf getConf(Long id) throws Exception {
        return new RiskJobSceneConf();
    }

    @Override
    public void create(Long id, RiskJobSceneConf conf) throws Exception {

    }

    @Override
    public void delete(Long id) throws Exception {

    }

    @Override
    public void modify(Long id, RiskJobSceneConf conf) throws Exception {

    }

}
