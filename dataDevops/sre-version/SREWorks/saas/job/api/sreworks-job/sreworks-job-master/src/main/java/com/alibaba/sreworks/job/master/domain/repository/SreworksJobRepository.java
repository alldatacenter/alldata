package com.alibaba.sreworks.job.master.domain.repository;

import com.alibaba.sreworks.job.master.domain.DO.SreworksJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * @author jinghua.yjh
 */
public interface SreworksJobRepository
    extends JpaRepository<SreworksJob, Long>, JpaSpecificationExecutor<SreworksJob> {

    SreworksJob findFirstById(Long id);

    Page<SreworksJob> findAllByOrderByIdDesc(Pageable pageable);

    Page<SreworksJob> findAllBySceneTypeInAndScheduleTypeInAndTriggerTypeInOrderByIdDesc(
        List<String> sceneTypeList, List<String> scheduleType, List<String> triggerTypeList, Pageable pageable
    );

}
