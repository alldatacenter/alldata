package com.datasophon.worker.actor;

import akka.actor.UntypedActor;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.command.GetLogCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.worker.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

public class LogActor extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger(LogActor.class);

    @Override
    public void onReceive(Object msg) throws Throwable {
        if (msg instanceof GetLogCommand) {
            logger.info("get query log command");
            GetLogCommand command = (GetLogCommand) msg;
            HashMap<String, String> paramMap = new HashMap<>();
            String hostName = InetAddress.getLocalHost().getHostName();
            paramMap.put("${user}", "root");
            paramMap.put("${host}", hostName);
            String logFileName = PlaceholderUtils.replacePlaceholders(command.getLogFile(), paramMap, Constants.REGEX_VARIABLE);
            
            ExecResult execResult = new ExecResult();
            String logStr = "can not find log file";
            if (logFileName.startsWith(StrUtil.SLASH) && FileUtil.exist(logFileName)) {
                logStr = FileUtils.readLastRows(logFileName, Charset.defaultCharset(), PropertyUtils.getInt("rows"));
            } else if (FileUtil.exist(Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName() + Constants.SLASH + logFileName)) {
                logStr = FileUtils.readLastRows(Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName() + Constants.SLASH + logFileName, Charset.defaultCharset(), PropertyUtils.getInt("rows"));
            }else{
                String workDir = System.getProperty("user.dir");
                logStr = FileUtils.readLastRows(workDir +"/logs/datasophon-worker.log", Charset.defaultCharset(), PropertyUtils.getInt("rows"));
            }
            execResult.setExecResult(true);
            execResult.setExecOut(logStr);
            getSender().tell(execResult, getSelf());
        } else {
            unhandled(msg);
        }
    }
}
