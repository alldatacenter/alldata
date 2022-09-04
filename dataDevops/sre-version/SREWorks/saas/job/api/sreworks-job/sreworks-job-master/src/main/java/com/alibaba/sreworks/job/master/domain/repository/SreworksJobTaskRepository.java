package com.alibaba.sreworks.job.master.domain.repository;

import com.alibaba.sreworks.job.master.domain.DO.SreworksJobTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * @author jinghua.yjh
 */
public interface SreworksJobTaskRepository
    extends JpaRepository<SreworksJobTask, Long>, JpaSpecificationExecutor<SreworksJobTask> {

    SreworksJobTask findFirstById(Long id);

    List<SreworksJobTask> findAllBySceneType(String sceneType);

    List<SreworksJobTask> findAllByNameLikeOrderByIdDesc(String name);

    Page<SreworksJobTask> findAllByNameLikeAndExecTypeLikeOrderByIdDesc(
        String name, String execType, Pageable pageable);

}
