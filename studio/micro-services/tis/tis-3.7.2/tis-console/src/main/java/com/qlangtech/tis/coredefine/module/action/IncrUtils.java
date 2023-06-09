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
package com.qlangtech.tis.coredefine.module.action;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.qlangtech.tis.config.k8s.HorizontalpodAutoscaler;
import com.qlangtech.tis.config.k8s.ReplicasSpec;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.runtime.module.misc.IMessageHandler;
import com.qlangtech.tis.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 增量自动部署相关
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2018年10月31日
 */
public class IncrUtils {

  private IncrUtils() {
  }

  public static final Pattern PATTERN_NUMBER = Pattern.compile("[1-9]\\d*");

  public static boolean isNumber(String val) {
    Matcher m = PATTERN_NUMBER.matcher(val);
    return m.matches();
  }

  /**
   * @param context
   * @param form
   * @param msg
   * @return
   */
  public static IncrSpecResult parseIncrSpec(Context context, final JSONObject form, IControlMsgHandler msg) {
    ReplicasSpec spec = new ReplicasSpec();
    IncrSpecResult result = new IncrSpecResult(spec, context, msg);
    result.success = false;
    String fieldPods = "pods";
    int replicCount = form.getIntValue(fieldPods);
    if (replicCount < 1) {
      // msg.addErrorMessage(context, "请设置Pods，不能小于1");
      msg.addFieldError(context, fieldPods, "请设置Pods，不能小于1");
      // return result;
    } else if (replicCount > 20) {
      msg.addFieldError(context, fieldPods, "不能大于20");
      // return result;
    }
    spec.setReplicaCount(replicCount);
    Specification s = null;
    String fieldCuprequest = "cuprequest";
    String cpurequest = StringUtils.trimToEmpty(form.getString(fieldCuprequest));
    String cpuRequestUnit = form.getString("cuprequestunit");
    if (StringUtils.isEmpty(cpuRequestUnit)) {
      msg.addFieldError(context, fieldCuprequest, "请填写CPU请求单位");
    }
    cpuRequestUnit = "cores".equals(cpuRequestUnit) ? StringUtils.EMPTY : cpuRequestUnit;
    if (StringUtils.isEmpty(cpurequest)) {
      msg.addFieldError(context, fieldCuprequest, "请填写");
    } else if (!IncrUtils.isNumber(cpurequest)) {
      //msg.addErrorMessage(context, "cpurequest must be " + IncrUtils.PATTERN_NUMBER);
      msg.addFieldError(context, fieldCuprequest, "必须是非负整数");

    } else {
      s = new Specification();
      s.setVal(Integer.parseInt(cpurequest));
      s.setUnit(cpuRequestUnit);
      int maxCpuCoresRequest = 4;
      if (s.normalizeCPU() > (maxCpuCoresRequest * 1024)) {
        msg.addFieldError(context, fieldCuprequest, "请检查CPU请求资源，不能大于" + maxCpuCoresRequest + "cores");
        //  return result;
      }
      spec.setCpuRequest(s);
    }


    String filedCpuLimit = "cuplimit";
    String cupLimit = StringUtils.trimToEmpty(form.getString(filedCpuLimit));
    String cupLimitUnit = form.getString("cuplimitunit");
    if (StringUtils.isEmpty(cupLimitUnit)) {
      msg.addFieldError(context, filedCpuLimit, "请填写CPU最大请求单位");
    }
    cupLimitUnit = "cores".equals(cupLimitUnit) ? StringUtils.EMPTY : cupLimitUnit;
    if (StringUtils.isEmpty(cupLimit)) {
      msg.addFieldError(context, filedCpuLimit, "请填写");
    } else if (!IncrUtils.isNumber(cupLimit)) {
      // msg.addErrorMessage(context, "CPU limit must be " + IncrUtils.PATTERN_NUMBER);
      msg.addFieldError(context, filedCpuLimit, "必须是非负整数");
      // return result;
    } else {
      s = new Specification();
      s.setVal(Integer.parseInt(cupLimit));
      s.setUnit(cupLimitUnit);
      int maxCpuCoresLimit = 8;
      if (s.normalizeCPU() > maxCpuCoresLimit * 1024) {
        msg.addFieldError(context, filedCpuLimit, "请检查CPU最大申请资源，不能大于" + maxCpuCoresLimit + "cores");
        // return result;
      }
      spec.setCpuLimit(s);
    }


    if (spec.getCpuRequest() != null && spec.getCpuLimit() != null && spec.getCpuRequest().cpuBigThan(spec.getCpuLimit())) {
      msg.addFieldError(context, fieldCuprequest, "请检查CPU`申请资源`，不能大于`最大申请资源`");
      msg.addFieldError(context, filedCpuLimit, "请检查CPU`最大申请资源`，不能小于`申请资源`");
    }


    // cupLimit = cupLimit + cupLimitUnit;
    String fieldMemoryRequest = "memoryrequest";
    String memoryRequest = form.getString(fieldMemoryRequest);
    String memoryRequestUnit = form.getString("memoryrequestunit");
    if (StringUtils.isEmpty(memoryRequest)) {
      msg.addFieldError(context, fieldMemoryRequest, "请填写");
    } else if (!IncrUtils.isNumber(memoryRequest)) {
      // msg.addErrorMessage(context, "内存格式" + IncrUtils.PATTERN_NUMBER);
      msg.addFieldError(context, fieldMemoryRequest, "必须是非负整数");
      // return result;
    } else {
      s = new Specification();
      s.setVal(Integer.parseInt(memoryRequest));
      s.setUnit(memoryRequestUnit);
      int maxMemoryRequest = 4;
      if (s.normalizeMemory() > (maxMemoryRequest * 1024)) {
        msg.addFieldError(context, fieldMemoryRequest, "请检查内存申请资源，不能大于" + maxMemoryRequest + "G");
        // return result;
      }
      spec.setMemoryRequest(s);
    }
    if (StringUtils.isEmpty(memoryRequestUnit)) {
      // msg.addErrorMessage(context, "请填写内存请求单位");
      msg.addFieldError(context, fieldMemoryRequest, "请填写内存请求单位");
      //return result;
    }
    String fieldMemorylimit = "memorylimit";
    String memoryLimit = (form.getString(fieldMemorylimit));
    String memoryLimitUnit = form.getString("memorylimitunit");
    if (StringUtils.isEmpty(memoryLimit)) {
      msg.addFieldError(context, fieldMemorylimit, "请填写");
    } else if (!IncrUtils.isNumber(memoryLimit)) {
      //msg.addErrorMessage(context, "内存上限" + IncrUtils.PATTERN_NUMBER);
      msg.addFieldError(context, fieldMemorylimit, "必须为非负整数");
      //return result;
    } else {
      int maxMemorylimit = 8;
      s = new Specification();
      s.setVal(Integer.parseInt(memoryLimit));
      s.setUnit(memoryLimitUnit);
      if (s.normalizeMemory() > (maxMemorylimit * 1024)) {
        msg.addFieldError(context, fieldMemorylimit, "请检查内存最大申请资源，不能大于" + maxMemorylimit + "G");
        //return result;
      }
      spec.setMemoryLimit(s);
    }
    if (StringUtils.isEmpty(memoryLimitUnit)) {
      msg.addFieldError(context, fieldMemorylimit, "请填写内存上限单位");
      //return result;
    }


    if (spec.getMemoryRequest() != null && spec.getMemoryLimit() != null && spec.getMemoryRequest().memoryBigThan(spec.getMemoryLimit())) {
      msg.addFieldError(context, fieldMemoryRequest, "请检查内存`申请资源`，不能大于`最大申请资源`");
      msg.addFieldError(context, fieldMemorylimit, "请检查内存`最大申请资源`，不能小于`申请资源`");
      // return result;
    }

    final String KEY_MIN_HPA_POD = "minHpaPod";
    final String KEY_MAX_HPA_POD = "maxHpaPod";
    final String KEY_CPU_AVERAGE_UTILIZATION = "cpuAverageUtilization";

    boolean supportHpa = form.getBooleanValue("supportHpa");
    if (supportHpa) {
      // 支持弹性扩容
      Integer maxHpaPod = form.getInteger(KEY_MAX_HPA_POD);
      Integer minHpaPod = form.getInteger(KEY_MIN_HPA_POD);
      if (minHpaPod == null) {
        msg.addFieldError(context, KEY_MIN_HPA_POD, "请填写'最小Pods'");
      }
      if (maxHpaPod == null) {
        msg.addFieldError(context, KEY_MAX_HPA_POD, "请填写'最大Pods'");
      }
      Integer cpuAverageUtilization = form.getInteger(KEY_CPU_AVERAGE_UTILIZATION);
      if (cpuAverageUtilization == null) {
        msg.addFieldError(context, KEY_CPU_AVERAGE_UTILIZATION, "请填写'CPU平均利用率'");
      }

      if (context.hasErrors()) {
        result.success = false;
        return result;
      }
      boolean hasErr = false;
      if (cpuAverageUtilization < 1 || cpuAverageUtilization >= 100) {
        msg.addFieldError(context, KEY_CPU_AVERAGE_UTILIZATION, "必须大于0且小于100");
        hasErr = true;
      }

      if (minHpaPod < 1) {
        msg.addFieldError(context, KEY_MIN_HPA_POD, "最小弹性Pod数必须大于0");
        hasErr = true;
      }
      if (maxHpaPod <= minHpaPod) {
        msg.addFieldError(context, KEY_MAX_HPA_POD, "必须大于最小弹性Pod数");
        msg.addFieldError(context, KEY_MIN_HPA_POD, "必须小于最大弹性Pod数");
        hasErr = true;
      }
      if (hasErr) {
        return result;
      }
      HorizontalpodAutoscaler hpa = new HorizontalpodAutoscaler();
      hpa.setMaxPod(maxHpaPod);
      hpa.setMinPod(minHpaPod);
      hpa.setCpuAverageUtilization(cpuAverageUtilization);


      if (hpa.getMinPod() > spec.getReplicaCount()) {
        // msg.addFieldError(context, KEY_MAX_HPA_POD, "必须大于最小弹性Pod数");
        // msg.addFieldError(context, KEY_MIN_HPA_POD, "必须小于最大弹性Pod数");
        msg.addFieldError(context, KEY_MIN_HPA_POD, "不能大于'Pods'数");
        msg.addFieldError(context, fieldPods, "不能小于'弹性扩容最小Pods数'");
        // msgHandler.(context, resourceLiteria + " Request Is inValid " + request);
        return result;
      }


      result.hpa = hpa;
    }

    result.success = !context.hasErrors();
    return result;
  }

  public static class IncrSpecResult {
    public HorizontalpodAutoscaler hpa;
    private final ReplicasSpec spec;

    private boolean success;

    private final Context context;

    private final IMessageHandler msgHandler;

    public ReplicasSpec getSpec() {
      return this.spec;
    }

    public boolean isSuccess() {
      if (!success) {
        return false;
      }
      if (!validRequestAndLimit(spec.getMemoryRequest(), spec.getMemoryLimit(), r -> r.normalizeMemory(), "Memory")) {
        return false;
      }
      if (!validRequestAndLimit(spec.getCpuRequest(), spec.getCpuLimit(), r -> r.normalizeCPU(), "CPU")) {
        return false;
      }

      return true;
    }

    private boolean validRequestAndLimit(Specification request, Specification limit, IMetricsGetter getter, String resourceLiteria) {
      int r = getter.get(request);
      if (r < 1) {
        msgHandler.addErrorMessage(context, resourceLiteria + " Request Is inValid " + request);
        return false;
      }
      int l = getter.get(limit);
      if (l < 1) {
        msgHandler.addErrorMessage(context, resourceLiteria + " Limit Is InValid " + limit);
        return false;
      }
      if (r > l) {
        msgHandler.addErrorMessage(context, resourceLiteria + " Request(" + request + ") can not bigger than Limit(" + limit + ") ");
        return false;
      }
      return true;
    }

    IncrSpecResult(ReplicasSpec spec, Context context, IMessageHandler msgHandler) {
      this.spec = spec;
      this.context = context;
      this.msgHandler = msgHandler;
    }
  }

  private interface IMetricsGetter {

    public int get(Specification s);
  }

  public static void main(String[] args) throws Exception {
    Specification s = Specification.parse("300");
    System.out.println(s.getUnit() + "," + s.getVal());
    // System.out.println(isNumber("123000"));
    //
    // System.out.println(isNumber("0123000"));
    // BuildStatus status = readLastBuildRecordStatus("search4totalpay");
    // System.out.println(status.getBuildName());
    // rollbackIncrDeploy("search4totalpay");
    ReplicasSpec spec = new ReplicasSpec();
    // spec.setGitAddress("git@git.2dfire-inc.com.qlangtech.searcher/tis-mars.git");
    // spec.setGitRef("develop");
    //
    // Specification s = new Specification();
    // s.setVal(100);
    // s.setUnit("c");
    // spec.setCpuLimit(s);
    //
    // s = new Specification();
    // s.setVal(200);
    // s.setUnit("cores");
    // spec.setCpuRequest(s);
    //
    // s = new Specification();
    // s.setVal(2);
    // s.setUnit("G");
    // spec.setMemoryLimit(s);
    //
    // s = new Specification();
    // s.setVal(200);
    // s.setUnit("M");
    // spec.setMemoryRequest(s);
    //
    // saveIncrSpec("search4totalpay", spec);
    spec = readIncrSpec("search4totalpay");
    // System.out.println(spec.getGitAddress());
  }

  /**
   * 保存增量通道配置规格
   *
   * @param spec
   */
  public static void saveIncrSpec(String indexName, ReplicasSpec spec) throws IOException {
    File f = getIncrSpecFile(indexName);
    FileUtils.write(f, JSON.toJSONString(spec, true), Charset.forName("utf8"), false);
  }

  /**
   * 读取索引规格
   *
   * @param indexName
   * @return
   * @throws IOException
   */
  public static ReplicasSpec readIncrSpec(String indexName) {
    File f = getIncrSpecFile(indexName);
    if (!f.exists()) {
      return null;
    }
    try {
      return JSON.parseObject(FileUtils.readFileToString(f, Charset.forName("utf8")), ReplicasSpec.class);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * 添加一条部署寫入
   *
   * @param indexName
   * @param status
   * @param version
   */
  public static DeploymentStatus appendDeploymentRecord(String indexName, Status status, int version, int replica, boolean shallSeralize, int preLatestVersionNumber) {
    synchronized (IncrUtils.class) {
      DeploymentStatus s = new DeploymentStatus(indexName);
      s.setStatus(status);
      s.setVersion(version);
      s.setPreVersion(preLatestVersionNumber);
      s.setReplica(replica);
      if (shallSeralize) {
        File f = getDeploymentHistoryFile(indexName);
        String line = version + "," + preLatestVersionNumber + "," + status.val + "," + replica + "\n";
        appendLine(f, line);
      }
      return s;
    }
  }

  public static void appendIndeterminacyDeploymentRecord(String indexName, int preLatestVersionNumber) {
    synchronized (IncrUtils.class) {
      appendDeploymentRecord(indexName, Status.CREATED, -1, -1, true, preLatestVersionNumber);
    }
  }

  /**
   * 读最后一条部署记录
   *
   * @param indexName
   * @return
   */
  public static DeploymentStatus readLastDeploymentRecord(String indexName) {
    synchronized (IncrUtils.class) {
      DeploymentStatus s = new DeploymentStatus(indexName);
      s.setStatus(Status.NONE);
      File f = getDeploymentHistoryFile(indexName);
      // File monitorFile, int n, IProcessLine lineProcess
      Utils.readLastNLine(f, 1, new Utils.IProcessLine() {

        @Override
        public void print(String line) {
          String[] ary = StringUtils.split(line, ",");
          s.setVersion(Integer.parseInt(ary[0]));
          s.setPreVersion(Integer.parseInt(ary[1]));
          s.setStatus(Status.parse(ary[2]));
          s.setReplica(Integer.parseInt(ary[3]));
        }
      });
      return s;
    }
  }

  /**
   * 取消增量构建，需要建已經写入的deploy记录回滚
   */
  public static void rollbackIncrDeploy(String indexName) {
    synchronized (IncrUtils.class) {
      final List<DeploymentStatus> history = Lists.newArrayList();
      File f = getDeploymentHistoryFile(indexName);
      // File monitorFile, int n, IProcessLine lineProcess
      Utils.readLastNLine(f, 2, new Utils.IProcessLine() {

        @Override
        public void print(String line) {
          DeploymentStatus s = new DeploymentStatus(indexName);
          String[] ary = StringUtils.split(line, ",");
          s.setVersion(Integer.parseInt(ary[0]));
          s.setPreVersion(Integer.parseInt(ary[1]));
          s.setStatus(Status.parse(ary[2]));
          s.setReplica(Integer.parseInt(ary[3]));
          history.add(s);
        }
      });
      if (history.size() == 1) {
        appendDeploymentRecord(indexName, Status.NONE, -1, -1, true, -1);
      } else if (history.size() == 2) {
        DeploymentStatus s = history.get(0);
        appendDeploymentRecord(indexName, s.getStatus(), s.getVersion(), s.getReplica(), true, s.getPreVersion());
      }
    }
  }

  private static void appendLine(File f, String line) {
    try {
      try (FileOutputStream out = FileUtils.openOutputStream(f, true)) {
        out.write((line).getBytes());
        out.flush();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static File getIncrBuildFile(String indexName) {
    String dataDir = System.getProperty("data.dir", ".");
    if (StringUtils.isEmpty(dataDir)) {
      throw new IllegalArgumentException("system dir 'data.dir' can not be empty");
    }
    File f = new File(dataDir, "/" + indexName + "/incr-build/history.log");
    return f;
  }

  private static File getDeploymentHistoryFile(String indexName) {
    String dataDir = System.getProperty("data.dir", ".");
    if (StringUtils.isEmpty(dataDir)) {
      throw new IllegalArgumentException("system dir 'data.dir' can not be empty");
    }
    return new File(dataDir, "/" + indexName + "/incr-deployment/history.log");
  }

  private static File getIncrSpecFile(String indexName) {
    String dataDir = System.getProperty("data.dir", ".");
    if (StringUtils.isEmpty(dataDir)) {
      throw new IllegalArgumentException("system dir 'data.dir' can not be empty");
    }
    if (StringUtils.isEmpty(indexName)) {
      throw new IllegalArgumentException("indexName can not be empty");
    }
    return new File(dataDir, "/incr-spec/" + indexName + ".text");
  }

  public static class DeploymentStatus {

    private final String collection;

    private final String type = "deploy";

    private int version;

    private int preVersion;

    private Status status;

    private int replica;

    public String getType() {
      return type;
    }

    public String getReplicationControllerName() {
      return this.collection + "-" + this.version;
    }

    public int getPreVersion() {
      return preVersion;
    }

    public void setPreVersion(int preVersion) {
      this.preVersion = preVersion;
    }

    public int getReplica() {
      return replica;
    }

    public void setReplica(int replica) {
      this.replica = replica;
    }

    /**
     * 是否是不确定的，因為重新build之後會自動觸發一次deploy，先记录在日志中了
     *
     * @return
     */
    public boolean isIndeterminacy() {
      return this.version < 0;
    }

    public DeploymentStatus(String deploymentName) {
      super();
      this.collection = deploymentName;
    }

    public String getCollection() {
      return collection;
    }

    public int getVersion() {
      return version;
    }

    public void setVersion(int version) {
      this.version = version;
    }

    public String getStatusLiterial() {
      switch (this.status) {
        case CREATED:
          return "部署中";
        case SUCCESS:
          return "运行中";
        case FAILD:
          return "部署失败";
        default:
          return "尚未部署";
      }
    }

    public Status getStatus() {
      return status;
    }

    public void setStatus(Status status) {
      this.status = status;
    }
  }

  public static enum Status {

    CREATED("created"), SUCCESS("success"), FAILD("faild"), NONE("none"), CANCELLED("cancelled");

    private String val;

    private Status(String val) {
      this.val = val;
    }

    public String getVal() {
      return this.val;
    }

    public static Status parse(String val) {
      if (CREATED.val.equals(val)) {
        return CREATED;
      } else if (SUCCESS.val.equals(val)) {
        return SUCCESS;
      } else if (FAILD.val.equals(val)) {
        return FAILD;
      } else if (NONE.val.equals(val)) {
        return NONE;
      } else if (CANCELLED.val.equals(val)) {
        return CANCELLED;
      }
      throw new IllegalStateException("illegal val:" + val);
    }
  }
}
