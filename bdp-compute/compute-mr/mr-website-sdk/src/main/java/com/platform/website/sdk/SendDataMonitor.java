package com.platform.website.sdk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 发送url数据的监控者,用于启动一个单独的线程来发送数据
 */
public class SendDataMonitor {

  private static final Logger log = Logger.getGlobal();
  //队列，用于存储发送url
  //LinkedBlockingQueue是一个由链表实现的有界队列阻塞队列，但大小默认值为Integer.MAX_VALUE，所以我们在使用LinkedBlockingQueue时建议手动传值，为其提供我们所需的大小，避免队列过大造成机器负载或者内存爆满等情况
  private BlockingQueue<String> queue = new LinkedBlockingQueue<String>();


  private static SendDataMonitor sendDataMonitor = null;

  private SendDataMonitor() {
    //
  }

  public static SendDataMonitor getSendDataMonitor() {
    if (sendDataMonitor == null) {
      synchronized (SendDataMonitor.class) {
        if (sendDataMonitor == null) {
          sendDataMonitor = new SendDataMonitor();
          Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
              SendDataMonitor.sendDataMonitor.run();
            }
          });
          //测试的时候不设置成守护模式
//          thread.setDaemon(true);
          thread.start();
        }
      }
    }
    return sendDataMonitor;
  }

  /**
   * 添加url到队列
   * @param url
   * @throws InterruptedException
   */
  public static void addSendUrl(String url) throws InterruptedException {
    getSendDataMonitor().queue.put(url);
  }

  /**
   * 具体执行发送的方法
   */
  private void run(){
    while (true){
      try {
        String url = this.queue.take();
        HttpRequestUtil.sendData(url);
      } catch (Throwable e) {
        log.log(Level.WARNING, "发送url异常", e);
      }
    }
  }

  /**
   * 内部类，用于发送数据的http工具类
   */
  public static class HttpRequestUtil{
    public static void sendData(String url) throws IOException {
      HttpURLConnection connection = null;
      BufferedReader in = null;
      try {
        URL obj = new URL(url);
         connection = (HttpURLConnection)obj.openConnection();
        //设置连接参数
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod("GET");
        System.out.println("发送url: "  + url);

        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

      }finally {
        try{
          if (in != null){
            in.close();
          }
        }catch (Throwable throwable){
          //nothing
        }
      }
    }
  }


}
