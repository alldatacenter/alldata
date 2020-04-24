package com.platform.website.transformer.service.rpc.server;

import com.platform.website.transformer.service.rpc.IDimensionConverter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.ipc.Server;
import org.apache.log4j.Logger;

/**
 * IDimensionConverter服务接口的启动类
 * 
 * @author wulinhao
 *
 */
public class DimensionConverterServer {
    public static final String CONFIG_SAVE_PATH = "/WEBSITE/transformer/rpc/config";
    private static final Logger logger = Logger.getLogger(DimensionConverterServer.class);
    private AtomicBoolean isRunning = new AtomicBoolean(false); // 标识是否启动
    private Server server = null;// 服务对象
    private static String ClusterName = "nn";
    private static final String HADOOP_URL = "hdfs://" + ClusterName;
    private static Configuration conf = new Configuration();

    static {
        conf.set("fs.defaultFS", HADOOP_URL);
        conf.set("dfs.nameservices", ClusterName);
        conf.set("dfs.ha.namenodes." + ClusterName, "Master,Master2");
        conf.set("dfs.namenode.rpc-address." + ClusterName + ".Master", "192.168.52.156:9000");
        conf.set("dfs.namenode.rpc-address." + ClusterName + ".Master2", "192.168.52.147:9000");
        conf.set("dfs.client.failover.proxy.provider." + ClusterName,
            "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider");

//    conf.addResource("hbase-site.xml");
        conf.addResource("output-collector.xml");
        conf.addResource("query-mapping.xml");
        conf.addResource("transformer-env.xml");
    }


    public static void main(String[] args) {
        DimensionConverterServer dcs = new DimensionConverterServer(conf);
        dcs.startServer();
    }

    public DimensionConverterServer(Configuration conf) {
        this.conf = conf;

        /**
         * 当程序正常退出,系统调用 System.exit方法或虚拟机被关闭时才会执行添加的shutdownHook线程。
         * 其中shutdownHook是一个已初始化但并不有启动的线程，当jvm关闭的时候，
         * 会执行系统中已经设置的所有通过方法addShutdownHook添加的钩子，
         * 当系统执行完这些钩子后，jvm才会关闭。所以可通过这些钩子在jvm关闭的时候进行内存清理、资源回收等工作。
         */
        // 添加一个钩子，进行关闭操作
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DimensionConverterServer.this.stopServer();
                } catch (IOException e) {
                    // nothing
                }
            }
        }));
    }

    /**
     * 关闭服务
     * 
     * @throws IOException
     */
    public void stopServer() throws IOException {
        logger.info("关闭服务开始");
            try {
                this.removeListenerAddress();
            } finally {
                if (this.server != null) {
                Server tmp = this.server;
                this.server = null;
                tmp.stop();
            }
        }
        logger.info("关闭服务结束");
    }

    /**
     * 启动服务
     * 
     */
    public void startServer() {
        logger.info("开始启动服务");
        synchronized (this) {
            if (isRunning.get()) {
                // 启动完成
                return;
            }

                try {
                // 创建一个对象
                IDimensionConverter converter = new DimensionConverterImpl();
                // 创建服务
                this.server = new RPC.Builder(conf).setInstance(converter).setProtocol(IDimensionConverter.class).setVerbose(true).build();
                // 获取ip地址和端口号
                int port = this.server.getPort();
                String address = InetAddress.getLocalHost().getHostAddress();
                this.saveListenerAddress(address, port);
                // 启动
                this.server.start();
                // 标识成功
                isRunning.set(true);
                logger.info("启动服务成功，监听ip地址:" + address + "，端口:" + port);
            } catch (Throwable e) {
                isRunning.set(false);
                logger.error("启动服务发生异常", e);
                // 关闭可能异常创建的服务
                try {
                    this.stopServer();
                } catch (Throwable ee) {
                    // nothing
                }
                throw new RuntimeException("启动服务发生异常", e);
            }
        }

    }

    /**
     * 保存监听信息
     * 
     * @param address
     * @param port
     * @throws IOException
     */
    private void saveListenerAddress(String address, int port) throws IOException {
        // 删除已经存在的
        this.removeListenerAddress();

        // 进行数据输出操作
        FileSystem fs = null;
        BufferedWriter bw = null;

        try {
            fs = FileSystem.get(conf);
            Path path = new Path(CONFIG_SAVE_PATH);
            bw = new BufferedWriter(new OutputStreamWriter(fs.create(path)));
            bw.write(address);
            bw.newLine();
            bw.write(String.valueOf(port));
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    // nothing
                }
            }

            if (fs != null) {
                try {
                    fs.close();
                } catch (IOException e) {
                    // nothing
                }
            }
        }
    }

    /**
     * 删除监听信息
     * 
     * @throws IOException
     */
    private void removeListenerAddress() throws IOException {
        FileSystem fs = null;

        try {
            fs = FileSystem.get(conf);
            Path path = new Path(CONFIG_SAVE_PATH);
            if (fs.exists(path)) {
                // 存在，则删除
                fs.delete(path, true);
            }
        } finally {

            if (fs != null) {
                try {
                    fs.close();
                } catch (IOException e) {
                    // nothing
                }
            }
        }
    }
}
