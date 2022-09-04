package com.alibaba.sreworks.job.worker.taskscene.normal;

import com.alibaba.sreworks.job.worker.taskscene.AbstractTaskScene;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@EqualsAndHashCode(callSuper = true)
@Data
@Service
@Slf4j
public class NormalTaskScene extends AbstractTaskScene<NormalTaskSceneConf> {

    public String type = "collect";

    @Override
    public Class<NormalTaskSceneConf> getConfClass() {
        return NormalTaskSceneConf.class;
    }
    
}
