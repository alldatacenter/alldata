/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.manage.servlet;

import ch.qos.logback.core.helpers.CyclicBuffer;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import com.qlangtech.tis.assemble.ExecResult;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.coredefine.module.action.ExtendWorkFlowBuildHistory;
import com.qlangtech.tis.coredefine.module.action.TISK8sDelegate;
import com.qlangtech.tis.exec.ExecutePhaseRange;
import com.qlangtech.tis.fullbuild.phasestatus.PhaseStatusCollection;
import com.qlangtech.tis.job.common.JobCommon;
import com.qlangtech.tis.manage.spring.ZooKeeperGetter;
import com.qlangtech.tis.pubhook.common.RunEnvironment;
import com.qlangtech.tis.rpc.grpc.log.LogCollectorClient;
import com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState;
import com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget;
import com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection;
import com.qlangtech.tis.trigger.jst.ILogListener;
import com.qlangtech.tis.trigger.jst.MonotorTarget;
import com.qlangtech.tis.trigger.jst.PayloadMonitorTarget;
import com.qlangtech.tis.trigger.jst.RegisterMonotorTarget;
import com.qlangtech.tis.trigger.socket.ExecuteState;
import com.qlangtech.tis.trigger.socket.LogType;
import com.qlangtech.tis.workflow.dao.IWorkflowDAOFacade;
import com.tis.hadoop.rpc.RpcServiceReference;
import com.tis.hadoop.rpc.StatusRpcClient;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 实时執行日誌接收集群中其他服务节点反馈过来的日志信息<br>
 * 不需要绑定taskid
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2014-4-2
 */
public class LogFeedbackServlet extends WebSocketServlet {

  private static final Logger logger = LoggerFactory.getLogger(LogFeedbackServlet.class);

  private static final long serialVersionUID = 1L;

  private RpcServiceReference statusRpc;

  private IWorkflowDAOFacade wfDao;
  private ZooKeeperGetter zkGetter;

  private static final ExecutorService executorService = Executors.newCachedThreadPool();

//  private void closeStatusRpc() {
//    statusRpc.get().close();
//    statusRpc = null;
//  }

  private RpcServiceReference getStatusRpc() {
    if (this.statusRpc != null) {
      return this.statusRpc;
    }
    try {
      Objects.requireNonNull(zkGetter, "zkGetter can not be null");
      this.statusRpc = StatusRpcClient.getService(zkGetter.getInstance());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return this.statusRpc;
  }

  @Override
  public void configure(WebSocketServletFactory factory) {
    factory.getPolicy().setIdleTimeout(240000);
    factory.getPolicy().setAsyncWriteTimeout(-1);
    factory.setCreator((req, rep) -> {
      return new LogSocket();
    });
    this.zkGetter = BasicServlet.getBeanByType(getServletContext(), ZooKeeperGetter.class);
    this.wfDao = BasicServlet.getBeanByType(getServletContext(), IWorkflowDAOFacade.class);
  }

  public class LogSocket extends WebSocketAdapter implements ILogListener, LogCollectorClient.IPhaseStatusCollectionListener {

    // 在客户端中将服务端的流式消息缓存一些，这样用户重复打开终端显示，第二次显示不会为空内容
    private final Map<LogType, CyclicBuffer<MessageOrBuilder>> logtypes = new HashMap<>();

    private String collectionName;

    private int taskid;

    private StreamObserver<PMonotorTarget> pMonotorObserver;
    private ExtendWorkFlowBuildHistory buildTask;

    public LogSocket() {
    }

    @Override
    public void onWebSocketConnect(Session sess) {
      super.onWebSocketConnect(sess);
      this.taskid = Integer.parseInt(this.getParameter(JobCommon.KEY_TASK_ID, Collections.singletonList("-1")));
      this.collectionName = getParameter("collection", Collections.singletonList(MonotorTarget.DUMP_COLLECTION));
      List<RegisterMonotorTarget> typies = parseLogTypes(this.getParameter("logtype"));
      logger.info("taskid:{},appname:{},typies:{}", this.taskid, this.collectionName
        , typies.stream().map((t) -> String.valueOf(t)).collect(Collectors.joining(",")));
      try {
        if (this.taskid > 0 && typies.size() < 2) {
          buildTask = getBuildHistory();
          this.sendMsg2Client(buildTask);
          // if (ExecResult.parse(build.getState()) != ExecResult.DOING) {
          // // 如果任务已经完成则没有必要继续监听了
          // return;
          // }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      addMonitor(typies);
    }

    private ExtendWorkFlowBuildHistory getBuildHistory() {
      return new ExtendWorkFlowBuildHistory(wfDao.getWorkFlowBuildHistoryDAO().selectByPrimaryKey(this.taskid));
    }

    private void addMonitor(List<RegisterMonotorTarget> typies) {
      typies.forEach((t) -> {
        try {
          addMonitor(t);
        } catch (Exception e) {
          logger.error(t.toString(), e);
          throw new RuntimeException(e);
        }
      });
    }


    /**
     * 接收客户端发送的消息
     *
     * @param message
     */
    @Override
    public void onWebSocketText(String message) {
      JSONObject body = JSON.parseObject(message);
      List<RegisterMonotorTarget> logtype = parseLogTypes(body.getString("logtype"));
      addMonitor(logtype);
    }

    private StreamObserver<PMonotorTarget> getMonitorSet() {
      if (pMonotorObserver == null) {
        StatusRpcClient.AssembleSvcCompsite feedback = getStatusRpc().get();
        pMonotorObserver = feedback.registerMonitorEvent(this);
      }
      return pMonotorObserver;
    }

    @Override
    public boolean isClosed() {
      return this.isNotConnected();
    }

    // @Override
    // public Set<MonotorTarget> getMonitorTypes() {
    // return this.monitorSet;
    // }

    /**
     * impl: com.qlangtech.tis.trigger.jst.ILogListener
     *
     * @param evt
     */
    @Override
    public synchronized void read(Object evt) {
      try {
        PExecuteState event = (PExecuteState) evt;
        LogType ltype = LogCollectorClient.convert(event.getLogType());
        CyclicBuffer<MessageOrBuilder> messageBuffer = null;
        if (this.isConnected() && (messageBuffer = this.logtypes.get(ltype)) != null) {
          // JsonFormat.Printer printer = JsonFormat.printer();
          // 向客户端缓存中也写一份
          messageBuffer.add(event);
          sendMsg2Client(event);
        }
      } catch (IOException e) {
        logger.error(e.getMessage(), e);
      }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
      super.onWebSocketClose(statusCode, reason);
      getMonitorSet().onCompleted();
      // try {
      // // 服下毒丸，通讯终止
      // getMonitorSet().put(RegisterMonotorTarget.PoisonPill);
      // } catch (InterruptedException e) {
      // throw new RuntimeException(e);
      // }
      logger.warn("onWebSocketClose:" + this.collectionName + ",statusCode：" + statusCode + ",reason:" + reason);
    }

    /**
     * @param monitorTarget
     */
    private void addMonitor(MonotorTarget monitorTarget) throws Exception {
      if (RunEnvironment.getSysRuntime() != RunEnvironment.DAILY && monitorTarget.testLogType(LogType.INCR_SEND)) {
        // 线上环境不提供详细日志发送
        // return;
      }

      synchronized (this.logtypes) {
        CyclicBuffer<MessageOrBuilder> msgBuffer = null;
        if ((msgBuffer = this.logtypes.get(monitorTarget.getLogType())) != null) {
          if (!monitorTarget.testLogType(LogType.INCR_DEPLOY_STATUS_CHANGE, LogType.DATAX_WORKER_POD_LOG)) {
            // 将缓存中的数据写入到客户端
            for (MessageOrBuilder msg : msgBuffer.asList()) {
              sendMsg2Client(msg);
            }
            return;
          }
        }
        if (msgBuffer == null) {
          msgBuffer = new CyclicBuffer<>(200);
          this.logtypes.put(monitorTarget.getLogType(), msgBuffer);
        }
      }

//      if (!this.logtypes.add(monitorTarget.getLogType()) && /**
//       * POD日志监听需要可能会因为超时而重连
//       */
//        !monitorTarget.testLogType(LogType.INCR_DEPLOY_STATUS_CHANGE, LogType.DATAX_WORKER_POD_LOG)) {
//        return;
//      }
      if (monitorTarget.testLogType(LogType.DATAX_WORKER_POD_LOG)) {
        PayloadMonitorTarget mtarget = (PayloadMonitorTarget) monitorTarget;
        final String podName = mtarget.getPayLoad();
        TISK8sDelegate k8sDelegate = TISK8sDelegate.getK8SDelegate(mtarget.getCollection());
        k8sDelegate.listPodsAndWatchLog(podName, this);
        return;
      } else if (monitorTarget.testLogType(LogType.INCR_DEPLOY_STATUS_CHANGE)) {
        PayloadMonitorTarget mtarget = (PayloadMonitorTarget) monitorTarget;
        final String podName = mtarget.getPayLoad();
        TISK8sDelegate k8sDelegate = TISK8sDelegate.getK8SDelegate(monitorTarget.getCollection());
        k8sDelegate.listPodsAndWatchLog(podName, this);
        return;
      } else if (monitorTarget.testLogType(LogType.FULL, LogType.INCR, LogType.INCR_SEND)) {
        PMonotorTarget.Builder t = PMonotorTarget.newBuilder();
        t.setLogtype(LogCollectorClient.convert(monitorTarget.getLogType().typeKind));
        t.setCollection(this.collectionName);
        if (this.taskid > 0) {
          t.setTaskid(this.taskid);
        }
        this.getMonitorSet().onNext(t.build());
        return;
      } else if (monitorTarget.testLogType(LogType.BuildPhraseMetrics)) {
        executorService.execute(() -> {
          try {
            StatusRpcClient.AssembleSvcCompsite feedback = getStatusRpc().get();
            final Iterator<PPhaseStatusCollection> statIt = feedback.buildPhraseStatus(taskid);
            boolean serverSideBreak = true;
            while (isConnected() && statIt.hasNext()) {
              if (!process(statIt.next())) {
                // 任务已经结束，停止接收任务执行状态
                serverSideBreak = false;
                break;
              }
            }
            logger.info("exit buildPhraseStatus status monitor,serverSideBreak:{}", serverSideBreak);
          } catch (StatusRuntimeException e) {
            getStatusRpc().reConnect();
            throw e;
          } catch (Exception e) {
            throw new RuntimeException("taskid:" + taskid, e);
          }
        });
        //} else if (monitorTarget.testLogType(LogType.MQ_TAGS_STATUS)) {
//        PluginStore<MQListenerFactory> mqListenerFactory = TIS.getPluginStore(this.collectionName, MQListenerFactory.class);
//        MQListenerFactory plugin = mqListenerFactory.getPlugin();
//        // 增量节点处理
//        final Map<String, TopicTagStatus> /* this.tag */
//          transferTagStatus = new HashMap<>();
//        final Map<String, TopicTagStatus> /* this.tag */
//          binlogTopicTagStatus = new HashMap<>();
//        List<TopicTagIncrStatus.FocusTags> focusTags = getFocusTags(zkGetter.getInstance(), collectionName);
//        // 如果size为0，则说明远程工作节点没有正常执行
//        if (focusTags.size() > 0) {
//          TopicTagIncrStatus topicTagIncrStatus = new TopicTagIncrStatus(focusTags);
//          executorService.execute(() -> {
//            IncrTagHeatBeatMonitor incrTagHeatBeatMonitor = new IncrTagHeatBeatMonitor(this.collectionName, this
//              , transferTagStatus, binlogTopicTagStatus, topicTagIncrStatus, plugin.createConsumerStatus(), zkGetter);
//            incrTagHeatBeatMonitor.build();
//          });
//        }
      } else {
        throw new IllegalStateException("monitor type:" + monitorTarget + " is illegal");
      }
    }

    /**
     * <<<<<<<<<<<< Impl: LogCollectorClient.IPhaseStatusCollectionListener
     *
     * @return
     */
    @Override
    public boolean isReady() {
      return !this.isClosed();
    }

    private Boolean preTaskComplete;

    /**
     * @param ss
     * @return true：任务仍在执行中 false：任务已经终止，不需要再继续监听了
     * @throws Exception
     */
    @Override
    public boolean process(PPhaseStatusCollection ss) throws Exception {
      if (this.isClosed()) {
        return false;
      }

      if (this.buildTask == null) {
        throw new IllegalStateException("taskid:" + this.taskid + " relevant buildTask can not be null");
      }

      PhaseStatusCollection buildState
        = LogCollectorClient.convert(ss, new ExecutePhaseRange(
        FullbuildPhase.parse(this.buildTask.getDelegate().getStartPhase()), FullbuildPhase.parse(this.buildTask.getDelegate().getEndPhase())));
      boolean jobStop = false;
      ExtendWorkFlowBuildHistory status = null;
      if (preTaskComplete != null) {

        if ((buildState.isComplete() ^ preTaskComplete)) {
          // 状态变化了要重新向客户发一个请求
          int waitTry = 0;
          do {
            if (waitTry++ > 4) {
              // 等待尝试4次退出
              break;
            }
            // assemble节点反馈执行状态与状态写入到数据库有一个时间差，需要等一下
            Thread.sleep(2000);
            status = getBuildHistory();
            if (isTerminal(status)) {
              jobStop = true;
              break;
            }
          } while (true);
          preTaskComplete = buildState.isComplete();
        }
      } else {
        status = getBuildHistory();
        if (isTerminal(status)) {
          jobStop = true;
        }
        preTaskComplete = buildState.isComplete();
      }
      if (jobStop && status != null) {
        this.sendMsg2Client(status);
      }
      this.sendMsg2Client(buildState);
      return !jobStop;
    }

    private boolean isTerminal(ExtendWorkFlowBuildHistory status) {
      return ExecResult.parse(status.getState()) != ExecResult.DOING &&
        ExecResult.parse(status.getState()) != ExecResult.ASYN_DOING;
    }

    @Override
    public void sendMsg2Client(Object biz) throws IOException {
      sendMsg2Client(JSON.toJSONString(biz, false));
    }

    private void sendMsg2Client(MessageOrBuilder biz) throws IOException {
      sendMsg2Client(JsonFormatPrinter.print(biz));
    }

    private void sendMsg2Client(String jsonContent) throws IOException {
      synchronized (LogSocket.this) {
        if (this.isClosed()) {
          throw new IllegalStateException("ws conn has closed,jsonContent:" + jsonContent);
        }
        // webSocket 不能多线程发送消息，所以要在这里加一个锁
        // https://stackoverflow.com/questions/36305830/blocking-message-pending-10000-for-blocking-using-spring-websockets
        this.getRemote().sendString(jsonContent);
      }
    }

    /**
     * LogCollectorClient.IPhaseStatusCollectionListener>>>>>>>>>>>>>>>>>
     */
    /**
     * 需要监听的实体的格式 “full”,“incrbuild:search4totalpay-1”
     *
     * @param logstype
     * @return
     */
    private List<RegisterMonotorTarget> parseLogTypes(String logstype) {
      List<RegisterMonotorTarget> types = new ArrayList<>();
      for (String t : StringUtils.split(logstype, ",")) {
        String[] arg = null;
        if (StringUtils.indexOf(t, ":") > 0) {
          arg = StringUtils.split(t, ":");
          if (arg.length != 2) {
            throw new IllegalArgumentException("arg:" + t + " is not illegal");
          }
          PayloadMonitorTarget payloadMonitor = MonotorTarget.createPayloadMonitor(this.collectionName, arg[1], LogType.parse(arg[0]));
          types.add(payloadMonitor);
        } else {
          types.add(MonotorTarget.createRegister(this.collectionName, LogType.parse(t)));
        }
      }
      types.forEach((t) -> {
        if (this.taskid > 0) {
          t.setTaskid(this.taskid);
        }
      });
      return types;
    }

    private String getParameter(String key) {
      return this.getParameter(key, Collections.emptyList());
    }

    private String getParameter(String key, List<String> dft) {
      Map<String, List<String>> params = this.getSession().getUpgradeRequest().getParameterMap();
      for (String v : params.getOrDefault(key, dft)) {
        return v;
      }
      throw new IllegalArgumentException("key:" + key + " relevant val is not exist in request");
    }
  }

//  public static List<TopicTagIncrStatus.FocusTags> getFocusTags(ITISCoordinator zookeeper, String collectionName) throws MalformedURLException {
//    //
//    JobType.RemoteCallResult<TopicInfo> topicInfo = JobType.ACTION_getTopicTags.assembIncrControlWithResult(
//      CoreAction.getAssembleNodeAddress(zookeeper),
//      collectionName, Collections.emptyList(), TopicInfo.class);
//    if (topicInfo.biz.getTopicWithTags().size() < 1) {
//      // 返回为空的话可以证明没有正常启动
//      return Collections.emptyList();
//    }
//    TopicInfo topicTags = topicInfo.biz;
//    return topicTags.getTopicWithTags().entrySet().stream().map((entry) -> new TopicTagIncrStatus.FocusTags(entry.getKey(), entry.getValue())).collect(Collectors.toList());
//  }

  static class TagCountMap extends HashMap<String, /* tag */
    Integer> {
  }

  private static final JsonFormat.Printer JsonFormatPrinter = JsonFormat.printer();

  public static class JSONPojo<T> {

    public static String serializeJSON(ExecuteState event) {
      return JSON.toJSONString(new JSONPojo(event.getLogType(), event.getMsg()), true);
    }

    private LogType logtype;

    private T data;

    public JSONPojo(LogType logtype, T data) {
      super();
      this.logtype = logtype;
      this.data = data;
    }

    public String getLogtype() {
      return logtype.getValue();
    }

    public void setLogtype(LogType logtype) {
      this.logtype = logtype;
    }

    public T getData() {
      return data;
    }

    public void setData(T data) {
      this.data = data;
    }
  }
}
