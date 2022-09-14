package com.alibaba.tesla.tkgone.server.services.sreworks;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.services.config.BaseConfigService;
import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yangjinghua
 */
@Log4j
@Service
public class SreworksSearchQueryService extends BaseConfigService {

   public List<JSONObject> getAllTypes(String category) {
       List<String> indexs = categoryConfigService.getCategoryIndexes(category).toJavaList(String.class);
       List<JSONObject> results = new ArrayList<>();
       for (String index : indexs) {
           String alias = categoryConfigService.getCategoryTypeAlias(category, index);
           JSONObject result = new JSONObject();
           result.put("index", index);
           result.put("alias", alias);
           results.add(result);
       }

       return results;
   }

}
