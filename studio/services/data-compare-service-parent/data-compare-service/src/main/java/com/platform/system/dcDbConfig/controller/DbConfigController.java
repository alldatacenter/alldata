package com.platform.system.dcDbConfig.controller;

import com.platform.common.constant.UserConstants;
import com.platform.common.utils.poi.ExcelUtil;
import com.platform.common.aspectj.lang.annotation.Log;
import com.platform.common.aspectj.lang.enums.BusinessType;
import com.platform.common.web.controller.BaseController;
import com.platform.common.web.domain.AjaxResult;
import com.platform.common.web.page.TableDataInfo;
import com.platform.system.dcDbConfig.domain.Dbconfig;
import com.platform.system.dcDbConfig.service.IDbconfigService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据源DB信息操作处理
 *
 * @author AllDataDC
 */
@Controller
@RequestMapping("/dcDbConfig")
public class DbConfigController extends BaseController {
    private static final Logger log = LoggerFactory.getLogger(DbConfigController.class);

    private String prefix = "compare/dcDbConfig";

    @Autowired
    private IDbconfigService dbconfigService;

    @GetMapping()
    public String index() {
        return prefix + "/index";
    }

    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Dbconfig dbconfig) {
        startPage();
        List<Dbconfig> list = dbconfigService.selectDbconfigList(dbconfig);
        return getDataTable(list);
    }

    @Log(title = "数据源DB管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(Dbconfig dbconfig) {
        List<Dbconfig> list = dbconfigService.selectDbconfigList(dbconfig);
        ExcelUtil<Dbconfig> util = new ExcelUtil<Dbconfig>(Dbconfig.class);
        return util.exportExcel(list, "数据源DB数据");
    }

    @Log(title = "数据源DB管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        try {
            return toAjax(dbconfigService.deleteDbconfigByIds(ids));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    /**
     * 新增数据源DB
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("dbtypes", dbconfigService.selectDbTypesAll());
        return prefix + "/add";
    }

    /**
     * 新增保存数据源DB
     */
    @Log(title = "add", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(@Validated Dbconfig dbconfig) {
        if (UserConstants.DBCONFIG_NAME_NOT_UNIQUE.equals(dbconfigService.checkConnectNameUnique(dbconfig))) {
            return error("新增数据库连接名称'" + dbconfig.getConnectName() + "'失败，数据库连接名称已存在");
        }
        return toAjax(dbconfigService.insertDbconfig(dbconfig));
    }

    /**
     * 修改数据源DB
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("dbtypes", dbconfigService.selectDbTypesAll());
        Dbconfig dbconfig = dbconfigService.selectDbconfigById(id);
        mmap.put("dbconfig", dbconfig);
        return prefix + "/edit";
    }

    /**
     * 修改保存数据源DB
     */
    @Log(title = "数据源DB管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(@Validated Dbconfig dbconfig) {
        if (UserConstants.POST_NAME_NOT_UNIQUE.equals(dbconfigService.checkConnectNameUnique(dbconfig))) {
            return error("修改数据库连接名称'" + dbconfig.getConnectName() + "'失败，数据库连接名称已存在");
        }
        return toAjax(dbconfigService.updateDbconfig(dbconfig));
    }

    @PostMapping("/checkConnectNameUnique")
    @ResponseBody
    public String checkConnectNameUnique(Dbconfig dbconfig) {
        return dbconfigService.checkConnectNameUnique(dbconfig);
    }


    @RequestMapping(value = "/testConnection", method = RequestMethod.POST)
    @ResponseBody
    public AjaxResult testConnection(Dbconfig dbconfig) {
        try {
            dbconfigService.testConnection(dbconfig);
        } catch (Exception e) {
            e.printStackTrace();
            return error("错误信息:" + e.getMessage());
        }
        return success("测试连接成功");
    }
}
