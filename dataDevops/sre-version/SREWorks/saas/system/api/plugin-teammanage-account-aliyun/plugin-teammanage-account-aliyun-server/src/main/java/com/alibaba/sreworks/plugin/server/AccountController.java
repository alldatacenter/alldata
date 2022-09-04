package com.alibaba.sreworks.plugin.server;

import java.util.List;

import com.alibaba.sreworks.common.DTO.NameAlias;
import com.alibaba.sreworks.common.util.NameAliasUtil;

import com.aliyun.ecs20140526.Client;
import com.aliyun.ecs20140526.models.DescribeRegionsRequest;
import com.aliyun.teaopenapi.models.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
public class AccountController {

    @RequestMapping(value = "keys", method = RequestMethod.GET)
    public List<NameAlias> keys() {
        return NameAliasUtil.getNameAliasList(Account.class);
    }

    @RequestMapping(value = "check", method = RequestMethod.POST)
    public boolean check(@RequestBody Account account) throws Exception {
        Config config = new Config();
        config.accessKeyId = account.getAccessKeyId();
        config.accessKeySecret = account.getAccessKeySecret();
        config.endpoint = "ecs.aliyuncs.com";
        Client client = new Client(config);
        client.describeRegions(new DescribeRegionsRequest());
        return true;
    }

}
