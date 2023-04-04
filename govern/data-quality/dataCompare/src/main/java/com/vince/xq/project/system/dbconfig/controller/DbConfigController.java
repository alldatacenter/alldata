package com.vince.xq.project.system.dbconfig.controller;

import com.vince.xq.common.constant.UserConstants;
import com.vince.xq.common.utils.poi.ExcelUtil;
import com.vince.xq.framework.aspectj.lang.annotation.Log;
import com.vince.xq.framework.aspectj.lang.enums.BusinessType;
import com.vince.xq.framework.web.controller.BaseController;
import com.vince.xq.framework.web.domain.AjaxResult;
import com.vince.xq.framework.web.page.TableDataInfo;
import com.vince.xq.project.system.dbconfig.domain.Dbconfig;
import com.vince.xq.project.system.dbconfig.service.IDbconfigService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 岗位信息操作处理
 *
 * @author ruoyi
 */
@Controller
@RequestMapping("/system/dbconfig")
public class DbConfigController extends BaseController {
    private static final Logger log = LoggerFactory.getLogger(DbConfigController.class);

    private String prefix = "system/dbconfig";

    @Autowired
    private IDbconfigService dbconfigService;

    @RequiresPermissions("system:dbconfig:view")
    @GetMapping()
    public String operlog() {
        return prefix + "/dbconfig";
    }

    @RequiresPermissions("system:dbconfig:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Dbconfig dbconfig) {
        startPage();
        List<Dbconfig> list = dbconfigService.selectDbconfigList(dbconfig);
        return getDataTable(list);
    }

    @Log(title = "岗位管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("system:dbconfig:export")
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(Dbconfig dbconfig) {
        List<Dbconfig> list = dbconfigService.selectDbconfigList(dbconfig);
        ExcelUtil<Dbconfig> util = new ExcelUtil<Dbconfig>(Dbconfig.class);
        return util.exportExcel(list, "岗位数据");
    }

    @RequiresPermissions("system:dbconfig:remove")
    @Log(title = "岗位管理", businessType = BusinessType.DELETE)
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
     * 新增岗位
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("dbtypes", dbconfigService.selectDbTypesAll());
        return prefix + "/add";
    }

    /**
     * 新增保存岗位
     */
    @RequiresPermissions("system:dbconfig:add")
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
     * 修改岗位
     */
    @RequiresPermissions("system:dbconfig:edit")
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("dbtypes", dbconfigService.selectDbTypesAll());
        Dbconfig dbconfig = dbconfigService.selectDbconfigById(id);
        mmap.put("dbconfig", dbconfig);
        return prefix + "/edit";
    }

    /**
     * 修改保存岗位
     */
    @RequiresPermissions("system:dbconfig:edit")
    @Log(title = "岗位管理", businessType = BusinessType.UPDATE)
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
