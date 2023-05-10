// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package k8sflink

import (
	"encoding/base64"
	"encoding/json"

	flinkoperatorv1beta1 "github.com/googlecloudplatform/flink-operator/api/v1beta1"
	"github.com/sirupsen/logrus"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/actionexecutor/logic"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/containers"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/constraintbuilders"
)

const (
	FlinkIngressPrefix = "flinkcluster"
)

var (
	defaultLogConfig = map[string]string{
		"log4j-console.properties": `rootLogger.level = INFO
      rootLogger.appenderRef.file.ref = LogFile
      rootLogger.appenderRef.console.ref = LogConsole
      appender.file.name = LogFile
      appender.file.type = File
      appender.file.append = false
      appender.file.fileName = ${sys:log.file}
      appender.file.layout.type = PatternLayout
      appender.file.layout.pattern = %d{yyyy-MM-dd HH:mm:ss,SSS} %-5p %-60c %x - %m%n
      appender.console.name = LogConsole
      appender.console.type = CONSOLE
      appender.console.layout.type = PatternLayout
      appender.console.layout.pattern = %d{yyyy-MM-dd HH:mm:ss,SSS} %-5p %-60c %x - %m%n
      logger.akka.name = akka
      logger.akka.level = INFO
      logger.kafka.name= org.apache.kafka
      logger.kafka.level = INFO
      logger.hadoop.name = org.apache.hadoop
      logger.hadoop.level = INFO
      logger.zookeeper.name = org.apache.zookeeper
      logger.zookeeper.level = INFO
      logger.netty.name = org.apache.flink.shaded.akka.org.jboss.netty.channel.DefaultChannelPipeline
      logger.netty.level = OFF`,
		"logback-console.xml": `<configuration>
        <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
          <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{60} %X{sourceThread} - %msg%n</pattern>
          </encoder>
        </appender>
        <appender name="file" class="ch.qos.logback.core.FileAppender">
          <file>${log.file}</file>
          <append>false</append>
          <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{60} %X{sourceThread} - %msg%n</pattern>
          </encoder>
        </appender>
        <root level="INFO">
          <appender-ref ref="console"/>
          <appender-ref ref="file"/>
        </root>
        <logger name="akka" level="INFO" />
        <logger name="org.apache.kafka" level="INFO" />
        <logger name="org.apache.hadoop" level="INFO" />
        <logger name="org.apache.zookeeper" level="INFO" />
        <logger name="org.apache.flink.shaded.akka.org.jboss.netty.channel.DefaultChannelPipeline" level="INFO" />
      </configuration>`,
	}
)

func getInt32Points(numeric int32) *int32 {
	return &numeric
}
func getStringPoints(s string) *string {
	return &s
}

func composeResources(res apistructs.BigdataResource) corev1.ResourceRequirements {
	return corev1.ResourceRequirements{
		Limits: corev1.ResourceList{
			corev1.ResourceCPU:    resource.MustParse(res.CPU),
			corev1.ResourceMemory: resource.MustParse(res.Memory),
		},
		Requests: corev1.ResourceList{
			corev1.ResourceCPU:    resource.MustParse(res.CPU),
			corev1.ResourceMemory: resource.MustParse(res.Memory),
		},
	}
}

func composeEnvs(envs map[string]string) []corev1.EnvVar {
	envVars := []corev1.EnvVar{}

	for k, v := range envs {
		envVars = append(envVars, corev1.EnvVar{
			Name:      k,
			Value:     v,
			ValueFrom: nil,
		})
	}

	return envVars
}

func (k *K8sFlink) ComposeFlinkCluster(job apistructs.JobFromUser, data apistructs.BigdataConf, hostURL string) *flinkoperatorv1beta1.FlinkCluster {

	scheduleInfo2, _, _ := logic.GetScheduleInfo(k.cluster, string(k.Name()), string(Kind), job)
	affinity := &constraintbuilders.K8S(&scheduleInfo2, nil, nil, nil).Affinity
	flinkCluster := flinkoperatorv1beta1.FlinkCluster{
		TypeMeta: metav1.TypeMeta{
			Kind:       "FlinkCluster",
			APIVersion: "flinkoperator.k8s.io/v1beta1",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:        data.Name,
			Namespace:   data.Namespace,
			Labels:      nil,
			Annotations: nil,
		},
		Spec: flinkoperatorv1beta1.FlinkClusterSpec{
			Image: flinkoperatorv1beta1.ImageSpec{
				Name:       data.Spec.Image,
				PullPolicy: logic.GetPullImagePolicy(),
				PullSecrets: []corev1.LocalObjectReference{
					{
						Name: apistructs.AliyunRegistry,
					},
				},
			},
			JobManager: flinkoperatorv1beta1.JobManagerSpec{
				Ingress: &flinkoperatorv1beta1.JobManagerIngressSpec{
					HostFormat: getStringPoints(hostURL),
				},
				Replicas:  getInt32Points(data.Spec.FlinkConf.JobManagerResource.Replica),
				Resources: composeResources(data.Spec.FlinkConf.JobManagerResource),
				PodLabels: map[string]string{
					apistructs.TerminusDefineTag: containers.MakeFlinkJobManagerID(data.Name),
				},
				Volumes:        nil,
				VolumeMounts:   nil,
				InitContainers: nil,
				NodeSelector:   nil,
				Affinity:       affinity,
				Tolerations:    nil,
				Sidecars:       nil,
				PodAnnotations: map[string]string{
					apistructs.MSPTerminusDefineTag:  containers.MakeFlinkJobManagerID(data.Name),
					apistructs.MSPTerminusOrgIDTag:   job.GetOrgID(),
					apistructs.MSPTerminusOrgNameTag: job.GetOrgName(),
				},
			},
			TaskManager: flinkoperatorv1beta1.TaskManagerSpec{
				Replicas:  data.Spec.FlinkConf.TaskManagerResource.Replica,
				Resources: composeResources(data.Spec.FlinkConf.TaskManagerResource),
				PodLabels: map[string]string{
					apistructs.TerminusDefineTag: containers.MakeFlinkTaskManagerID(data.Name),
				},
				Volumes:        nil,
				VolumeMounts:   nil,
				InitContainers: nil,
				NodeSelector:   nil,
				Affinity:       affinity,
				Tolerations:    nil,
				Sidecars:       nil,
				PodAnnotations: map[string]string{
					apistructs.MSPTerminusDefineTag:  containers.MakeFlinkTaskManagerID(data.Name),
					apistructs.MSPTerminusOrgIDTag:   job.GetOrgID(),
					apistructs.MSPTerminusOrgNameTag: job.GetOrgName(),
				},
			},
			EnvVars:         data.Spec.Envs,
			FlinkProperties: data.Spec.Properties,
			LogConfig:       composeLogConfig(data.Spec.FlinkConf.LogConfig),
		},
	}

	if data.Spec.FlinkConf.Kind == apistructs.FlinkJob {
		flinkCluster.Spec.Job = k.composeFlinkJob(job, data)
	}

	return &flinkCluster
}

func (k *K8sFlink) composeFlinkJob(job apistructs.JobFromUser, data apistructs.BigdataConf) *flinkoperatorv1beta1.JobSpec {
	scheduleInfo2, _, _ := logic.GetScheduleInfo(k.cluster, string(k.Name()), string(Kind), job)
	return &flinkoperatorv1beta1.JobSpec{
		JarFile:           data.Spec.Resource,
		ClassName:         &data.Spec.Class,
		Args:              data.Spec.Args,
		Parallelism:       getInt32Points(data.Spec.FlinkConf.Parallelism),
		NoLoggingToStdout: nil,
		Volumes:           nil,
		VolumeMounts:      nil,
		InitContainers:    nil,
		RestartPolicy:     getJobRestartPolicy(flinkoperatorv1beta1.JobRestartPolicyFromSavepointOnFailure),
		CleanupPolicy: &flinkoperatorv1beta1.CleanupPolicy{
			AfterJobSucceeds:  flinkoperatorv1beta1.CleanupActionDeleteCluster,
			AfterJobFails:     flinkoperatorv1beta1.CleanupActionKeepCluster,
			AfterJobCancelled: flinkoperatorv1beta1.CleanupActionDeleteTaskManager,
		},
		Affinity:        &constraintbuilders.K8S(&scheduleInfo2, nil, nil, nil).Affinity,
		CancelRequested: nil,
		PodAnnotations: map[string]string{
			apistructs.MSPTerminusDefineTag: containers.MakeFlinkJobID(data.Name),
		},
		Resources: corev1.ResourceRequirements{},
		PodLabels: map[string]string{
			apistructs.TerminusDefineTag: containers.MakeFlinkJobID(data.Name),
		},
	}
}

func getJobRestartPolicy(restartPolicy flinkoperatorv1beta1.JobRestartPolicy) *flinkoperatorv1beta1.JobRestartPolicy {
	return &restartPolicy
}

func composeStatusDesc(status flinkoperatorv1beta1.FlinkClusterStatus) apistructs.StatusDesc {
	statusDesc := apistructs.StatusDesc{}

	// If query status immediately after create flinkCluster, will get empty status, but actually it`s running
	switch status.State {
	case flinkoperatorv1beta1.ClusterStateCreating,
		flinkoperatorv1beta1.ClusterStateReconciling,
		flinkoperatorv1beta1.ClusterStateUpdating,
		flinkoperatorv1beta1.ClusterStateRunning,
		"":
		statusDesc.Status = apistructs.StatusRunning
	case flinkoperatorv1beta1.ClusterStateStopping,
		flinkoperatorv1beta1.ClusterStatePartiallyStopped,
		flinkoperatorv1beta1.ClusterStateStopped:
		statusDesc.Status = apistructs.StatusStopped
		return statusDesc
	}
	if status.Components.Job == nil {
		return statusDesc
	}
	switch status.Components.Job.State {
	case flinkoperatorv1beta1.JobStateSucceeded:
		statusDesc.Status = apistructs.StatusStoppedOnOK
	case flinkoperatorv1beta1.JobStateFailed:
		statusDesc.Status = apistructs.StatusStoppedOnFailed
		return statusDesc
	case flinkoperatorv1beta1.JobStateCancelled:
		statusDesc.Status = apistructs.StatusStoppedByKilled
		return statusDesc
	}
	switch status.Components.TaskManagerStatefulSet.State {
	case flinkoperatorv1beta1.ComponentStateReady,
		flinkoperatorv1beta1.ComponentStateNotReady,
		flinkoperatorv1beta1.ComponentStateUpdating:
		statusDesc.Status = apistructs.StatusRunning
	case flinkoperatorv1beta1.ComponentStateDeleted:
		statusDesc.Status = apistructs.StatusStoppedByKilled
	}

	return statusDesc
}

func getBoolPoint(bl bool) *bool {
	return &bl
}

func composeOwnerReferences(versionGroup, kind, name string, uid types.UID) metav1.OwnerReference {
	return metav1.OwnerReference{
		APIVersion:         versionGroup,
		Kind:               kind,
		Name:               name,
		UID:                uid,
		Controller:         getBoolPoint(true),
		BlockOwnerDeletion: getBoolPoint(true),
	}
}

// composeLogConfig try to convert flinkConf`s logConfig field to map[string]string
// if convert failed or config is empty, return the default log config
func composeLogConfig(config string) map[string]string {
	if config == "" {
		return defaultLogConfig
	}

	decodeConfig, err := base64.StdEncoding.DecodeString(config)
	if err != nil {
		logrus.Errorf("failed to base64 decode customize logConfig, err: %v", err)
		return defaultLogConfig
	}

	customizeConfig := map[string]string{}
	if err := json.Unmarshal(decodeConfig, &customizeConfig); err != nil {
		logrus.Errorf("failed to unmarshal customize logConfig, err: %v", err)
		return defaultLogConfig
	}

	return customizeConfig
}
