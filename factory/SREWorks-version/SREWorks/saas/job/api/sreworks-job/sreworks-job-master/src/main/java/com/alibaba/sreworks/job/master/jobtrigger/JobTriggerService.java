package com.alibaba.sreworks.job.master.jobtrigger;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Slf4j
@Service
public class JobTriggerService {

    @Autowired
    List<AbstractJobTrigger<? extends AbstractJobTriggerConf>> jobTriggerList;

    public List<String> listType() {
        return jobTriggerList.stream().map(AbstractJobTrigger::getType).collect(Collectors.toList());
    }

    public AbstractJobTrigger<? extends AbstractJobTriggerConf> getJobTrigger(String type) throws Exception {
        for (AbstractJobTrigger<? extends AbstractJobTriggerConf> jobTrigger : jobTriggerList) {
            if (type.equals(jobTrigger.getType())) {
                return jobTrigger;
            }
        }
        throw new Exception("can not find trigger by type: " + type);
    }

}
