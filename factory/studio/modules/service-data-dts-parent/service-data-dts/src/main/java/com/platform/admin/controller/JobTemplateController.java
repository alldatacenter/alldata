package com.platform.admin.controller;


import com.platform.admin.base.BaseController;
import com.platform.admin.entity.JobTemplate;
import com.platform.admin.service.JobTemplateService;
import com.platform.core.biz.model.ReturnT;
import com.platform.core.util.DateUtil;
import com.platform.admin.core.cron.CronExpression;
import com.platform.admin.core.util.I18nUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 *
 * @author AllDataDC
 * @date 2023/3/26 11:14
 * 任务模板
 **/
@RestController
@RequestMapping("/jobTemplate")
@Api(tags = "任务配置接口")
public class JobTemplateController extends BaseController {

    @Resource
    private JobTemplateService jobTemplateService;

    @GetMapping("/pageList")
    @ApiOperation("任务模板列表")
    public ReturnT<Map<String, Object>> pageList(@RequestParam(value = "current", required = false, defaultValue = "0") int current,
                                        @RequestParam(value = "size", required = false, defaultValue = "10") int size,
                                                 @RequestParam(value = "jobGroup") int jobGroup, @RequestParam(value = "jobDesc") String jobDesc,
                                                 @RequestParam(value = "executorHandler") String executorHandler,
                                                 @RequestParam(value = "userId") int userId, @RequestParam(value = "projectIds",required = false) Integer[] projectIds) {

        return new ReturnT<>(jobTemplateService.pageList((current-1)*size, size, jobGroup, jobDesc, executorHandler, userId, projectIds));
    }

    @PostMapping("/add")
    @ApiOperation("添加任务模板")
    public ReturnT<String> add(HttpServletRequest request, @RequestBody JobTemplate jobTemplate) {
        jobTemplate.setUserId(getCurrentUserId(request));
        return jobTemplateService.add(jobTemplate);
    }

    @PostMapping("/update")
    @ApiOperation("更新任务")
    public ReturnT<String> update(HttpServletRequest request,@RequestBody JobTemplate jobTemplate) {
        jobTemplate.setUserId(getCurrentUserId(request));
        return jobTemplateService.update(jobTemplate);
    }

    @PostMapping(value = "/remove/{id}")
    @ApiOperation("移除任务模板")
    public ReturnT<String> remove(@PathVariable(value = "id") int id) {
        return jobTemplateService.remove(id);
    }

    @GetMapping("/nextTriggerTime")
    @ApiOperation("获取近5次触发时间")
    public ReturnT<List<String>> nextTriggerTime(String cron) {
        List<String> result = new ArrayList<>();
        try {
            CronExpression cronExpression = new CronExpression(cron);
            Date lastTime = new Date();
            for (int i = 0; i < 5; i++) {
                lastTime = cronExpression.getNextValidTimeAfter(lastTime);
                if (lastTime != null) {
                    result.add(DateUtil.formatDateTime(lastTime));
                } else {
                    break;
                }
            }
        } catch (ParseException e) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_field_cron_invalid"));
        }
        return new ReturnT<>(result);
    }
}
