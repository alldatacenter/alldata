package com.alibaba.datax.plugin.ftp.common;

import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.reader.ftpreader.Constant;
import com.alibaba.datax.plugin.reader.ftpreader.Key;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class FtpHelper implements AutoCloseable {
    public static final String KEY_META_FILE = "meta.json";
    public static FtpHelper createFtpClient(Configuration cfg) {
        String host = cfg.getString(Key.HOST);
        String protocol = cfg.getString(Key.PROTOCOL);
        String username = cfg.getString(Key.USERNAME);
        String password = cfg.getString(Key.PASSWORD);
        int timeout = cfg.getInt(Key.TIMEOUT, Constant.DEFAULT_TIMEOUT);
        int port = cfg.getInt(Key.PORT, Constant.DEFAULT_SFTP_PORT);
        String connectPattern = cfg.getUnnecessaryValue(Key.CONNECTPATTERN, Constant.DEFAULT_FTP_CONNECT_PATTERN, null);
        return createFtpClient(protocol, host, username, password, port, timeout, connectPattern);
    }

    public static FtpHelper createFtpClient(String protocol, String host
            , String username, String password, int port, int timeout, String connectPattern) {
        if (port < 1) {
            throw new IllegalArgumentException("ftp port can not small than 1");
        }
        FtpHelper ftpHelper = null;
        if ("sftp".equals(protocol)) {
            //sftp协议
            ftpHelper = new SftpHelper();
        } else if ("ftp".equals(protocol)) {
            // ftp 协议
            ftpHelper = new StandardFtpHelper();
        }
        ftpHelper.loginFtpServer(host, username, password, port, timeout, connectPattern);
        return ftpHelper;
    }

    /**
     * 支持目录递归创建
     */
    public abstract void mkDirRecursive(String directoryPath);

    /**
     * @param @param host
     * @param @param username
     * @param @param password
     * @param @param port
     * @param @param timeout
     * @param @param connectMode
     * @return void
     * @throws
     * @Title: LoginFtpServer
     * @Description: 与ftp服务器建立连接
     */
    protected abstract void loginFtpServer(String host, String username, String password, int port, int timeout, String connectMode);

    @Override
    public void close() throws Exception {
        this.logoutFtpServer();
    }

    /**
     * @param
     * @return void
     * @throws
     * @Title: LogoutFtpServer
     * todo 方法名首字母
     * @Description: 断开与ftp服务器的连接
     */
    public abstract void logoutFtpServer();

    /**
     * @param @param  directoryPath
     * @param @return
     * @return boolean
     * @throws
     * @Title: isDirExist
     * @Description: 判断指定路径是否是目录
     */
    public abstract boolean isDirExist(String directoryPath);

    /**
     * @param @param  filePath
     * @param @return
     * @return boolean
     * @throws
     * @Title: isFileExist
     * @Description: 判断指定路径是否是文件
     */
    public abstract boolean isFileExist(String filePath);

    /**
     * @param @param  filePath
     * @param @return
     * @return boolean
     * @throws
     * @Title: isSymbolicLink
     * @Description: 判断指定路径是否是软链接
     */
    public abstract boolean isSymbolicLink(String filePath);

    /**
     * @param @param  directoryPath
     * @param @param  parentLevel 父目录的递归层数（首次为0）
     * @param @param  maxTraversalLevel 允许的最大递归层数
     * @param @return
     * @return HashSet<String>
     * @throws
     * @Title: getListFiles
     * @Description: 递归获取指定路径下符合条件的所有文件绝对路径
     */
    public abstract HashSet<String> getListFiles(String directoryPath, int parentLevel, int maxTraversalLevel);

    /**
     * @param @param  filePath
     * @param @return
     * @return InputStream
     * @throws
     * @Title: getInputStream
     * @Description: 获取指定路径的输入流
     */
    public abstract InputStream getInputStream(String filePath);

    /**
     * @param @param  srcPaths 路径列表
     * @param @param  parentLevel 父目录的递归层数（首次为0）
     * @param @param  maxTraversalLevel 允许的最大递归层数
     * @param @return
     * @return HashSet<String>
     * @throws
     * @Title: getAllFiles
     * @Description: 获取指定路径列表下符合条件的所有文件的绝对路径
     */
    public HashSet<String> getAllFiles(List<String> srcPaths, int parentLevel, int maxTraversalLevel) {
        HashSet<String> sourceAllFiles = new HashSet<String>();
        if (!srcPaths.isEmpty()) {
            for (String eachPath : srcPaths) {
                sourceAllFiles.addAll(getListFiles(eachPath, parentLevel, maxTraversalLevel));
            }
        }
        return sourceAllFiles;
    }

    public abstract Set<String> getAllFilesInDir(String path, String fileName);

    /**
     * warn: 不支持文件夹删除, 比如 rm -rf
     */
    public abstract void deleteFiles(Set<String> filesToDelete);


    public final OutputStream getOutputStream(String filePath) {
        return this.getOutputStream(filePath, true);
    }

    public abstract OutputStream getOutputStream(String filePath, boolean append);
}
