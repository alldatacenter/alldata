package com.platform.system.dcJobConfig.controller;

import com.platform.common.aspectj.lang.annotation.Log;
import com.platform.common.aspectj.lang.enums.BusinessType;
import com.platform.system.dcJobConfig.service.IJobconfigService;
import com.platform.common.web.controller.BaseController;
import com.platform.common.web.domain.AjaxResult;
import com.platform.common.web.page.TableDataInfo;
import com.platform.system.dcDbConfig.service.IDbconfigService;
import com.platform.system.dcJobInstance.service.IInstanceService;
import com.platform.system.dcJobConfig.domain.Jobconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务配置信息操作处理
 *
 * @author AllDataDC
 */
@Controller
@RequestMapping("/dcJobConfig")
public class JobConfigController extends BaseController {
    private static final Logger log = LoggerFactory.getLogger(JobConfigController.class);

    private String prefix = "compare/dcJobConfig";

    @Autowired
    private IJobconfigService jobconfigService;

    @Autowired
    private IDbconfigService dbconfigService;

    @Autowired
    private IInstanceService instanceService;

    @GetMapping()
    public String index() {
        return prefix + "/index";
    }

    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Jobconfig jobconfig) {
        startPage();
        List<Jobconfig> list = jobconfigService.selectJobconfigList(jobconfig);
        return getDataTable(list);
    }

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
     * 修改任务配置
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        Jobconfig jobconfig = jobconfigService.selectJobconfigById(id);
        mmap.put("jobconfig", jobconfig);
        return prefix + "/edit";
    }

    /**
     * 修改保存任务配置
     */
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
