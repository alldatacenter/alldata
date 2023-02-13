package com.alibaba.sreworks.job.master.jobscene.collect;

import com.alibaba.sreworks.job.master.jobscene.AbstractJobScene;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@EqualsAndHashCode(callSuper = true)
@Data
@Service
@Slf4j
public class CollectJobScene extends AbstractJobScene<CollectJobSceneConf> {

    public String type = "collect";

    @Override
    public Class<CollectJobSceneConf> getConfClass() {
        return CollectJobSceneConf.class;
    }

    @Override
    public CollectJobSceneConf getConf(Long id) throws Exception {
        return new CollectJobSceneConf();
    }

    @Override
    public void create(Long id, CollectJobSceneConf conf) throws Exception {

    }

    @Override
    public void delete(Long id) throws Exception {

    }

    @Override
    public void modify(Long id, CollectJobSceneConf conf) throws Exception {

    }

}
