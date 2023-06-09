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

package com.qlangtech.tis.datax.job;

import com.alibaba.citrus.turbine.Context;
import com.google.common.collect.Maps;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.config.k8s.HorizontalpodAutoscaler;
import com.qlangtech.tis.config.k8s.ReplicasSpec;
import com.qlangtech.tis.coredefine.module.action.RcHpaStatus;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.coredefine.module.action.impl.RcDeployment;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.IPluginStore;
import com.qlangtech.tis.plugin.PluginStore;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.incr.WatchPodLog;
import com.qlangtech.tis.plugin.k8s.K8sImage;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.trigger.jst.ILogListener;
import com.qlangtech.tis.util.HeteroEnum;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DataX 任务执行容器
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-04-23 17:49
 **/
@Public
public abstract class DataXJobWorker implements Describable<DataXJobWorker> {

    public static final String KEY_FIELD_NAME = "k8sImage";

    public static final String KEY_WORKER_TYPE = "workerType";

    public static final TargetResName K8S_DATAX_INSTANCE_NAME = new TargetResName("datax-worker");
    public static final TargetResName K8S_FLINK_CLUSTER_NAME = new TargetResName("flink-cluster");

    public static void validateTargetName(String targetName) {
        if (K8S_DATAX_INSTANCE_NAME.getName().equals(targetName)
                || K8S_FLINK_CLUSTER_NAME.getName().equals(targetName)) {
            return;
        }
        throw new IllegalArgumentException("targetName:" + targetName + " is illegal");
    }

    @FormField(ordinal = 1, type = FormFieldType.SELECTABLE, validate = {Validator.require})
    public String k8sImage;

//    @Override
//    public final String identityValue() {
//        return ((BasicDescriptor) this.getDescriptor()).getWorkerType().getName();
//    }


//    public static DataXJobWorker getDataxJobWorker() {
//        return getJobWorker(K8S_DATAX_INSTANCE_NAME);
//    }

    public static DataXJobWorker getFlinkClusterWorker() {
        return getJobWorker(K8S_FLINK_CLUSTER_NAME);
    }

    public static DataXJobWorker getJobWorker(TargetResName resName) {
        IPluginStore<DataXJobWorker> dataxJobWorkerStore = getJobWorkerStore(resName);
//        Optional<DataXJobWorker> firstWorker
//                = dataxJobWorkerStore.getPlugins().stream().filter((p) -> isJobWorkerMatch(resName, p.getDescriptor())).findFirst();
//        if (firstWorker.isPresent()) {
//            return firstWorker.get();
//        }
//        return null;
        return dataxJobWorkerStore.getPlugin();
    }

    public static IPluginStore<DataXJobWorker> getJobWorkerStore(TargetResName resName) {
        return TIS.getPluginStore("jobworker", resName.getName(), DataXJobWorker.class);
    }

    public static void setJobWorker(TargetResName resName, DataXJobWorker worker) {
        IPluginStore<DataXJobWorker> store = getJobWorkerStore(resName);
        store.setPlugins(null, Optional.empty(), Collections.singletonList(PluginStore.getDescribablesWithMeta(store, worker)));
    }

    public static List<Descriptor<DataXJobWorker>> getDesc(TargetResName resName) {
        return HeteroEnum.DATAX_WORKER.descriptors().stream()
                .map((d) -> (Descriptor<DataXJobWorker>) d)
                .filter((desc) -> {
                    return isJobWorkerMatch(resName, desc);
                })
                .collect(Collectors.toList());
    }

    private static boolean isJobWorkerMatch(TargetResName targetName, Descriptor<DataXJobWorker> desc) {
        return targetName.getName().equals(desc.getExtractProps().get(DataXJobWorker.KEY_WORKER_TYPE));
    }

    /**
     * 服务是否已经启动
     *
     * @return
     */
    public boolean inService() {
        File launchToken = this.getServerLaunchTokenFile();
        return launchToken.exists();
    }

    protected void writeLaunchToken() throws IOException {
        File launchToken = this.getServerLaunchTokenFile();
        SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        FileUtils.write(launchToken, timeFormat.format(new Date()), TisUTF8.get());
    }

    protected void deleteLaunchToken() {
        File launchToken = this.getServerLaunchTokenFile();
        FileUtils.deleteQuietly(launchToken);
    }

    protected File getServerLaunchTokenFile() {
        TargetResName workerType = ((BasicDescriptor) this.getDescriptor()).getWorkerType();
        IPluginStore<DataXJobWorker> workerStore = getJobWorkerStore(workerType);
        File target = workerStore.getTargetFile().getFile();
        return new File(target.getParentFile(), (workerType.getName() + ".launch_token"));
    }

    /**
     * 重启全部Pod节点
     *
     * @param
     */
    public abstract void relaunch();

    public abstract void relaunch(String podName);

    /**
     * 获取已经启动的RC运行参数
     *
     * @param
     * @return
     */
    public abstract RcDeployment getRCDeployment();

    public abstract RcHpaStatus getHpaStatus();

    /**
     * 开始增量监听
     *
     * @param listener
     */
    public abstract WatchPodLog listPodAndWatchLog(String podName, ILogListener listener);

//    /**
//     * dataXWorker service 是否是启动状态
//     *
//     * @return
//     */
//    public static boolean isDataXWorkerServiceOnDuty() {
//        PluginStore<DataXJobWorker> jobWorkerStore = TIS.getPluginStore(DataXJobWorker.class);
//        List<DataXJobWorker> services = jobWorkerStore.getPlugins();
//        return services.size() > 0 && jobWorkerStore.getPlugin().inService();
//    }

    /**
     * 通过Curator来实现分布式任务overseer-worker模式
     *
     * @return
     */
    public abstract String getZookeeperAddress();

    public abstract String getZkQueuePath();

    protected final K8sImage getK8SImage() {
        K8sImage k8sImage = TIS.getPluginStore(K8sImage.class).find(this.k8sImage);
        Objects.requireNonNull(k8sImage, "k8sImage:" + this.k8sImage + " can not be null");
        return k8sImage;
    }

    private ReplicasSpec replicasSpec;

    // 是否支持弹性伸缩容量
    private HorizontalpodAutoscaler hpa;

    public HorizontalpodAutoscaler getHpa() {
        return hpa;
    }

    public void setHpa(HorizontalpodAutoscaler hpa) {
        this.hpa = hpa;
    }

    protected boolean supportHPA() {
        return hpa != null;
    }

    public ReplicasSpec getReplicasSpec() {
        return replicasSpec;
    }

    public void setReplicasSpec(ReplicasSpec replicasSpec) {
        this.replicasSpec = replicasSpec;
    }


    /**
     * 将控制器删除掉
     */
    public abstract void remove();

    /**
     * 启动服务
     */
    public abstract void launchService();


    @Override
    public Descriptor<DataXJobWorker> getDescriptor() {
        return TIS.get().getDescriptor(this.getClass());
    }


    protected static abstract class BasicDescriptor extends Descriptor<DataXJobWorker> {

        public BasicDescriptor() {
            super();
            this.registerSelectOptions(KEY_FIELD_NAME, () -> {
                IPluginStore<K8sImage> images = TIS.getPluginStore(K8sImage.class);
                return images.getPlugins();
            });
        }

        @Override
        public Map<String, Object> getExtractProps() {
            Map<String, Object> extractProps = super.getExtractProps();
            extractProps.put(KEY_WORKER_TYPE, getWorkerType().getName());
            return extractProps;
        }

        protected abstract TargetResName getWorkerType();

        @Override
        protected boolean verify(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {

            return true;
        }
    }


}
