package org.dromara.cloudeon;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import com.google.common.collect.Lists;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.io.output.NoCloseOutputStream;
import org.dromara.cloudeon.utils.SshConnectionPool;
import org.dromara.cloudeon.utils.SshUtils;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class SSHPoolTest {
    private static final Map<String, SshConnectionPool> pools = new HashMap<>();
    public static final int TIMES =500;
    public static final String command = " sudo docker  run --net=host -v /opt/edp/dolphinscheduler1/conf:/opt/edp/dolphinscheduler1/conf -v /opt/edp/dolphinscheduler1/log:/opt/edp/dolphinscheduler1/log   -v /opt/edp/dolphinscheduler1/data:/opt/edp/dolphinscheduler1/data  registry.mufankong.top/udh/dolphinscheduler:3.0.5 sh -c \"  /opt/edp/dolphinscheduler1/conf/init-dolphinscheduler-db.sh \"   \n";
    public static final ArrayList<String> serverList = Lists.newArrayList("fl002");
    public static final int PORT = 22;
    public static final String USERNAME = "root";
    public static final String PASSWORD = "Ltcyhlwylym@admin2021zi!";

    @Test
    public void executeCommandUsingPool() throws Exception {
        //循环100次，每次随机选一个节点执行
        for (int i = 0; i < TIMES; i++) {
            int randomInt = RandomUtil.randomInt(serverList.size());
            sayHelloUsingPoll(serverList.get(randomInt), command);
        }
    }

    @Test
    public void error() throws Exception {
        String server = "fl002";
        pools.put(server, new SshConnectionPool(server, PORT, USERNAME, PASSWORD));
        ClientSession clientSession = pools.get(server).borrowObject();
        SshUtils.execCmdWithResult(clientSession, command);

    }


    @Test
    public void executeCommandNoPool() throws Exception {
        //循环100次，每次随机选一个节点执行
        for (int i = 0; i < TIMES; i++) {
            int randomInt = RandomUtil.randomInt(serverList.size());
            sayHello(serverList.get(randomInt), command);
        }
    }

    private void sayHelloUsingPoll(String server, String command) throws Exception {
        SshConnectionPool pool = pools.get(server);
        if (pool == null) {
            pools.put(server, new SshConnectionPool(server, PORT, USERNAME, PASSWORD));
            pool = pools.get(server);
        }
        ClientSession session = pool.borrowObject();
        Assert.equals(SshUtils.execCmdWithResult(session, command), "ok!!!");
        pool.returnObject(session);
    }


    private void sayHello(String server, String command) throws Exception {
        try (ClientSession session = SshUtils.openConnectionByPassword(server, PORT, USERNAME, PASSWORD);) {

            Assert.equals(SshUtils.execCmdWithResult(session, command), "ok!!!");
        }


    }


}
