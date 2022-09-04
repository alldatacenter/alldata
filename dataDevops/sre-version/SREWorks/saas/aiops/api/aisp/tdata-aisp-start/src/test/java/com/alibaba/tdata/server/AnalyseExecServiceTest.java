package com.alibaba.tdata.server;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tdata.aisp.server.common.utils.UuidUtil;
import com.alibaba.tdata.aisp.server.controller.param.CodeParam;
import com.alibaba.tdata.aisp.server.controller.param.TaskTrendQueryParam;
import com.alibaba.tdata.aisp.server.controller.result.TaskReportResult;
import com.alibaba.tdata.aisp.server.service.AnalyseExecuteService;
import com.alibaba.tdata.start.Application;

import com.taobao.pandora.boot.test.junit4.DelegateTo;
import com.taobao.pandora.boot.test.junit4.PandoraBootRunner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;

/**
 * @ClassName: AnalyseExecServiceTest
 * @Author: dyj
 * @DATE: 2021-12-13
 * @Description:
 **/
@Slf4j
@RunWith(PandoraBootRunner.class)
@DelegateTo(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
public class AnalyseExecServiceTest {
    @Autowired
    private AnalyseExecuteService executeService;

    @Test
    public void execTest(){
        String body = "{\"taskType\":\"sync\",\"series\":[[1635216096000,23.541],[1635216097000,33.541]]}";
        JSONObject param = JSONObject.parseObject(body);
        JSONObject exec = executeService.exec(UuidUtil.genUuid(), param, "bentoml_test", "bentoml_test");
        System.out.println(exec);
        assert exec!=null;
    }

    @Test
    public void getDocTest(){
        String bentoml_test = executeService.getDoc("bentoml_test");
        System.out.println(bentoml_test);
        assert !StringUtils.isEmpty(bentoml_test);
    }

    @Test
    public void codeTest() throws IOException, NoSuchAlgorithmException {
        JSONObject input = new JSONObject();
        input.put("taskType", "sync");
        CodeParam codeParam = CodeParam.builder().codeType("curl").url("http://test").input(input).build();
        String code = executeService.code(codeParam);
        System.out.println(code);
        assert StringUtils.isEmpty(code);
    }
}
