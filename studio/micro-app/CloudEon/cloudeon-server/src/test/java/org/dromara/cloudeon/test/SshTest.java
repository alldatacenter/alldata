package org.dromara.cloudeon.test;

import org.dromara.cloudeon.utils.SshUtils;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class SshTest {


    @Test
    public void execCmdWithResult() throws IOException {
        ClientSession session = SshUtils.openConnectionByPassword(
                "10.81.16.19",
                22,
                "root",
                "Ltcyhlwylym@admin2021zi!");

//        SshUtils.uploadFile(session,"/opt/edp/ZOOKEEPER1/conf", "/Users/huzekang/Downloads/apache-zookeeper-3.6.3-bin.tar.gz");
//        SshUtils.uploadLocalDirToRemote(session,"/opt/edp/ZOOKEEPER1/conf","/Volumes/Samsung_T5/opensource/e-mapreduce/work/ZOOKEEPER1/fl001/conf");
    }

    @Test
    public void upload() throws IOException {
        ClientSession session = SshUtils.openConnectionByPassword(
                "10.81.16.19",
                22,
                "root",
                "Ltcyhlwylym@admin2021zi!");

        SftpFileSystem fileSystem;
        try {
            fileSystem = SftpClientFactory.instance().createSftpFileSystem(session);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("打开sftp失败："+e);
        }
//        SshUtils.uploadLocalDirToRemote("/tmp/test/", "/Volumes/Samsung_T5/opensource/e-mapreduce/cloudeon-stack/UDH-1.0.0", fileSystem);
        SshUtils.uploadFile("/tmp/test/", "/Volumes/Samsung_T5/opensource/e-mapreduce/remote-script/check.sh", fileSystem);


    }



    @Test
    public void ssh2GetLog2() throws IOException, JSchException {

        JSch jsch = new JSch();
        Session session = jsch.getSession("root", "10.81.16.19", 22);
        session.setConfig("StrictHostKeyChecking", "no");
        session.setPassword("Ltcyhlwylym@admin2021zi!");
        session.connect();

        Channel channel = session.openChannel("exec");
        ((com.jcraft.jsch.ChannelExec) channel).setCommand("tail -f /opt/edp/monitor1/log/grafana.log\n");

        InputStream in = channel.getInputStream();
        channel.connect();

        FileWriter writer = new FileWriter("/Volumes/Samsung_T5/opensource/e-mapreduce/file.txt");
        byte[] buffer = new byte[1024];
        while (true) {
            int n = in.read(buffer);
            if (n < 0) {
                break;
            }
            writer.write(new String(buffer, 0, n));
            writer.flush();
        }
        channel.disconnect();
        session.disconnect();
    }




}
