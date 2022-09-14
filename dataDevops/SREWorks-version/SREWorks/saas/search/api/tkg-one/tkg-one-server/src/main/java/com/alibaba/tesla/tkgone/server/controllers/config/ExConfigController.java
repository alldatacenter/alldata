package com.alibaba.tesla.tkgone.server.controllers.config;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.tkgone.server.common.ErrorUtil;
import com.alibaba.tesla.tkgone.server.common.annotation.timediff.TimeDiff;
import com.alibaba.tesla.tkgone.server.services.config.BaseConfigService;
import com.alibaba.tesla.tkgone.server.services.config.ExConfigService;
import com.alibaba.tesla.web.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * @author jialiang.tjl
 */
@RestController
@RequestMapping("/config/ex")
@Slf4j
public class ExConfigController extends BaseController {

    @Autowired
    BaseConfigService baseConfigService;

    @Autowired
    ExConfigService exConfigService;

    @RequestMapping(value = "/get", method = RequestMethod.POST)
    public TeslaBaseResult getName(@RequestBody JSONObject inJson) throws Exception {

        String name = inJson.getString("name");
        JSONObject exJson = inJson.getJSONObject("config");
        Object object = JSONObject.parse(baseConfigService.getNameContent(name));
        return buildSucceedResult(
            exConfigService.parseContent(object, exJson)
        );

    }

    @Override
    public String getUserEmailAddress() {
        return super.getUserEmailAddress();
    }

    @TimeDiff(name="[POST]获取外部数据/config/ex/get/${name}")
    @RequestMapping(value = "/get/{name}", method = RequestMethod.POST)
    public TeslaBaseResult getName(@PathVariable String name, @RequestBody JSONObject exJson) throws Exception {

        Object object = JSONObject.parse(baseConfigService.getNameContent(name));
        return buildSucceedResult(
            exConfigService.parseContent(object, exJson)
        );

    }

    @ExceptionHandler({Throwable.class})
    public TeslaBaseResult exception(Throwable error) {
        int code = HttpStatus.SC_INTERNAL_SERVER_ERROR;
        String message = null;
        String data = null;
        if (error instanceof IllegalArgumentException && !StringUtils.isEmpty(error.getMessage())) {
            code = HttpStatus.SC_BAD_REQUEST;
            message = "参数错误：" + error.getMessage();
            data = "";
        }

        if (StringUtils.isEmpty(message)) {
            message = "服务异常：" + error.toString();
        }

        if (data == null) {
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(os);
                error.printStackTrace(ps);
                ps.close();
                data = os.toString();
            } catch (Throwable e) {
                ErrorUtil.log(log, e, "process exception failed");
            }
        }

        return new TeslaBaseResult(code, message, data);
    }
}
