package com.alibaba.sreworks.job.worker.taskhandlers;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.taskhandler.ApiContent;
import com.alibaba.sreworks.job.taskhandler.ApiContentAction;
import com.alibaba.sreworks.job.utils.BeansUtil;
import com.alibaba.sreworks.job.utils.JsonUtil;
import com.alibaba.sreworks.job.worker.services.ElasticTaskInstanceService;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.CollectionUtils;

import java.net.http.HttpResponse;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class DatasetApiTaskHandler extends AbstractTaskHandler {

    public static String execType = "api";

    @Override
    public void execute() throws Exception {

        ApiContent apiContent = JSONObject.parseObject(getTaskInstance().getExecContent(), ApiContent.class);
        HttpResponse<String> response = ApiContentAction.run(apiContent);
        String responseBody = response.body();
        BeansUtil.context.getBean(ElasticTaskInstanceService.class)
            .update(getTaskInstance().getId(), JsonUtil.map("stdout", responseBody));
        if (response.statusCode() >= 300) {
            throw new Exception("response statusCode is " + response.statusCode() + ", body is " + responseBody);
        }
        patchVarConf(apiContent.getVarConfMap(), responseBody);

    }

    private void patchVarConf(Map<String, String> varConfMap, String responseBody) {

        JSONObject varConf = JSONObject.parseObject(getTaskInstance().getVarConf());
        varConf = varConf == null ? new JSONObject() : varConf;
        if (CollectionUtils.isEmpty(varConfMap)) {
            return;
        }
        JSONObject responseObject = JSONObject.parseObject(responseBody);

        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariables(responseObject);
        ExpressionParser parser = new SpelExpressionParser();

        for (String key : varConfMap.keySet()) {
            String value = varConfMap.get(key);
            value = parser.parseExpression(value).getValue(context, String.class);
            varConf.put(key, value);
        }
        getTaskInstance().setOutVarConf(JSONObject.toJSONString(varConf));

    }

}
