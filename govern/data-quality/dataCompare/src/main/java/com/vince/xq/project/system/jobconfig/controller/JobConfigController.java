package com.vince.xq.project.system.jobconfig.controller;

import com.vince.xq.framework.aspectj.lang.annotation.Log;
import com.vince.xq.framework.aspectj.lang.enums.BusinessType;
import com.vince.xq.project.system.jobconfig.service.IJobconfigService;
import com.vince.xq.framework.web.controller.BaseController;
import com.vince.xq.framework.web.domain.AjaxResult;
import com.vince.xq.framework.web.page.TableDataInfo;
import com.vince.xq.project.system.dbconfig.service.IDbconfigService;
import com.vince.xq.project.system.instance.service.IInstanceService;
import com.vince.xq.project.system.jobconfig.domain.Jobconfig;
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
@RequestMapping("/system/jobconfig")
public class JobConfigController extends BaseController {
    private static final Logger log = LoggerFactory.getLogger(JobConfigController.class);

    private String prefix = "system/jobconfig";

    @Autowired
    private IJobconfigService jobconfigService;

    @Autowired
    private IDbconfigService dbconfigService;

    @Autowired
    private IInstanceService instanceService;

    @RequiresPermissions("system:jobconfig:view")
    @GetMapping()
    public String operlog() {
        return prefix + "/jobconfig";
    }

    @RequiresPermissions("system:jobconfig:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Jobconfig jobconfig) {
        startPage();
        List<Jobconfig> list = jobconfigService.selectJobconfigList(jobconfig);
        return getDataTable(list);
    }

    @RequiresPermissions("system:jobconfig:remove")
    @Log(title = "job管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        try {
            return toAjax(jobconfigService.deleteJobconfigByIds(ids));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("system:jobconfig:run")
    @Log(title = "job管理", businessType = BusinessType.RUN)
    @PostMapping("/run")
    @ResponseBody
    public AjaxResult run(String ids) {
        try {
            instanceService.runJob(ids);
            return success();
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    /**
     * 新增
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("dbConfigList", dbconfigService.selectDbconfigAll());
        return prefix + "/add";
    }

    /**
     * 新增保存
     */
    @RequiresPermissions("system:jobconfig:add")
    @Log(title = "add", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(@Validated Jobconfig jobconfig) {
        try {
            return toAjax(jobconfigService.insertJobconfig(jobconfig));
        } catch (Exception e) {
            e.printStackTrace();
            return error(e.getMessage());
        }
    }

    /**
     * 修改岗位
     */
    @RequiresPermissions("system:jobconfig:edit")
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        Jobconfig jobconfig = jobconfigService.selectJobconfigById(id);
        mmap.put("jobconfig", jobconfig);
        return prefix + "/edit";
    }

    /**
     * 修改保存岗位
     */
    @RequiresPermissions("system:jobconfig:edit")
    @Log(title = "job管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(@Validated Jobconfig jobconfig) {
        return toAjax(jobconfigService.updateJobconfig(jobconfig));
    }

    @RequestMapping(value = "/checkTableName", method = RequestMethod.POST)
    @ResponseBody
    public AjaxResult checkTableName(Jobconfig jobconfig) {
        try {
            log.info("========checkTableName==========");
            jobconfigService.checkTableName(jobconfig);
        } catch (Exception e) {
            e.printStackTrace();
            return error("错误信息:" + e.getMessage());
        }
        return success("表和字段校验成功");
    }

}
