package com.alibaba.tesla.appmanager.server.controller;

import com.alibaba.tesla.appmanager.api.provider.UnitProvider;
import com.alibaba.tesla.appmanager.auth.controller.AppManagerBaseController;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.UnitCategoryDTO;
import com.alibaba.tesla.appmanager.domain.dto.UnitDTO;
import com.alibaba.tesla.appmanager.domain.req.unit.UnitQueryReq;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Unit Category
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@RequestMapping("/unit-categories")
@RestController
public class UnitCategoryController extends AppManagerBaseController {

    @Autowired
    private UnitProvider unitProvider;

    @GetMapping
    public TeslaBaseResult queryByCondition(OAuth2Authentication auth) {
        Pagination<UnitDTO> units = unitProvider.queryByCondition(UnitQueryReq.builder()
                .pageSize(DefaultConstant.UNLIMITED_PAGE_SIZE)
                .build());
        Set<String> categories = new HashSet<>();
        for (UnitDTO unit : units.getItems()) {
            if (StringUtils.isEmpty(unit.getCategory())) {
                categories.add("UNKNOWN");
            } else {
                categories.add(unit.getCategory());
            }
        }
        List<UnitCategoryDTO> unitCategories = new ArrayList<>();
        for (String category : categories) {
            unitCategories.add(UnitCategoryDTO.builder().category(category).build());
        }
        return buildSucceedResult(Pagination.valueOf(unitCategories, Function.identity()));
    }
}
