package com.alibaba.tesla.tkgone.server.consumer;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.RunCmd;
import com.alibaba.tesla.tkgone.server.common.Tools;
import com.alibaba.tesla.tkgone.server.domain.dto.ConsumerDto;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yangjinghua
 */
@Log4j
@Service
public class ScriptConsumer extends AbstractConsumer implements InitializingBean {

    @Override
    public void afterPropertiesSet() {
        int concurrentExecNum = consumerConfigService.getSingleConsumerScriptConcurrentExecNum();
        int effectiveInterval = consumerConfigService.getScriptEffectiveInterval();
        afterPropertiesSet(concurrentExecNum, effectiveInterval, ConsumerSourceType.script);
    }

    @Override
    int consumerDataByConsumerDto(ConsumerDto consumerDto) throws Exception {
        String workDir = "/home/admin/";
        String name = consumerDto.getName();
        String content = consumerDto.getSourceInfoJson().getString("content");
        String cmd = consumerDto.getSourceInfoJson().getString("cmd");
        cmd = Tools.processTemplateString(cmd,
                JSONObject.parseObject(JSONObject.toJSONString(consumerDto)).getInnerMap());
        int timeout = consumerDto.getSourceInfoJson().getIntValue("timeout");
        boolean isPartition = consumerDto.getSourceInfoJson().getBooleanValue("isPartition");

        Tools.writeToFile(String.format("%s/%s", workDir, name), content);
        RunCmd runCmd = new RunCmd();
        runCmd.start(cmd, workDir, null, timeout);
        List<JSONObject> allJsonObjectList = new ArrayList<>();
        List<JSONObject> jsonObjectList;
        String partition = isPartition ? Tools.currentDataString() : null;
        int alreadyFetchDataSize = 0;
        do {
            jsonObjectList = new ArrayList<>();
            for (String line : runCmd.getStdouts(Constant.FETCH_DATA_SIZE)) {
                try {
                    jsonObjectList.add(JSONObject.parseObject(line));
                } catch (Exception e) {
                    log.error(consumerDto.getName() + " 输出解析失败: ", e);
                }
            }
            allJsonObjectList.addAll(jsonObjectList);
            alreadyFetchDataSize += saveToBackendStore(consumerDto, jsonObjectList, consumerDto.getImportConfigArray(),
                    partition);
        } while (jsonObjectList.size() >= Constant.FETCH_DATA_SIZE);
        PrintWriter printWriter = new PrintWriter("/tmp/PrintWriter");
        for (JSONObject tmpJson : allJsonObjectList) {
            printWriter.println(JSONObject.toJSONString(tmpJson));
        }
        printWriter.close();
        log.info(String.format("%s fetchData: %s", consumerDto.getName(), alreadyFetchDataSize));
        if (runCmd.getStatus() != 0) {
            throw new Exception("脚本执行错误: " + runCmd.getStderr());
        }
        return alreadyFetchDataSize;
    }

}
