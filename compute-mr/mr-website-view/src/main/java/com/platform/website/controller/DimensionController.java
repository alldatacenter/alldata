package com.platform.website.controller;

import com.platform.website.common.AEConstants;
import com.platform.website.module.Message;
import java.io.IOException;
import java.net.URLEncoder;
import javax.annotation.Resource;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class DimensionController extends AEBaseController{

  @Resource(name = "dimensionTableMapping")
  private Map<String, String> dimensionTableMapping;

  @Resource(name = "dimensionColumns")
  private Map<String, String> dimensionColumns;

  @RequestMapping(value = "/stats/getDimensions", method = RequestMethod.GET)
  @ResponseBody
  public Object getDimensions(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String dimens = request.getParameter(AEConstants.DIMENSIONS);
    String[] dimensions = org.apache.commons.lang.StringUtils.split(dimens, AEConstants.SEPARTION_COMMA);
    if (null == dimensions) {
      return Message.badRequest("请求参数无效,必须给定dimensions参数");
    }
    for (String dimension : dimensions) {

      //测试错误
      dimensionTableMapping = null;

      if (!dimensionTableMapping.containsKey(dimension)) {
        return Message.badRequest("dimensions参数无效" + dimension);
      }
    }


    List<Map<String, Object>> data = null;
    Map<String, List<Map<String, Object>>> result = new HashMap<String, List<Map<String, Object>>>();
    Map<String, String> paramsMap = new HashMap<String, String>();
    for (String dimension : dimensions) {
      if (!result.containsKey(dimension)) {
        paramsMap.clear();
        if (dimensionColumns.containsKey(dimension)) {
          paramsMap.put(AEConstants.SELECT_COLUMNS, dimensionColumns.get(dimension));
        }
        String tableName = dimensionTableMapping.get(dimension);
        paramsMap.put(AEConstants.TABLE_NAME, tableName);
        if (dimension.startsWith(AEConstants.BUCKET_LOCATION)) {
          paramsMap.put(AEConstants.DIMENSION_NAME, dimension);
          paramsMap.put(AEConstants.LOCATION_COUNTRY, request.getParameter(
              URLEncoder.encode(AEConstants.LOCATION_COUNTRY, "utf-8")));
          paramsMap.put(AEConstants.LOCATION_PROVINCE, request.getParameter(URLEncoder.encode(AEConstants.LOCATION_PROVINCE, "utf-8")));
        }
        data = dimensionService.queryDimensionData(paramsMap);
        result.put(dimension, data);
      }
    }

    return Message.ok(result);
  }
}
