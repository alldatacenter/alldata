package com.datasophon.worker.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.GenerateAlertConfigCommand;
import com.datasophon.common.model.AlertItem;
import com.datasophon.common.model.Generators;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.worker.utils.FreemakerUtils;


import java.util.HashMap;
import java.util.List;

public class AlertConfigActor extends UntypedActor {
    @Override
    public void onReceive(Object message) throws Throwable, Throwable {
        if(message instanceof GenerateAlertConfigCommand){
            GenerateAlertConfigCommand command = (GenerateAlertConfigCommand) message;
            ExecResult execResult = new ExecResult();
            HashMap<Generators, List<AlertItem>> configFileMap = command.getConfigFileMap();
            for (Generators generators : configFileMap.keySet()) {
                List<AlertItem> alertItems = configFileMap.get(generators);
                FreemakerUtils.generatePromAlertFile(generators,alertItems,generators.getFilename().replace(".yml","").toUpperCase());
            }
            execResult.setExecResult(true);
            getSender().tell(execResult,getSelf());
        }else {
            unhandled(message);
        }
    }
}
