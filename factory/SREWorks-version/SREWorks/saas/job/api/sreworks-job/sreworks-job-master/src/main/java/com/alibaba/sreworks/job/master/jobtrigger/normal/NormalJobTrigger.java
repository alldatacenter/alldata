package com.alibaba.sreworks.job.master.jobtrigger.normal;

import com.alibaba.sreworks.job.master.jobtrigger.AbstractJobTrigger;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
@Service
public class NormalJobTrigger extends AbstractJobTrigger<NormalJobTriggerConf> {

    public String type = "normal";

    @Override
    public Class<NormalJobTriggerConf> getConfClass() {
        return NormalJobTriggerConf.class;
    }

    @Override
    public void create(Long id, NormalJobTriggerConf conf) throws Exception {

    }

    @Override
    public void delete(Long id) throws Exception {

    }

    @Override
    public void modify(Long id, NormalJobTriggerConf conf) throws Exception {

    }

    @Override
    public NormalJobTriggerConf getConf(Long id) throws Exception {
        return new NormalJobTriggerConf();
    }

    @Override
    public Map<Long, NormalJobTriggerConf> getConfBatch(Set<Long> ids) throws Exception {
        return new HashMap<>();
    }

    @Override
    public Boolean getState(Long id) throws Exception {
        return false;
    }

    @Override
    public Map<Long, Boolean> getStateBatch(Set<Long> ids) throws Exception {
        return new HashMap<>();
    }

    @Override
    public void toggleState(Long id, Boolean state) throws Exception{

    }
}
