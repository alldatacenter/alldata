package org.dromara.cloudeon.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SshUtils {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SshUtils.class);


    /**
     * 打开远程会话
     */
    public static ClientSession openConnection(String sshHost, Integer sshPort, String sshUser, String privateKey) {

        SshClient sshClient = SshClient.setUpDefaultClient();
        sshClient.start();
        ClientSession session = null;
        try {
            session = sshClient.connect(sshUser, sshHost, sshPort).verify().getClientSession();
            session.addPublicKeyIdentity(getKeyPairFromString(privateKey));
            if (session.auth().verify().isFailure()) {
                LOG.info("验证失败");
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LOG.info(sshHost + " 连接成功");
        return session;
    }

    /**
     * 打开远程会话
     */
    public static ClientSession openConnectionByPassword(String sshHost, Integer sshPort, String sshUser, String password) {

        SshClient sshClient = SshClient.setUpDefaultClient();
        sshClient.start();
        ClientSession session = null;
        try {
            session = sshClient.connect(sshUser, sshHost, sshPort).verify(10L, TimeUnit.SECONDS).getClientSession();
            session.addPasswordIdentity(password);
            if (session.auth().verify(10L, TimeUnit.SECONDS).isFailure()) {
                LOG.info("验证失败");
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LOG.info(sshHost + " 连接成功");
        return session;
    }


    /**
     * 关闭远程会话
     */
    public static void closeConnection(ClientSession session) {
        try {
            session.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取密钥对
     */
    static KeyPair getKeyPairFromString(String pk) {
        final KeyPairGenerator rsa;
        try {
            rsa = KeyPairGenerator.getInstance("RSA");
            final KeyPair keyPair = rsa.generateKeyPair();
            final ByteArrayOutputStream stream = new ByteArrayOutputStream();
            stream.write(pk.getBytes());
            final ObjectOutputStream o = new ObjectOutputStream(stream);
            o.writeObject(keyPair);
            return keyPair;
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 同步执行,需要获取执行完的结果
     *
     * @param session 连接
     * @param command 命令
     * @return 结果
     */
    public static String execCmdWithResult(ClientSession session, String command) throws IOException {
        session.resetAuthTimeout();
        LOG.info("exe cmd: {}", command);
        // 命令返回的结果
        ChannelExec channelExec = null;
        // 返回结果流
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 错误信息
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        channelExec = session.createExecChannel(command);
        channelExec.setOut(out);
        channelExec.setErr(err);
        // 执行并等待
        channelExec.open().verify(10L, TimeUnit.SECONDS);
        Set<ClientChannelEvent> events =
                channelExec.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), TimeUnit.SECONDS.toMillis(100000));
        //  检查请求是否超时
        if (events.contains(ClientChannelEvent.TIMEOUT)) {
            throw new RuntimeException("ssh 连接超时");
        }
        Integer exitStatus = channelExec.getExitStatus();
        LOG.info("mina result {}", exitStatus);
        if (exitStatus != 0) {
            String errorMsg = err.toString();
            if (!StrUtil.isNotBlank(errorMsg)) {
                errorMsg = out.toString();
            }
            throw new RuntimeException("ssh 执行命令失败：" + errorMsg);
        }
        LOG.info("exe cmd return : {}", out);
        return out.toString().trim();
    }

    /**
     * 上传单个文件到指定目录,相同路径ui覆盖
     *
     * @param remoteDirPath 远程目录地址
     * @param inputFile     文件 File
     */
    public static boolean uploadFile(String remoteDirPath, String inputFile, SftpFileSystem sftp) {
        File uploadFile = new File(inputFile);
        InputStream localInputStream = null;
        String remotePath = remoteDirPath + "/" + new File(inputFile).getName();
        try {
            OutputStream outputStream = sftp.getClient().write(remotePath);
            localInputStream = Files.newInputStream(uploadFile.toPath());
            IoUtil.copy(localInputStream, outputStream);
            return true;
        } catch (IOException e) {
            LOG.error("上传找到文件到远程服务上 {} 失败", remotePath);
            throw new RuntimeException(e);
        }
    }
    /**
     * 递归上传本地目录内所有子文件夹和子文件到远程文件夹中
     */
    public static void uploadDirectory(SftpFileSystem fs, File localDir, String remoteDir) throws IOException {
        // 创建远程目录
        SftpClient sftpClient = fs.getClient();
        createDir(null, remoteDir, fs);

        // 递归上传每个子文件和子目录
        for (File file : localDir.listFiles()) {
            String remoteFilePath = remoteDir + "/" + file.getName();
            if (file.isFile()) {
                // 上传文件
                try (FileInputStream in = new FileInputStream(file); OutputStream outputStream = sftpClient.write(remoteFilePath);) {
                    IoUtil.copy(in, outputStream);
                }
            } else {
                // 递归上传子目录
                uploadDirectory(fs, file, remoteFilePath);
            }
        }
    }
    /**
     * 将本地目录里所有文件拷贝到远程目录中
     *
     * @param remoteDirPath 需要以"/"结尾
     * @param localDir
     */
    public static void uploadLocalDirToRemote(String remoteDirPath, String localDir, SftpFileSystem sftp) throws IOException {

        Path localDirPath = Paths.get(localDir);
        Files.walk(localDirPath).forEach(file -> {

            File localFile = file.toFile();
            String localFileAbsolutePath = localFile.getAbsolutePath();
            String removeSuffix = StrUtil.removeSuffix(FileUtil.subPath(localDir, localFile), localFile.getName());
            if (!Files.isDirectory(file)) {
                String finalRemoteDir = remoteDirPath + removeSuffix;
                LOG.info("即将上传文件:{} 到远程目录:{}", localFileAbsolutePath, finalRemoteDir);
                uploadFile(finalRemoteDir, localFileAbsolutePath, sftp);
            } else {
                String finalRemoteDir = remoteDirPath + FileUtil.subPath(localDir, localFile);
                if (!finalRemoteDir.equals(remoteDirPath)){
                    createDir(null, finalRemoteDir, sftp);
                }

            }
        });

    }

    /**
     * 创建目录
     *
     * @param path
     * @return
     */
    public static boolean createDir(ClientSession session, String path, SftpFileSystem sftp) {
        try {
            Path remoteRoot = sftp.getDefaultDir().resolve(path);
            if (!Files.exists(remoteRoot)) {
                Files.createDirectories(remoteRoot);
                LOG.info("远程服务器上创建目录 {} 成功",remoteRoot);
                return true;
            }
        } catch (IOException e) {
            LOG.error("远程服务上创建目录 {} 失败", path);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return false;
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        ClientSession session = SshUtils.openConnection("localhost", 22, "huzekang",
                "/Users/huzekang/.ssh/id_rsa");
        String ls = SshUtils.execCmdWithResult(session, "java -version");
        System.out.println(ls);
//        boolean dir = MinaUtils.createDir(session,"/home/shinow/test/");
//        System.out.println(dir);
//        boolean uploadFile = MinaUtils.uploadFile(session, "/Users/liuxin/opt/test", "/Users/liuxin/Downloads/yarn-default.xml");
//        System.out.println(uploadFile);
    }

}
