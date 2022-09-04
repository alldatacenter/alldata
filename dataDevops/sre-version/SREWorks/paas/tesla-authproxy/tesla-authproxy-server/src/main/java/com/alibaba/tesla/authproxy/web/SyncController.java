package com.alibaba.tesla.authproxy.web;

import com.alibaba.tesla.authproxy.BaseController;
import com.alibaba.tesla.authproxy.model.TeslaServiceExtAppDO;
import com.alibaba.tesla.authproxy.model.TeslaServiceUserDO;
import com.alibaba.tesla.authproxy.model.example.TeslaServiceExtAppExample;
import com.alibaba.tesla.authproxy.model.example.TeslaServiceUserExample;
import com.alibaba.tesla.authproxy.model.example.TeslaServiceUserExample.Criteria;
import com.alibaba.tesla.authproxy.service.SyncService;
import com.alibaba.tesla.common.base.TeslaResult;
import com.alibaba.tesla.common.utils.TeslaResultBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权代改造涉及数据同步（users表和ta_user;ext_apps和ta_app_ext）
 *
 * @author cdx
 * @date 2019/10/10
 */
@RestController
@RequestMapping("/sync")
public class SyncController extends BaseController {

    @Autowired
    private SyncService syncService;

    @PostMapping("/users")
    @ResponseBody
    public TeslaResult syncUsers(@RequestBody TeslaServiceUserDO teslaServiceUserDO) {
        TeslaServiceUserExample example = new TeslaServiceUserExample();
        example.setOrderByClause("userid desc");
        Criteria criteria = example.createCriteria();
        if (null != teslaServiceUserDO.getCreatetime()) {
            criteria.andCreatetimeGreaterThanOrEqualTo(teslaServiceUserDO.getCreatetime());
        }
        criteria.andValidflagEqualTo((byte)1);
        syncService.syncUserByExample(example);
        return TeslaResultBuilder.successResult();
    }

    @PostMapping("/apps")
    @ResponseBody
    public TeslaResult syncApps(@RequestBody TeslaServiceExtAppDO teslaServiceExtAppDO) {
        TeslaServiceExtAppExample teslaServiceExtAppExample = new TeslaServiceExtAppExample();
        TeslaServiceExtAppExample.Criteria criteria = teslaServiceExtAppExample.createCriteria();
        criteria.andValidflagEqualTo((byte)1);
        if (null != teslaServiceExtAppDO.getCreatetime()) {
            criteria.andCreatetimeGreaterThanOrEqualTo(teslaServiceExtAppDO.getCreatetime());
        }
        syncService.syncAppByExample(teslaServiceExtAppExample);
        return TeslaResultBuilder.successResult();
    }

}
