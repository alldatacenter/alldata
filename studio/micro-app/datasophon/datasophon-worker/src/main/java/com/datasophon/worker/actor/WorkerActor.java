package com.datasophon.worker.actor;

import akka.actor.*;
import com.alibaba.fastjson.JSONObject;
import com.datasophon.common.model.StartWorkerMessage;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;

import java.io.IOException;

public class WorkerActor extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger(WorkerActor.class);


    @Override
    public void preRestart(Throwable reason, Option<Object> message) {
        logger.info("worker actor restart by reason {}",reason.getMessage());
    }

    @Override
    public void preStart() throws IOException {
        ActorRef installServiceActor = getContext().actorOf(Props.create(InstallServiceActor.class), getActorRefName(InstallServiceActor.class));
        ActorRef configureServiceActor = getContext().actorOf(Props.create(ConfigureServiceActor.class), getActorRefName(ConfigureServiceActor.class));
        ActorRef startServiceActor = getContext().actorOf(Props.create(StartServiceActor.class), getActorRefName(StartServiceActor.class));
        ActorRef stopServiceActor = getContext().actorOf(Props.create(StopServiceActor.class), getActorRefName(StopServiceActor.class));
        ActorRef restartServiceActor = getContext().actorOf(Props.create(RestartServiceActor.class), getActorRefName(RestartServiceActor.class));
        ActorRef logActor = getContext().actorOf(Props.create(LogActor.class), getActorRefName(LogActor.class));
        ActorRef executeCmdActor = getContext().actorOf(Props.create(ExecuteCmdActor.class), getActorRefName(ExecuteCmdActor.class));
        ActorRef fileOperateActor = getContext().actorOf(Props.create(FileOperateActor.class), getActorRefName(FileOperateActor.class));
        ActorRef alertConfigActor = getContext().actorOf(Props.create(AlertConfigActor.class), getActorRefName(AlertConfigActor.class));
        ActorRef unixUserActor = getContext().actorOf(Props.create(UnixUserActor.class), getActorRefName(UnixUserActor.class));
        ActorRef unixGroupActor = getContext().actorOf(Props.create(UnixGroupActor.class), getActorRefName(UnixGroupActor.class));
        ActorRef kerberosActor = getContext().actorOf(Props.create(KerberosActor.class), getActorRefName(KerberosActor.class));

        getContext().watch(installServiceActor);
        getContext().watch(configureServiceActor);
        getContext().watch(startServiceActor);
        getContext().watch(stopServiceActor);
        getContext().watch(restartServiceActor);
        getContext().watch(logActor);
        getContext().watch(executeCmdActor);
        getContext().watch(fileOperateActor);
        getContext().watch(alertConfigActor);
        getContext().watch(unixUserActor);
        getContext().watch(unixGroupActor);
        getContext().watch(kerberosActor);
    }

    /** Get ActorRef name from Class name. */
    private String getActorRefName(Class clazz) {
        return StringUtils.uncapitalize(clazz.getSimpleName());
    }


    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof String) {

        }  else if(message instanceof Terminated){
            Terminated t = (Terminated) message;
            logger.info("find actor {} terminated",t.getActor());
        }
        else {
            unhandled(message);
        }
    }


    public static void main(String[] args) {
        String str = "{coreNum: 8, totalMem: 31.4189, totalDisk: 991.51,usedDisk: 9.59, diskAvail: 981.92,usedMem:5.91602,memUsedPersent:18.8295,diskUsedPersent:1.0,averageLoad:0.06}";
        StartWorkerMessage message = JSONObject.parseObject(str, StartWorkerMessage.class);
    }

}
