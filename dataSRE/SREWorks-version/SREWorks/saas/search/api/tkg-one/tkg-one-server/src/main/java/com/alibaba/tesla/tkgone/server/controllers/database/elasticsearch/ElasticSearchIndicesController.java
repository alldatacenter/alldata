package com.alibaba.tesla.tkgone.server.controllers.database.elasticsearch;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.RedisHelper;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchIndicesService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.mapper.IndexMapper;
import com.alibaba.tesla.web.controller.BaseController;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author jialiang.tjl
 * @date 2018/08/24 说明:
 * <p>
 * 没有处理
 */

@RestController
@RequestMapping("/database/elasticsearch/indices")
public class ElasticSearchIndicesController extends BaseController {

    @Autowired
    ElasticSearchIndicesService elasticSearchIndicesService;

    @Autowired
    RedisHelper redisHelper;

    @Autowired
    private IndexMapper indexMapper;

    @RequestMapping(value = "/delete/{index}", method = RequestMethod.DELETE)
    public TeslaBaseResult delete(@PathVariable String index) throws Exception {
        Set<String> realIndexes = indexMapper.getRealByAlias(index);
        if (CollectionUtils.isEmpty(realIndexes)) {
            return buildSucceedResult(0);
        }

        AcknowledgedResponse delete = null;
        for(String realIndex : realIndexes) {
            delete = this.elasticSearchIndicesService.delete(realIndex, index);
        }
        return buildSucceedResult(delete);
    }

    @RequestMapping(value = "/exists/{index}", method = RequestMethod.GET)
    public TeslaBaseResult exits(@PathVariable String index) throws Exception {
        boolean exists = this.elasticSearchIndicesService.exists(index);
        return buildSucceedResult(exists);
    }

    @RequestMapping(value = "/getStats/{index}", method = RequestMethod.GET)
    public TeslaBaseResult getStats(@PathVariable String index) throws Exception {
        JSONObject stats = this.elasticSearchIndicesService.getStats(index);
        return buildSucceedResult(stats);
    }

    @RequestMapping(value = "/getSize/{index}", method = RequestMethod.GET)
    public TeslaBaseResult getSize(@PathVariable String index) throws Exception {
        long size = this.elasticSearchIndicesService.getSize(index);
        return buildSucceedResult(size);
    }

    @RequestMapping(value = "/getHealth/{index}", method = RequestMethod.GET)
    public TeslaBaseResult getHealth(@PathVariable String index) throws Exception {
        JSONObject health = this.elasticSearchIndicesService.getHealth(index);
        return buildSucceedResult(health);
    }

    @RequestMapping(value = "/getCount/{index}", method = RequestMethod.GET)
    public TeslaBaseResult getCount(@PathVariable String index) throws Exception {
        return buildSucceedResult(this.elasticSearchIndicesService.getCount(index));
    }

    @RequestMapping(value = "/getIndices", method = RequestMethod.GET)
    public TeslaBaseResult getIndices() {
        Set<String> indices = indexMapper.getAliasIndexes();
        return buildSucceedResult(indices);
    }

    @RequestMapping(value = "/startReindexAll", method = RequestMethod.PUT)
    public TeslaBaseResult startReindexAll() throws Exception {
        List<String> uuids = new ArrayList<>();
        for (String index : indexMapper.getAliasIndexes()) {
            uuids.add(elasticSearchIndicesService.startReindex(index));
        }
        return buildSucceedResult(uuids);
    }

    @RequestMapping(value = "/startReindex/{index}", method = RequestMethod.PUT)
    public TeslaBaseResult startReindex(@PathVariable String index) throws Exception {
        return buildSucceedResult(elasticSearchIndicesService.startReindex(index));
    }

    @RequestMapping(value = "/statusReindex/{uuid}", method = RequestMethod.GET)
    public TeslaBaseResult statusReindex(@PathVariable String uuid) {
        return buildSucceedResult(redisHelper.hget(Constant.REDIS_REINDEX_STATUS, uuid));
    }

    @RequestMapping(value = "/removeField/{type}/{field}", method = RequestMethod.DELETE)
    public TeslaBaseResult removeField(@PathVariable String type, @PathVariable String field) throws Exception {
        return buildSucceedResult(
            elasticSearchIndicesService.removeTypeProperty(type, field)
        );
    }
}
