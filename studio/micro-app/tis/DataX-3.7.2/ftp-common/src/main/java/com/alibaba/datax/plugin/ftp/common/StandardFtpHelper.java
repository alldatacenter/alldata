package com.alibaba.datax.plugin.ftp.common;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.plugin.reader.ftpreader.FtpReaderErrorCode;
import com.alibaba.datax.plugin.unstructuredstorage.reader.UnstructuredStorageReaderUtil;
import com.alibaba.datax.plugin.writer.ftpwriter.FtpWriterErrorCode;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class StandardFtpHelper extends FtpHelper {
    private static final Logger LOG = LoggerFactory.getLogger(StandardFtpHelper.class);
    FTPClient ftpClient = null;

    @Override
    public void loginFtpServer(String host, String username, String password, int port, int timeout,
                               String connectMode) {
        ftpClient = new FTPClient();
        try {
            ftpClient.setConnectTimeout(timeout);
            ftpClient.setDataTimeout(timeout);
            // 连接
            ftpClient.connect(host, port);
            // 登录
            ftpClient.login(username, password);
            // 不需要写死ftp server的OS TYPE,FTPClient getSystemType()方法会自动识别
            // ftpClient.configure(new FTPClientConfig(FTPClientConfig.SYST_UNIX));

            if ("PASV".equalsIgnoreCase(connectMode)) {
                ftpClient.enterRemotePassiveMode();
                ftpClient.enterLocalPassiveMode();
            } else if ("PORT".equalsIgnoreCase(connectMode)) {
                ftpClient.enterLocalActiveMode();
                // ftpClient.enterRemoteActiveMode(host, port);
            }
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                String message = String.format("与ftp服务器建立连接失败,请检查用户名和密码是否正确: [%s]",
                        "message:host =" + host + ",username = " + username + ",port =" + port);
                LOG.error(message);
                throw DataXException.asDataXException(FtpReaderErrorCode.FAIL_LOGIN, message);
            }
            //设置命令传输编码
            String fileEncoding = System.getProperty("file.encoding");
            ftpClient.setControlEncoding(fileEncoding);
        } catch (UnknownHostException e) {
            String message = String.format("请确认ftp服务器地址是否正确，无法连接到地址为: [%s] 的ftp服务器", host);
            LOG.error(message);
            throw DataXException.asDataXException(FtpReaderErrorCode.FAIL_LOGIN, message, e);
        } catch (IllegalArgumentException e) {
            String message = String.format("请确认连接ftp服务器端口是否正确，错误的端口: [%s] ", port);
            LOG.error(message);
            throw DataXException.asDataXException(FtpReaderErrorCode.FAIL_LOGIN, message, e);
        } catch (Exception e) {
            String message = String.format("与ftp服务器建立连接失败 : [%s]",
                    "message:host =" + host + ",username = " + username + ",port =" + port);
            LOG.error(message);
            throw DataXException.asDataXException(FtpReaderErrorCode.FAIL_LOGIN, message, e);
        }

    }

    @Override
    public void logoutFtpServer() {
        if (ftpClient.isConnected()) {
            try {
                //todo ftpClient.completePendingCommand();//打开流操作之后必须，原因还需要深究
                ftpClient.logout();
            } catch (IOException e) {
                String message = "与ftp服务器断开连接失败";
                LOG.error(message);
                throw DataXException.asDataXException(FtpReaderErrorCode.FAIL_DISCONNECT, message, e);
            } finally {
                if (ftpClient.isConnected()) {
                    try {
                        ftpClient.disconnect();
                    } catch (IOException e) {
                        String message = "与ftp服务器断开连接失败";
                        LOG.error(message);
                        throw DataXException.asDataXException(FtpReaderErrorCode.FAIL_DISCONNECT, message, e);
                    }
                }

            }
        }
    }

    @Override
    public boolean isDirExist(String directoryPath) {
        try {
            return ftpClient.changeWorkingDirectory(new String(directoryPath.getBytes(), FTP.DEFAULT_CONTROL_ENCODING));
        } catch (IOException e) {
            String message = String.format("进入目录：[%s]时发生I/O异常,请确认与ftp服务器的连接正常", directoryPath);
            LOG.error(message);
            throw DataXException.asDataXException(FtpReaderErrorCode.COMMAND_FTP_IO_EXCEPTION, message, e);
        }
    }

    @Override
    public boolean isFileExist(String filePath) {
        boolean isExitFlag = false;
        try {
            FTPFile[] ftpFiles = ftpClient.listFiles(new String(filePath.getBytes(), FTP.DEFAULT_CONTROL_ENCODING));
            if (ftpFiles.length == 1 && ftpFiles[0].isFile()) {
                isExitFlag = true;
            }
        } catch (IOException e) {
            String message = String.format("获取文件：[%s] 属性时发生I/O异常,请确认与ftp服务器的连接正常", filePath);
            LOG.error(message);
            throw DataXException.asDataXException(FtpReaderErrorCode.COMMAND_FTP_IO_EXCEPTION, message, e);
        }
        return isExitFlag;
    }

    @Override
    public boolean isSymbolicLink(String filePath) {
        boolean isExitFlag = false;
        try {
            FTPFile[] ftpFiles = ftpClient.listFiles(new String(filePath.getBytes(), FTP.DEFAULT_CONTROL_ENCODING));
            if (ftpFiles.length == 1 && ftpFiles[0].isSymbolicLink()) {
                isExitFlag = true;
            }
        } catch (IOException e) {
            String message = String.format("获取文件：[%s] 属性时发生I/O异常,请确认与ftp服务器的连接正常", filePath);
            LOG.error(message);
            throw DataXException.asDataXException(FtpReaderErrorCode.COMMAND_FTP_IO_EXCEPTION, message, e);
        }
        return isExitFlag;
    }

    HashSet<String> sourceFiles = new HashSet<String>();

    @Override
    public HashSet<String> getListFiles(String directoryPath, int parentLevel, int maxTraversalLevel) {
        if (parentLevel < maxTraversalLevel) {
            String parentPath = null;// 父级目录,以'/'结尾
            int pathLen = directoryPath.length();
            if (directoryPath.contains("*") || directoryPath.contains("?")) {
                // path是正则表达式
                String subPath = UnstructuredStorageReaderUtil.getRegexPathParentPath(directoryPath);
                if (isDirExist(subPath)) {
                    parentPath = subPath;
                } else {
                    String message = String.format("不能进入目录：[%s]," + "请确认您的配置项path:[%s]存在，且配置的用户有权限进入", subPath,
                            directoryPath);
                    LOG.error(message);
                    throw DataXException.asDataXException(FtpReaderErrorCode.FILE_NOT_EXISTS, message);
                }
            } else if (isDirExist(directoryPath)) {
                // path是目录
                if (directoryPath.charAt(pathLen - 1) == IOUtils.DIR_SEPARATOR) {
                    parentPath = directoryPath;
                } else {
                    parentPath = directoryPath + IOUtils.DIR_SEPARATOR;
                }
            } else if (isFileExist(directoryPath)) {
                // path指向具体文件
                sourceFiles.add(directoryPath);
                return sourceFiles;
            } else if (isSymbolicLink(directoryPath)) {
                //path是链接文件
                String message = String.format("文件:[%s]是链接文件，当前不支持链接文件的读取", directoryPath);
                LOG.error(message);
                throw DataXException.asDataXException(FtpReaderErrorCode.LINK_FILE, message);
            } else {
                String message = String.format("请确认您的配置项path:[%s]存在，且配置的用户有权限读取", directoryPath);
                LOG.error(message);
                throw DataXException.asDataXException(FtpReaderErrorCode.FILE_NOT_EXISTS, message);
            }

            try {
                FTPFile[] fs = ftpClient.listFiles(new String(directoryPath.getBytes(), FTP.DEFAULT_CONTROL_ENCODING));
                for (FTPFile ff : fs) {
                    String strName = ff.getName();
                    String filePath = parentPath + strName;
                    if (ff.isDirectory()) {
                        if (!(strName.equals(".") || strName.equals(".."))) {
                            //递归处理
                            getListFiles(filePath, parentLevel + 1, maxTraversalLevel);
                        }
                    } else if (ff.isFile()) {
                        // 是文件
                        sourceFiles.add(filePath);
                    } else if (ff.isSymbolicLink()) {
                        //是链接文件
                        String message = String.format("文件:[%s]是链接文件，当前不支持链接文件的读取", filePath);
                        LOG.error(message);
                        throw DataXException.asDataXException(FtpReaderErrorCode.LINK_FILE, message);
                    } else {
                        String message = String.format("请确认path:[%s]存在，且配置的用户有权限读取", filePath);
                        LOG.error(message);
                        throw DataXException.asDataXException(FtpReaderErrorCode.FILE_NOT_EXISTS, message);
                    }
                } // end for FTPFile
            } catch (IOException e) {
                String message = String.format("获取path：[%s] 下文件列表时发生I/O异常,请确认与ftp服务器的连接正常", directoryPath);
                LOG.error(message);
                throw DataXException.asDataXException(FtpReaderErrorCode.COMMAND_FTP_IO_EXCEPTION, message, e);
            }
            return sourceFiles;

        } else {
            //超出最大递归层数
            String message = String.format("获取path：[%s] 下文件列表时超出最大层数,请确认路径[%s]下不存在软连接文件", directoryPath, directoryPath);
            LOG.error(message);
            throw DataXException.asDataXException(FtpReaderErrorCode.OUT_MAX_DIRECTORY_LEVEL, message);
        }
    }

    @Override
    public InputStream getInputStream(String filePath) {
        try {
            AtomicBoolean closed = new AtomicBoolean(false);
            return new FilterInputStream(ftpClient.retrieveFileStream(new String(filePath.getBytes(), FTP.DEFAULT_CONTROL_ENCODING))) {
                @Override
                public void close() throws IOException {
                    super.close();
                    // 只能被关闭一次
                    if (closed.compareAndSet(false, true)) {
                        if (!ftpClient.completePendingCommand()) {
                            throw new IOException("completePendingCommand faild");
                        }
                    }
                }
            };
        } catch (IOException e) {
            String message = String.format("读取文件 : [%s] 时出错,请确认文件：[%s]存在且配置的用户有权限读取", filePath, filePath);
            LOG.error(message);
            throw DataXException.asDataXException(FtpReaderErrorCode.OPEN_FILE_ERROR, message);
        }
    }


    @Override
    public void mkDirRecursive(String directoryPath) {
        StringBuilder dirPath = new StringBuilder();
        dirPath.append(IOUtils.DIR_SEPARATOR_UNIX);
        String[] dirSplit = StringUtils.split(directoryPath, IOUtils.DIR_SEPARATOR_UNIX);
        String message = String.format("创建目录:%s时发生异常,请确认与ftp服务器的连接正常,拥有目录创建权限", directoryPath);
        try {
            // ftp server不支持递归创建目录,只能一级一级创建
            for (String dirName : dirSplit) {
                dirPath.append(dirName);
                boolean mkdirSuccess = mkDirSingleHierarchy(dirPath.toString());
                dirPath.append(IOUtils.DIR_SEPARATOR_UNIX);
                if (!mkdirSuccess) {
                    throw DataXException.asDataXException(
                            FtpWriterErrorCode.COMMAND_FTP_IO_EXCEPTION,
                            message);
                }
            }
        } catch (IOException e) {
            message = String.format("%s, errorMessage:%s", message,
                    e.getMessage());
            LOG.error(message);
            throw DataXException.asDataXException(
                    FtpWriterErrorCode.COMMAND_FTP_IO_EXCEPTION, message, e);
        }
    }

    private boolean mkDirSingleHierarchy(String directoryPath) throws IOException {
        boolean isDirExist = this.ftpClient
                .changeWorkingDirectory(directoryPath);
        // 如果directoryPath目录不存在,则创建
        if (!isDirExist) {
            int replayCode = this.ftpClient.mkd(directoryPath);
            if (replayCode != FTPReply.COMMAND_OK
                    && replayCode != FTPReply.PATHNAME_CREATED) {
                return false;
            }
        }
        return true;
    }


    @Override
    public Set<String> getAllFilesInDir(String dir, String prefixFileName) {
        Set<String> allFilesWithPointedPrefix = new HashSet<String>();
        try {
            boolean isDirExist = this.ftpClient.changeWorkingDirectory(dir);
            if (!isDirExist) {
                throw DataXException.asDataXException(
                        FtpWriterErrorCode.COMMAND_FTP_IO_EXCEPTION,
                        String.format("进入目录[%s]失败", dir));
            }
            this.printWorkingDirectory();
            FTPFile[] fs = this.ftpClient.listFiles(dir);
            // LOG.debug(JSON.toJSONString(this.ftpClient.listNames(dir)));
            LOG.debug(String.format("ls: %s",
                    JSON.toJSONString(fs, SerializerFeature.UseSingleQuotes)));
            for (FTPFile ff : fs) {
                String strName = ff.getName();
                if (strName.startsWith(prefixFileName)) {
                    allFilesWithPointedPrefix.add(strName);
                }
            }
        } catch (IOException e) {
            String message = String
                    .format("获取path:[%s] 下文件列表时发生I/O异常,请确认与ftp服务器的连接正常,拥有目录ls权限, errorMessage:%s",
                            dir, e.getMessage());
            LOG.error(message);
            throw DataXException.asDataXException(
                    FtpWriterErrorCode.COMMAND_FTP_IO_EXCEPTION, message, e);
        }
        return allFilesWithPointedPrefix;
    }

    private void printWorkingDirectory() {
        try {
            LOG.info(String.format("current working directory:%s",
                    this.ftpClient.printWorkingDirectory()));
        } catch (Exception e) {
            LOG.warn(String.format("printWorkingDirectory error:%s",
                    e.getMessage()));
        }
    }


    @Override
    public void deleteFiles(Set<String> filesToDelete) {
        String eachFile = null;
        boolean deleteOk = false;
        try {
            this.printWorkingDirectory();
            for (String each : filesToDelete) {
                LOG.info(String.format("delete file [%s].", each));
                eachFile = each;
                deleteOk = this.ftpClient.deleteFile(each);
                if (!deleteOk) {
                    String message = String.format(
                            "删除文件:[%s] 时失败,请确认指定文件有删除权限", eachFile);
                    throw DataXException.asDataXException(
                            FtpWriterErrorCode.COMMAND_FTP_IO_EXCEPTION,
                            message);
                }
            }
        } catch (IOException e) {
            String message = String.format(
                    "删除文件:[%s] 时发生异常,请确认指定文件有删除权限,以及网络交互正常, errorMessage:%s",
                    eachFile, e.getMessage());
            LOG.error(message);
            throw DataXException.asDataXException(
                    FtpWriterErrorCode.COMMAND_FTP_IO_EXCEPTION, message, e);
        }
    }

    @Override
    public OutputStream getOutputStream(String filePath, boolean append) {
        try {
            this.printWorkingDirectory();
            String parentDir = filePath.substring(0,
                    StringUtils.lastIndexOf(filePath, IOUtils.DIR_SEPARATOR));
            this.ftpClient.changeWorkingDirectory(parentDir);
            this.printWorkingDirectory();
            OutputStream writeOutputStream = append ? this.ftpClient
                    .appendFileStream(filePath) : this.ftpClient.storeFileStream(filePath);
            String message = String.format(
                    "打开FTP文件[%s]获取写出流时出错,请确认文件%s有权限创建，有权限写出等", filePath,
                    filePath);
            if (null == writeOutputStream) {
                throw DataXException.asDataXException(
                        FtpWriterErrorCode.OPEN_FILE_ERROR, message);
            }
            AtomicBoolean closed = new AtomicBoolean(false);
            return new FilterOutputStream(writeOutputStream) {
                @Override
                public void close() throws IOException {
                    super.close();
                    // 只能被关闭一次
                    if (closed.compareAndSet(false, true)) {
                        if (!ftpClient.completePendingCommand()) {
                            throw new IOException("completePendingCommand faild");
                        }
                    }
                }
            };
        } catch (IOException e) {
            String message = String.format(
                    "写出文件 : [%s] 时出错,请确认文件:[%s]存在且配置的用户有权限写, errorMessage:%s",
                    filePath, filePath, e.getMessage());
            LOG.error(message);
            throw DataXException.asDataXException(
                    FtpWriterErrorCode.OPEN_FILE_ERROR, message);
        }
    }
}
