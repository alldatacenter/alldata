package com.datasophon.worker.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.datasophon.common.Constants;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.RunAs;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.worker.utils.FreemakerUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.*;

public class ConfigureServiceHandler {
    private static final Logger logger = LoggerFactory.getLogger(ConfigureServiceHandler.class);

    private static final String RANGER_ADMIN = "RangerAdmin";

    public ExecResult configure(Map<Generators, List<ServiceConfig>> cofigFileMap,
                                String decompressPackageName,
                                Integer myid,
                                String serviceRoleName,
                                RunAs runAs) {
        ExecResult execResult = new ExecResult();
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            HashMap<String, String> paramMap = new HashMap<>();
            paramMap.put("${host}", hostName);
            paramMap.put("${user}", "root");
            paramMap.put("${myid}", myid + "");
            logger.info("start configure service role {}", serviceRoleName);
            for (Generators generators : cofigFileMap.keySet()) {
                List<ServiceConfig> configs = cofigFileMap.get(generators);
                String dataDir = "";
                Iterator<ServiceConfig> iterator = configs.iterator();
                ArrayList<ServiceConfig> customConfList = new ArrayList<>();
                while (iterator.hasNext()) {
                    ServiceConfig config = iterator.next();
                    logger.info("find config {}", config.getName());
                    if (StringUtils.isNotBlank(config.getType())) {
                        switch (config.getType()) {
                            case Constants.INPUT:
                                String value = PlaceholderUtils.replacePlaceholders((String) config.getValue(), paramMap, Constants.REGEX_VARIABLE);
                                config.setValue(value);
                                break;
                            case Constants.MULTIPLE:
                                conventToStr(config);
                                break;
                            default:
                                break;
                        }
                    }
                    if (Constants.PATH.equals(config.getConfigType())) {
                        createPath(config, runAs);
                    }
                    if (Constants.CUSTOM.equals(config.getConfigType())) {
                        addToCustomList(iterator, customConfList, config);
                    }
                    if (!config.isRequired() && !Constants.CUSTOM.equals(config.getConfigType())) {
                        iterator.remove();
                    }
                    if (config.getValue() instanceof Boolean || config.getValue() instanceof Integer) {
                        logger.info("convert boolean and integer to string");
                        config.setValue(config.getValue().toString());
                    }

                    if ("dataDir".equals(config.getName())) {
                        logger.info("find dataDir : {}", config.getValue());
                        dataDir = (String) config.getValue();
                    }
                    if ("TrinoCoordinator".equals(serviceRoleName) && "coordinator".equals(config.getName())) {
                        logger.info("start config trino coordinator");
                        config.setValue("true");
                        ServiceConfig serviceConfig = new ServiceConfig();
                        serviceConfig.setName("node-scheduler.include-coordinator");
                        serviceConfig.setValue("false");
                        customConfList.add(serviceConfig);
                    }
                    if ("fe_priority_networks".equals(config.getName()) || "be_priority_networks".equals(config.getName())) {
                        config.setName("priority_networks");
                    }

                }

                if (Objects.nonNull(myid) && StringUtils.isNotBlank(dataDir)) {
                    FileUtil.writeUtf8String(myid + "", dataDir + Constants.SLASH + "myid");
                }

                if ("node.properties".equals(generators.getFilename())) {
                    ServiceConfig serviceConfig = new ServiceConfig();
                    serviceConfig.setName("node.id");
                    serviceConfig.setValue(IdUtil.simpleUUID());
                    customConfList.add(serviceConfig);
                }
                configs.addAll(customConfList);
                if (configs.size() > 0) {
                    FreemakerUtils.generateConfigFile(generators, configs, decompressPackageName);
                }
                execResult.setExecOut("configure success");
                logger.info("configure success");
            }
            if (RANGER_ADMIN.equals(serviceRoleName) && !setupRangerAdmin(decompressPackageName)) {
                return execResult;
            }
            execResult.setExecResult(true);
        } catch (Exception e) {
            execResult.setExecErrOut(e.getMessage());
            e.printStackTrace();
        }
        return execResult;
    }

    private boolean setupRangerAdmin(String decompressPackageName) {
        logger.info("start to execute ranger admin setup.sh");
        ArrayList<String> commands = new ArrayList<>();
        commands.add(Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + Constants.SLASH + "setup.sh");
        ExecResult execResult = ShellUtils.execWithStatus(Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName, commands, 300L);

        ArrayList<String> globalCommand = new ArrayList<>();
        globalCommand.add(Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + Constants.SLASH + "set_globals.sh");
        ShellUtils.execWithStatus(Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName, globalCommand, 300L);
        if (execResult.getExecResult()) {
            logger.info("ranger admin setup success");
            return true;
        }
        logger.info("ranger admin setup failed");
        return false;
    }

    private void createPath(ServiceConfig config, RunAs runAs) {
        String path = (String) config.getValue();
        if (path.contains(Constants.COMMA)) {
            for (String dir : path.split(Constants.COMMA)) {
                mkdir(dir, runAs);
            }
        } else {
            mkdir(path, runAs);
        }
    }

    private void addToCustomList(Iterator<ServiceConfig> iterator, ArrayList<ServiceConfig> customConfList, ServiceConfig config) {
        List<JSONObject> list = (List<JSONObject>) config.getValue();
        iterator.remove();
        for (JSONObject json : list) {
            if (Objects.nonNull(json)) {
                Set<String> set = json.keySet();
                for (String key : set) {
                    if (StringUtils.isNotBlank(key)) {
                        ServiceConfig serviceConfig = new ServiceConfig();
                        serviceConfig.setName(key);
                        serviceConfig.setValue(json.get(key));
                        customConfList.add(serviceConfig);
                    }
                }
            }
        }
    }

    private String conventToStr(ServiceConfig config) {
        JSONArray value = (JSONArray) config.getValue();
        List<String> strs = value.toJavaList(String.class);
        logger.info("size is :{}", strs.size());
        String joinValue = String.join(config.getSeparator(), strs);
        config.setValue(joinValue);
        logger.info("config set value to {}", config.getValue());
        return joinValue;
    }

    private void mkdir(String path, RunAs runAs) {
        if (!FileUtil.exist(path)) {
            logger.info("create file path {}", path);
            FileUtil.mkdir(path);
            ShellUtils.addChmod(path, "755");
            if (Objects.nonNull(runAs)) {
                ShellUtils.addChown(path, runAs.getUser(), runAs.getGroup());
            }
        }
    }
}
