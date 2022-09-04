package com.alibaba.sreworks.domain.repository;

import java.util.List;

import com.alibaba.sreworks.domain.DO.SreworksFile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * @author jinghua.yjh
 */
public interface SreworksFileRepository
    extends JpaRepository<SreworksFile, Long>, JpaSpecificationExecutor<SreworksFile> {

    SreworksFile findFirstById(Long id);

    SreworksFile findFirstByCategoryAndName(String category, String name);

    List<SreworksFile> findAllByCategory(String category);

    List<SreworksFile> findAllByCategoryAndType(String category, String type);

}
