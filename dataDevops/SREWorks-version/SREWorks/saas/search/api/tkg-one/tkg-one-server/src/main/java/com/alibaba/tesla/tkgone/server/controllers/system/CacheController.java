package com.alibaba.tesla.tkgone.server.controllers.system;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.tkgone.server.common.Cache;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.RedisHelper;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.mapper.IndexMapper;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.mapper.PartitionMapper;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yangjinghua
 */
@Api(tags = "Admin - Cache")
@RestController
@RequestMapping("/system/cache")
public class CacheController extends BaseController {

    @Autowired
    private RedisHelper redisHelper;

    @Autowired
    private PartitionMapper partitionMapper;

    @Autowired
    private IndexMapper indexMapper;

    @ApiOperation("获取所有的索引")
    @GetMapping(value = "/indexes")
    public TeslaBaseResult getIndexes() {
        return buildSucceedResult(indexMapper.getAliasIndexes());
    }

    @ApiOperation("获取所有索引分区")
    @RequestMapping(value = "/indexPartitions")
    public TeslaBaseResult getIndexPartitions() {
        JSONObject retJson = new JSONObject();
        for (String aliasIndex : indexMapper.getAliasIndexes()) {
            retJson.put(aliasIndex, partitionMapper.get(aliasIndex));
        }
        return buildSucceedResult(retJson);
    }

    @GetMapping(value = "/realIndexes")
    public TeslaBaseResult getRealIndexes() {
        return buildSucceedResult(indexMapper.getRealIndexes());
    }

    @GetMapping(value = "/reindexCache")
    public TeslaBaseResult getReindexCache() {
        return buildSucceedResult(redisHelper.hgetAll(Constant.REDIS_REINDEX_STATUS));
    }

    @ApiOperation("获取所有的内存配置")
    @GetMapping(value = "/allConfigDto")
    public TeslaBaseResult getAllConfigDto() {
        return buildSucceedResult(Cache.allConfigDto);
    }

    @RequestMapping(value = "/resetTypeCache", method = RequestMethod.PUT)
    public TeslaBaseResult resetTypeCache(String type) {
        redisHelper.delAll(type);
        return buildSucceedResult("清空缓存成功");
    }

}
