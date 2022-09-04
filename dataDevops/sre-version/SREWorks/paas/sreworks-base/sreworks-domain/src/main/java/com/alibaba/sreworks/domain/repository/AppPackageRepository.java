package com.alibaba.sreworks.domain.repository;

import java.util.List;

import com.alibaba.sreworks.domain.DO.App;
import com.alibaba.sreworks.domain.DO.AppPackage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 * @author jinghua.yjh
 */
public interface AppPackageRepository extends JpaRepository<AppPackage, Long>, JpaSpecificationExecutor<AppPackage> {

    AppPackage findFirstById(Long id);

    List<AppPackage> findAllByAppIdOrderByIdDesc(Long appId);

    List<AppPackage> findAllByOnSale(Integer onSale);

    List<AppPackage> findAllByStatusAndAppIdOrderByIdDesc(String status, Long appId);

    List<AppPackage> findAllByStatusNotIn(List<String> statusList);

    void deleteByAppId(Long appId);

    @Query(value = "select * from app where id in (select distinct app_id from app_package where on_sale = 1)"
        , nativeQuery = true)
    List<App> findAppWhichOnSale();

    int countByAppIdAndStatus(Long appId, String status);

}
