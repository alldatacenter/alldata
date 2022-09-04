package com.alibaba.sreworks.job.master.jobscene;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Slf4j
@Service
public class JobSceneService {

    @Autowired
    List<AbstractJobScene<? extends AbstractJobSceneConf>> jobSceneList;

    public List<String> listType() {
        return jobSceneList.stream().map(AbstractJobScene::getType).collect(Collectors.toList());
    }

    public AbstractJobScene<? extends AbstractJobSceneConf> getJobScene(String type) throws Exception {
        for (AbstractJobScene<? extends AbstractJobSceneConf> jobScene : jobSceneList) {
            if (type.equals(jobScene.getType())) {
                return jobScene;
            }
        }
        throw new Exception("can not find Scene by type: " + type);
    }

}
