package cn.datax.service.system.controller;

import cn.datax.common.base.BaseController;
import cn.datax.common.security.annotation.DataInner;
import cn.datax.service.system.api.dto.LogDto;
import cn.datax.service.system.async.AsyncTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inner")
public class InnerController extends BaseController {

    @Autowired
    private AsyncTask asyncTask;

    @DataInner
    @PostMapping("/logs")
    public void saveLog(@RequestBody LogDto log) {
        asyncTask.doTask(log);
    }
}
