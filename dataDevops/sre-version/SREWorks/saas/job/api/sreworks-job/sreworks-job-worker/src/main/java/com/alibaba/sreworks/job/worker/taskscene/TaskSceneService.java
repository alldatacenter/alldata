package com.alibaba.sreworks.job.worker.taskscene;

import com.alibaba.sreworks.job.worker.taskscene.normal.NormalTaskScene;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TaskSceneService {

    @Autowired
    List<AbstractTaskScene<? extends AbstractTaskSceneConf>> taskSceneList;

    @Autowired
    NormalTaskScene normalTaskScene;

    public List<String> listType() {
        return taskSceneList.stream().map(AbstractTaskScene::getType).collect(Collectors.toList());
    }

    public AbstractTaskScene<? extends AbstractTaskSceneConf> getTaskScene(String type) {
        for (AbstractTaskScene<? extends AbstractTaskSceneConf> jobSchedule : taskSceneList) {
            if (type.equals(jobSchedule.getType())) {
                return jobSchedule;
            }
        }
        return normalTaskScene;
    }

}
