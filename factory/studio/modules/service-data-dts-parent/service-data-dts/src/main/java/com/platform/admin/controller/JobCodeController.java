package com.platform.admin.controller;

import com.platform.admin.mapper.JobInfoMapper;
import com.platform.admin.mapper.JobLogGlueMapper;
import com.platform.admin.core.util.I18nUtil;
import com.platform.admin.entity.JobInfo;
import com.platform.admin.entity.JobLogGlue;
import com.platform.core.biz.model.ReturnT;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;

import static com.platform.core.biz.model.ReturnT.FAIL_CODE;

/**
 *
 * @author AllDataDC
 * @date 2023/3/26 11:14
 * 任务状态接口
 **/
@RestController
@RequestMapping("/jobcode")
@Api(tags = "任务状态接口")
public class JobCodeController {

    @Resource
    private JobInfoMapper jobInfoMapper;
    @Resource
    private JobLogGlueMapper jobLogGlueMapper;


    @RequestMapping(value = "/save", method = RequestMethod.POST)
    @ApiOperation("保存任务状态")
    public ReturnT<String> save(Model model, int id, String glueSource, String glueRemark) {
        // valid
        if (glueRemark == null) {
            return new ReturnT<>(FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_glue_remark")));
        }
        if (glueRemark.length() < 4 || glueRemark.length() > 100) {
            return new ReturnT<>(FAIL_CODE, I18nUtil.getString("jobinfo_glue_remark_limit"));
        }
        JobInfo existsJobInfo = jobInfoMapper.loadById(id);
        if (existsJobInfo == null) {
            return new ReturnT<>(FAIL_CODE, I18nUtil.getString("jobinfo_glue_jobid_invalid"));
        }

        // update new code
        existsJobInfo.setGlueSource(glueSource);
        existsJobInfo.setGlueRemark(glueRemark);
        existsJobInfo.setGlueUpdatetime(new Date());

        existsJobInfo.setUpdateTime(new Date());
        jobInfoMapper.update(existsJobInfo);

        // log old code
        JobLogGlue jobLogGlue = new JobLogGlue();
        jobLogGlue.setJobId(existsJobInfo.getId());
        jobLogGlue.setGlueType(existsJobInfo.getGlueType());
        jobLogGlue.setGlueSource(glueSource);
        jobLogGlue.setGlueRemark(glueRemark);

        jobLogGlue.setAddTime(new Date());
        jobLogGlue.setUpdateTime(new Date());
        jobLogGlueMapper.save(jobLogGlue);

        // remove code backup more than 30
        jobLogGlueMapper.removeOld(existsJobInfo.getId(), 30);

        return ReturnT.SUCCESS;
    }

}
