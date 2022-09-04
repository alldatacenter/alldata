package com.alibaba.sreworks.job.master.jobscene.normal;

import com.alibaba.sreworks.job.master.jobscene.AbstractJobScene;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@EqualsAndHashCode(callSuper = true)
@Data
@Service
@Slf4j
public class NormalJobScene extends AbstractJobScene<NormalJobSceneConf> {

    public String type = "normal";

    @Override
    public Class<NormalJobSceneConf> getConfClass() {
        return NormalJobSceneConf.class;
    }

    @Override
    public NormalJobSceneConf getConf(Long id) throws Exception {
        return new NormalJobSceneConf();
    }

    @Override
    public void create(Long id, NormalJobSceneConf conf) throws Exception {

    }

    @Override
    public void delete(Long id) throws Exception {

    }

    @Override
    public void modify(Long id, NormalJobSceneConf conf) throws Exception {

    }

}
