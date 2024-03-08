package com.platform.system.dcJob.controller;

import com.platform.common.utils.StringUtils;
import com.platform.common.utils.poi.ExcelUtil;
import com.platform.common.aspectj.lang.annotation.Log;
import com.platform.common.aspectj.lang.enums.BusinessType;
import com.platform.common.web.controller.BaseController;
import com.platform.common.web.domain.AjaxResult;
import com.platform.common.web.page.TableDataInfo;
import com.platform.system.dcJob.domain.Job;
import com.platform.system.dcJob.domain.JobLog;
import com.platform.system.dcJob.service.IJobLogService;
import com.platform.system.dcJob.service.IJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 调度日志操作处理
 * 
 * @author AllDataDC
 */
@Controller
@RequestMapping("/dcJobLog")
public class JobLogController extends BaseController
{
    private String prefix = "compare/dcJobLog";

    @Autowired
    private IJobService jobService;

    @Autowired
    private IJobLogService jobLogService;

    @GetMapping()
    public String index(@RequestParam(value = "jobId", required = false) Long jobId, ModelMap mmap)
    {
        if (StringUtils.isNotNull(jobId))
        {
            Job job = jobService.selectJobById(jobId);
            mmap.put("jobLog", job);
        }
        return prefix + "/index";
    }

    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(JobLog jobLog)
    {
        startPage();
        List<JobLog> list = jobLogService.selectJobLogList(jobLog);
        return getDataTable(list);
    }

    @Log(title = "调度日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(JobLog jobLog)
    {
        List<JobLog> list = jobLogService.selectJobLogList(jobLog);
        ExcelUtil<JobLog> util = new ExcelUtil<JobLog>(JobLog.class);
        return util.exportExcel(list, "调度日志");
    }

    @Log(title = "调度日志", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(jobLogService.deleteJobLogByIds(ids));
    }

    @GetMapping("/detail/{jobLogId}")
    public String detail(@PathVariable("jobLogId") Long jobLogId, ModelMap mmap)
    {
        mmap.put("name", "jobLog");
        mmap.put("jobLog", jobLogService.selectJobLogById(jobLogId));
        return prefix + "/detail";
    }

    @Log(title = "调度日志", businessType = BusinessType.CLEAN)
    @PostMapping("/clean")
    @ResponseBody
    public AjaxResult clean()
    {
        jobLogService.cleanJobLog();
        return success();
    }
}
