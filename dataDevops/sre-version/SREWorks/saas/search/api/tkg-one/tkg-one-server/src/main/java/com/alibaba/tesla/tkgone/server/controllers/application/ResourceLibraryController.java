package com.alibaba.tesla.tkgone.server.controllers.application;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.tkgone.server.services.app.ResourceLibraryService;
import com.alibaba.tesla.web.controller.BaseController;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author yangjinghua
 */
@Log4j
@RestController
@RequestMapping("/application/resource")
public class ResourceLibraryController extends BaseController {

    @Autowired
    ResourceLibraryService resourceLibraryService;

    @RequestMapping(value = "/getAllSubChildren", method = RequestMethod.POST)
    public TeslaBaseResult getAllSubChildren(@RequestBody JSONObject jsonObject) throws Exception {

        String product = jsonObject.getString("product");
        String path = jsonObject.getString("path");
        if (StringUtils.isEmpty(path)) {
            path = "";
        }
        path = StringUtils.stripEnd(path, "/");
        List<String> pathWords = new ArrayList<>(Arrays.asList(StringUtils.split(path, "/")));
        pathWords.removeAll(Arrays.asList("", null));
        JSONArray retArray = new JSONArray();
        log.info("pathWords: " + JSONObject.toJSONString(pathWords, true));
        if (CollectionUtils.isEmpty(pathWords)) {
            retArray = resourceLibraryService.getAllSubChildren(product, "");
        } else {
            String parentPath = StringUtils.stripEnd(new File(path).getParent(), "/");
            JSONArray tmpArray = resourceLibraryService.getAllSubChildren(product, parentPath);
            for (JSONObject tmpJson : tmpArray.toJavaList(JSONObject.class)) {
                if (tmpJson.getString("path").equals(path)) {
                    retArray.add(tmpJson);
                }
            }
        }

        return buildSucceedResult(retArray);

    }

}
