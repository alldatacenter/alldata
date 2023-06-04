package com.datasophon.worker;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.EventStream;
import akka.remote.AssociatedEvent;
import akka.remote.AssociationErrorEvent;
import akka.remote.DisassociatedEvent;
import com.alibaba.fastjson.JSONObject;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.remote.CreateUnixUserCommand;
import com.datasophon.common.lifecycle.ServerLifeCycleManager;
import com.datasophon.common.model.StartWorkerMessage;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.worker.actor.RemoteEventActor;
import com.datasophon.worker.actor.UnixUserActor;
import com.datasophon.worker.actor.WorkerActor;
import com.datasophon.worker.utils.UnixUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WorkerApplicationServer  {

    private static final Logger logger = LoggerFactory.getLogger(WorkerApplicationServer.class);

    private static final String USER_DIR = "user.dir";

    private static final String MASTER_HOST= "masterHost";

    private static final String WORKER = "worker";

    private static final String SH = "sh";

    private static final String NODE = "node";



    public static void main(String[] args) throws UnknownHostException {
        String hostname = InetAddress.getLocalHost().getHostName();
        String workDir = System.getProperty(USER_DIR);
        String masterHost = PropertyUtils.getString(MASTER_HOST);
        String cpuArchitecture = ShellUtils.getCpuArchitecture();

        CacheUtils.put(Constants.HOSTNAME,hostname);
        //init actor
        ActorSystem system = initActor(hostname);

        subscribeRemoteEvent(system);

        startNodeExporter(workDir, cpuArchitecture);

        Map<String,String> userMap = new HashMap<String,String>();
        initUserMap(userMap);

        createDefaultUser(userMap);

        tellToMaster(hostname, workDir, masterHost, cpuArchitecture, system);
        logger.info("start worker");

        /*
         * registry hooks, which are called before the process exits
         */
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!ServerLifeCycleManager.isStopped()) {
                close("WorkerServer shutdown hook");
            }
        }));
    }

    private static void initUserMap(Map<String, String> userMap) {
        userMap.put("hdfs","hadoop");
        userMap.put("yarn","hadoop");
        userMap.put("hive","hadoop");
        userMap.put("mapred","hadoop");
        userMap.put("hbase","hadoop");
        userMap.put("elastic","elastic");
    }

    private static void createDefaultUser(Map<String,String> userMap) {
        for (String user : userMap.keySet()) {
            String group = userMap.get(user);
            if(!UnixUtils.isGroupExists(group)){
                UnixUtils.createUnixGroup(group);
            };
            UnixUtils.createUnixUser(user,group,null);
        }
    }



    private static ActorSystem initActor(String hostname) {
        Config config = ConfigFactory.parseString("akka.remote.netty.tcp.hostname=" + hostname);
        ActorSystem system = ActorSystem.create(Constants.DATASOPHON, config.withFallback(ConfigFactory.load()));
        system.actorOf(Props.create(WorkerActor.class), WORKER);
        return system;
    }

    private static void subscribeRemoteEvent(ActorSystem system) {
        ActorRef remoteEventActor = system.actorOf(Props.create(RemoteEventActor.class), "remoteEventActor");
        EventStream eventStream = system.eventStream();
        eventStream.subscribe(remoteEventActor, AssociationErrorEvent.class);
        eventStream.subscribe(remoteEventActor, AssociatedEvent.class);
        eventStream.subscribe(remoteEventActor, DisassociatedEvent.class);
    }


    private static void tellToMaster(String hostname, String workDir, String masterHost, String cpuArchitecture, ActorSystem system) {
        ActorSelection workerStartActor = system.actorSelection("akka.tcp://datasophon@" + masterHost + ":2551/user/workerStartActor");
        ExecResult result = ShellUtils.exceShell(workDir + "/script/host-info-collect.sh");
        logger.info("host info collect result:{}", result);
        StartWorkerMessage startWorkerMessage = JSONObject.parseObject(result.getExecOut(), StartWorkerMessage.class);
        startWorkerMessage.setCpuArchitecture(cpuArchitecture);
        startWorkerMessage.setClusterId(PropertyUtils.getInt("clusterId"));
        startWorkerMessage.setHostname(hostname);
        workerStartActor.tell(startWorkerMessage, ActorRef.noSender());
    }

    public static void close(String cause) {
        logger.info("Worker server stopped, current cause: {}", cause);
        String workDir = System.getProperty("user.dir");
        String cpuArchitecture = ShellUtils.getCpuArchitecture();
        operateNodeExporter(workDir, cpuArchitecture, "stop");
        logger.info("Worker server stopped, current cause: {}", cause);
    }

    private static void startNodeExporter(String workDir, String cpuArchitecture) {
        operateNodeExporter(workDir, cpuArchitecture, "restart");
    }

    private static void operateNodeExporter(String workDir, String cpuArchitecture, String operate) {
        ArrayList<String> commands = new ArrayList<>();
        commands.add(SH);
        if (Constants.x86_64.equals(cpuArchitecture)) {
            commands.add(workDir + "/node/x86/control.sh");
        } else {
            commands.add(workDir + "/node/arm/control.sh");
        }
        commands.add(operate);
        commands.add(NODE);
        ShellUtils.execWithStatus(Constants.INSTALL_PATH, commands, 60L);
    }
}
