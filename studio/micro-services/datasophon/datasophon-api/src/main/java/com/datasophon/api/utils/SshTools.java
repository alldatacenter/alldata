package com.datasophon.api.utils;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.ChannelType;
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Objects;


public class SshTools {

    private static final Logger logger = LoggerFactory.getLogger(SshTools.class);

    private static final String SFTP = "sftp";


    /**
     * 获取实例
     *
     * @return 实例
     */
    public static SshTools getInstance() {
        return new SshTools();
    }

    /**
     * 登录主机
     * <p>
     * 密码登录
     *
     * @param hostIp   主机IP
     * @param hostPort 主机ssh端口
     * @param user     主机登录用户名
     * @param password 主机登录密码
     */
    private void loginByPassword(String hostIp,
                                 Integer hostPort,
                                 String user,
                                 String password) {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, hostIp, hostPort);
            session.setPassword(password);
            // 设置第一次登陆的时候提示，可选值:(ask | yes | no)
            session.setConfig("StrictHostKeyChecking", "no");
            // 连接超时
            session.connect(1000 * 10);
        } catch (JSchException e) {
            logger.error("登录时发生错误!", e);
        }
    }

    /**
     * 登录主机
     * <p>
     * 密钥登录
     *
     * @param hostIp     主机IP
     * @param hostPort   主机ssh端口
     * @param user       主机登录用户名
     * @param privateKey 密钥
     */
    public static Session createSession(String hostIp,
                                        Integer hostPort,
                                        String user,
                                        String privateKey) throws JSchException {

        JSch jsch = new JSch();
        jsch.addIdentity(privateKey);
        Session session = jsch.getSession(user, hostIp, hostPort);
        // 设置第一次登陆的时候提示，可选值:(ask | yes | no)
        session.setConfig("StrictHostKeyChecking", "no");
        // 连接超时
        session.connect(1000 * 100);
        return session;

    }

    public static String exec(Session session, String cmd, Charset charset, OutputStream errStream) throws JSchException, IOException {
        if (null == charset) {
            charset = CharsetUtil.CHARSET_UTF_8;
        }
        final ChannelExec channel = (ChannelExec) createChannel(session, ChannelType.EXEC);
        channel.setCommand(StrUtil.bytes(cmd, charset));
        channel.setInputStream(null);
        channel.setErrStream(errStream);
        InputStream in = null;
        try {
            channel.connect();
            in = channel.getInputStream();
            return IoUtil.read(in, charset);
        } finally {
            channel.disconnect();
            IoUtil.close(in);
        }
    }


    public static void upload(Session session,String targetDirectory, String uploadFile) throws FileNotFoundException, SftpException, JSchException {
        final ChannelSftp sftp = (ChannelSftp) createChannel(session, ChannelType.SFTP);
        sftp.cd(targetDirectory);
        File file = new File(uploadFile);
        sftp.put(new FileInputStream(file), file.getName());
        sftp.disconnect();
    }


    public static void download(String directory, String downloadFile, String saveFile, ChannelSftp sftp) throws SftpException, FileNotFoundException {
        sftp.cd(directory);
        File file = new File(saveFile);
        sftp.get(downloadFile, new FileOutputStream(file));
        sftp.disconnect();
    }


    public static Channel createChannel(Session session, ChannelType channelType) throws JSchException {
        Channel channel;
        if (false == session.isConnected()) {
            session.connect();
        }
        channel = session.openChannel(channelType.getValue());
        return channel;
    }

    /**
     * 登录后，主机执行指定命令
     *
     * @param command 命令
     * @return 执行命令后结果
     */
    public static String exec(Session session, String command) throws JSchException, IOException {
        logger.info("exe cmd: {}", command);

        byte[] tmp = new byte[1024];
        // 命令返回的结果
        StringBuilder resultBuffer = new StringBuilder();

        ChannelExec exec = (ChannelExec) session.openChannel("exec");
        exec.setCommand(command);
        exec.connect();

        // 返回结果流（命令执行错误的信息通过getErrStream获取）
        InputStream stdStream = exec.getInputStream();
        try {
            // 开始获得SSH命令的结果
            while (true) {
                while (stdStream.available() > 0) {
                    int i = stdStream.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    resultBuffer.append(new String(tmp, 0, i));
                }
                if (exec.isClosed()) {
                    break;
                }
                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                    logger.error("执行命令发生错误!", e);
                }
            }
        } finally {
            if (exec.isConnected()) {
                exec.disconnect();
            }
        }
        String result = resultBuffer.toString();
        logger.info("exe cmd return : {}", result);
        return result;
    }

    /**
     * 关闭连接
     */
    public static void close(Session session) {
        if (Objects.nonNull(session) && session.isConnected()) {
            session.disconnect();
        }
    }

    public static void main(String[] args) throws JSchException, IOException, SftpException {
        Session session = null;
        try {
            session = SshTools.createSession("172.31.96.16", 22, "root", "D:\\360Downloads\\id_rsa");
            String exec = SshTools.exec(session, "tar -zxvf /opt/datasophon/ddh/ddh-worker.tar.gz");
        }finally {
            SshTools.close(session);
        }
    }


}
