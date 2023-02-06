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

package k8s

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"strconv"
	"strings"
	"text/template"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	appsv1 "k8s.io/api/apps/v1"
	apiv1 "k8s.io/api/core/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8sapi"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/toleration"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/constraintbuilders"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/constraintbuilders/constraints"
	"github.com/erda-project/erda/pkg/strutil"
)

func (k *Kubernetes) createStatefulSet(ctx context.Context, info StatefulsetInfo) error {

	statefulName := statefulsetName(info.sg)

	set := &appsv1.StatefulSet{
		TypeMeta: metav1.TypeMeta{
			APIVersion: "apps/v1",
			Kind:       "StatefulSet",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:        statefulName,
			Namespace:   info.namespace,
			Labels:      make(map[string]string),
			Annotations: make(map[string]string),
		},
	}

	// Associate the original service name with the instance name under statefulset
	for k, v := range info.annotations {
		set.Annotations[k] = v
	}

	set.Spec = appsv1.StatefulSetSpec{
		RevisionHistoryLimit: func(i int32) *int32 { return &i }(int32(3)),
		Replicas:             func(i int32) *int32 { return &i }(int32(len(info.sg.Services))),
		ServiceName:          statefulName,
	}

	set.Spec.Selector = &metav1.LabelSelector{
		MatchLabels: map[string]string{
			"app":      statefulName,
			"addon_id": info.sg.Dice.ID,
		},
	}

	// Take one of the services
	service := &info.sg.Services[0]

	cpu := fmt.Sprintf("%.fm", service.Resources.Cpu*1000)
	memory := fmt.Sprintf("%.fMi", service.Resources.Mem)

	maxCpu := fmt.Sprintf("%.fm", service.Resources.MaxCPU*1000)
	maxMem := fmt.Sprintf("%.fMi", service.Resources.MaxMem)

	affinity := constraintbuilders.K8S(&info.sg.ScheduleInfo2, service, []constraints.PodLabelsForAffinity{
		{
			PodLabels: map[string]string{"addon_id": info.sg.Dice.ID},
		}}, k).Affinity

	if v, ok := service.Env[DiceWorkSpace]; ok {
		affinity.NodeAffinity.PreferredDuringSchedulingIgnoredDuringExecution = append(
			affinity.NodeAffinity.PreferredDuringSchedulingIgnoredDuringExecution,
			k.composeStatefulSetNodeAntiAffinityPreferred(v)...)
	}

	set.Spec.Template = apiv1.PodTemplateSpec{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: info.namespace,
			Labels: map[string]string{
				"app":      statefulName,
				"addon_id": info.sg.Dice.ID,
			},
			Annotations: map[string]string{},
		},
		Spec: apiv1.PodSpec{
			EnableServiceLinks:    func(enable bool) *bool { return &enable }(false),
			ShareProcessNamespace: func(b bool) *bool { return &b }(false),
			Tolerations:           toleration.GenTolerations(),
		},
	}

	hasHostPath := serviceHasHostpath(service)
	err := setPodLabelsFromService(hasHostPath, service.Labels, set.Labels)
	if err != nil {
		return errors.Errorf("error in service.Labels: %v for statefulset %v in namesapce %v", err, set.Name, set.Namespace)
	}
	err = setPodLabelsFromService(hasHostPath, service.Labels, set.Spec.Template.Labels)
	if err != nil {
		return errors.Errorf("error in service.Labels: %v for statefulset %v in namesapce %v", err, set.Name, set.Namespace)
	}
	err = setPodLabelsFromService(hasHostPath, service.DeploymentLabels, set.Spec.Template.Labels)
	if err != nil {
		return errors.Errorf("error in service.DeploymentLabels: %v for statefulset %v in namesapce %v", err, set.Name, set.Namespace)
	}

	// set pod Annotations from service.Labels and service.DeploymentLabels
	setPodAnnotationsFromLabels(service, set.Spec.Template.Annotations)

	set.Spec.Template.Spec.Affinity = &affinity

	imagePullSecrets, err := k.setImagePullSecrets(service.Namespace)
	if err != nil {
		return err
	}
	set.Spec.Template.Spec.ImagePullSecrets = imagePullSecrets
	// Currently only one business container is set in our Pod
	container := &apiv1.Container{
		Name:  statefulName,
		Image: service.Image,
		Resources: apiv1.ResourceRequirements{
			Requests: apiv1.ResourceList{
				apiv1.ResourceCPU:              resource.MustParse(cpu),
				apiv1.ResourceMemory:           resource.MustParse(memory),
				apiv1.ResourceEphemeralStorage: resource.MustParse(k8sapi.EphemeralStorageSizeRequest),
			},
			Limits: apiv1.ResourceList{
				apiv1.ResourceCPU:              resource.MustParse(maxCpu),
				apiv1.ResourceMemory:           resource.MustParse(maxMem),
				apiv1.ResourceEphemeralStorage: resource.MustParse(k8sapi.EphemeralStorageSizeLimit),
			},
		},
	}
	if service.Resources.EphemeralStorageCapacity > 1 {
		maxEphemeral := fmt.Sprintf("%dGi", service.Resources.EphemeralStorageCapacity)
		container.Resources.Limits[corev1.ResourceEphemeralStorage] = resource.MustParse(maxEphemeral)
	}
	// Forced to pull the image
	//container.ImagePullPolicy = apiv1.PullAlways

	// setting volume
	if err := k.setVolume(set, container, service); err != nil {
		return err
	}

	// configure health check
	SetHealthCheck(container, service)

	if len(service.Cmd) > 0 {
		container.Command = []string{"sh", "-c", service.Cmd}
	}

	// Set the over-score ratio according to the environment
	cpuSubscribeRatio := k.cpuSubscribeRatio
	memSubscribeRatio := k.memSubscribeRatio
	switch strutil.ToUpper(service.Env[DiceWorkSpace]) {
	case "DEV":
		cpuSubscribeRatio = k.devCpuSubscribeRatio
		memSubscribeRatio = k.devMemSubscribeRatio
	case "TEST":
		cpuSubscribeRatio = k.testCpuSubscribeRatio
		memSubscribeRatio = k.testMemSubscribeRatio
	case "STAGING":
		cpuSubscribeRatio = k.stagingCpuSubscribeRatio
		memSubscribeRatio = k.stagingMemSubscribeRatio
	}

	// Set fine-grained CPU based on the oversold ratio
	if err := k.SetFineGrainedCPU(container, info.sg.Extra, cpuSubscribeRatio); err != nil {
		return err
	}
	if err := k.SetOverCommitMem(container, memSubscribeRatio); err != nil {
		return err
	}

	set.Spec.Template.Spec.Containers = []apiv1.Container{*container}

	// ECI Pod inject fluent-bit sidecar container
	useECI := UseECI(set.Labels, set.Spec.Template.Labels)
	if useECI {
		sidecar, err := GenerateECIPodSidecarContainers(k.DeployInEdgeCluster())
		if err != nil {
			logrus.Errorf("%v", err)
			return err
		}
		SetPodContainerLifecycleAndSharedVolumes(&set.Spec.Template.Spec)
		set.Spec.Template.Spec.Containers = append(set.Spec.Template.Spec.Containers, sidecar)
	}

	// Set the statefulset environment variable
	for i := range set.Spec.Template.Spec.Containers {
		setEnv(&set.Spec.Template.Spec.Containers[i], info.envs, info.sg, info.namespace)
	}
	//setEnv(container, info.envs, info.sg, info.namespace)

	if info.namespace == "fake-test" {
		return nil
	}

	SetPodAnnotationsBaseContainerEnvs(set.Spec.Template.Spec.Containers[0], set.Spec.Template.Annotations)

	return k.sts.Create(set)
}

func extractContainerEnvs(containers []corev1.Container) (addonID, projectID, workspace, runtimeID string) {
	envSuffixMap := map[string]*string{
		"ADDON_ID":        &addonID,
		"DICE_PROJECT_ID": &projectID,
		"DICE_RUNTIME_ID": &runtimeID,
		"DICE_WORKSPACE":  &workspace,
	}
	for _, container := range containers {
		for _, env := range container.Env {
			for k, v := range envSuffixMap {
				if strutil.HasSuffixes(env.Name, k) && *v == "" {
					*v = env.Value
				}
			}
		}
	}
	return
}

func extractServicesEnvs(runtime *apistructs.ServiceGroup) (string, string, string, string) {
	envSuffixMap := map[string]string{
		"ADDON_ID":        "",
		"DICE_PROJECT_ID": "",
		"DICE_RUNTIME_ID": "",
		"DICE_WORKSPACE":  "",
	}
	for _, svc := range runtime.Services {
		for envKey, env := range svc.Env {
			for k := range envSuffixMap {
				if strings.Contains(envKey, k) {
					envSuffixMap[k] = env
				}
			}
		}
	}
	for envKey, env := range runtime.Dice.Labels {
		for k := range envSuffixMap {
			if strings.Contains(envKey, k) {
				envSuffixMap[k] = env
			}
		}
	}
	return envSuffixMap["ADDON_ID"], envSuffixMap["DICE_PROJECT_ID"], envSuffixMap["DICE_RUNTIME_ID"], envSuffixMap["DICE_WORKSPACE"]
}

// setBind only set hostPath for volume
func (k *Kubernetes) setBind(set *appsv1.StatefulSet, container *apiv1.Container, service *apistructs.Service) error {
	for i, bind := range service.Binds {
		if bind.HostPath == "" || bind.ContainerPath == "" {
			return errors.New("bind HostPath or ContainerPath is empty")
		}

		clusterInfo, err := k.ClusterInfo.Get()
		if err != nil {
			return err
		}
		hostPath, err := ParseJobHostBindTemplate(bind.HostPath, clusterInfo)
		if err != nil {
			return err
		}

		// Name formation: '[a-z0-9]([-a-z0-9]*[a-z0-9])?'
		name := strutil.Concat("hostpath-", strconv.Itoa(i))

		// The hostPath that does not start with an absolute path is used to apply for local disk resources in the old volume interface
		if !strings.HasPrefix(hostPath, "/") {
			//hostPath = strutil.Concat("/mnt/k8s/", hostPath)
			k.requestLocalVolume(set, container, bind)
			continue
		}

		// k8s hostPath Achieve listing
		set.Spec.Template.Spec.Volumes = append(set.Spec.Template.Spec.Volumes, apiv1.Volume{
			Name: name,
			VolumeSource: apiv1.VolumeSource{
				HostPath: &apiv1.HostPathVolumeSource{
					Path: hostPath,
				},
			},
		})

		container.VolumeMounts = append(container.VolumeMounts,
			apiv1.VolumeMount{
				Name:      name,
				MountPath: bind.ContainerPath,
				ReadOnly:  bind.ReadOnly,
			})
	}

	return nil
}

func (k *Kubernetes) setVolume(set *appsv1.StatefulSet, container *apiv1.Container, service *apistructs.Service) error {

	return k.setStatefulSetServiceVolumes(set, container, service)

	// HostPath is all used in Bind, and the host path is mounted to the container
	//return k.setBind(set, container, service)

	// new volume interface
	// configNewVolume(set, container, service)
}

func (k *Kubernetes) requestLocalVolume(set *appsv1.StatefulSet, container *apiv1.Container, bind apistructs.ServiceBind) {
	logrus.Infof("in requestLocalVolume, statefulset name: %s, hostPath: %s", set.Name, bind.HostPath)

	sc := localStorage
	capacity := "20Gi"
	if bind.SCVolume.Capacity >= 20 {
		capacity = fmt.Sprintf("%dGi", bind.SCVolume.Capacity)
	}

	if bind.SCVolume.StorageClassName != "" {
		sc = bind.SCVolume.StorageClassName
	}

	hostPath := bind.HostPath
	pvcName := strings.Replace(hostPath, "_", "-", -1)
	set.Spec.VolumeClaimTemplates = append(set.Spec.VolumeClaimTemplates, apiv1.PersistentVolumeClaim{
		ObjectMeta: metav1.ObjectMeta{
			Name: pvcName,
		},
		Spec: apiv1.PersistentVolumeClaimSpec{
			AccessModes: []apiv1.PersistentVolumeAccessMode{apiv1.ReadWriteOnce},
			Resources: apiv1.ResourceRequirements{
				Requests: apiv1.ResourceList{
					apiv1.ResourceStorage: resource.MustParse(capacity),
				},
			},
			StorageClassName: &sc,
		},
	})

	container.VolumeMounts = append(container.VolumeMounts,
		apiv1.VolumeMount{
			Name:      pvcName,
			MountPath: bind.ContainerPath,
			ReadOnly:  bind.ReadOnly,
		})
}

// new volume interface
func configNewVolume(set *appsv1.StatefulSet, container *apiv1.Container, service *apistructs.Service) {
	if len(service.Volumes) == 0 {
		return
	}
	// The volume of statefulset uses local disk or nas network disk
	// The local disk is simulated by hostPath, under the premise that the instances of statefulset are scheduled to different instances
	for _, vol := range service.Volumes {
		nas := 0
		local := 0
		var (
			name string
			path string
		)
		switch vol.VolumeType {
		case apistructs.LocalVolume:
			name = strutil.Concat("localvolume-", strconv.Itoa(local))
			path = strutil.Concat("/mnt/dice/volumes/", vol.VolumePath)
			local++
		case apistructs.NasVolume:
			name = strutil.Concat("nas-", strconv.Itoa(nas))
			path = vol.VolumePath
			nas++
		}

		container.VolumeMounts = []apiv1.VolumeMount{
			{
				Name:      name,
				MountPath: vol.ContainerPath,
			},
		}

		set.Spec.Template.Spec.Volumes = append(set.Spec.Template.Spec.Volumes,
			apiv1.Volume{
				Name: name,
				VolumeSource: apiv1.VolumeSource{
					HostPath: &apiv1.HostPathVolumeSource{
						Path: path,
					},
				},
			},
		)
	}
}

func setEnv(container *apiv1.Container, allEnv map[string]string, sg *apistructs.ServiceGroup, ns string) {
	// copy all env variable
	for k, v := range allEnv {
		// The key length of the processed environment variable should be greater than 3
		if len(k) <= 3 {
			continue
		}
		container.Env = append(container.Env,
			// The prefixes N0_, N1_, N2_, and other environment variables have been added
			apiv1.EnvVar{
				Name:  k,
				Value: v,
			})
	}
	// add K8S label
	container.Env = append(container.Env,
		apiv1.EnvVar{
			Name:  "IS_K8S",
			Value: "true",
		})
	// add namespace label
	container.Env = append(container.Env,
		apiv1.EnvVar{
			Name:  "DICE_NAMESPACE",
			Value: ns,
		})
	if len(sg.Services) > 0 {
		service := sg.Services[0]
		if len(service.Ports) >= 1 {
			container.Env = append(container.Env,
				apiv1.EnvVar{Name: "SELF_PORT", Value: fmt.Sprintf("%d", service.Ports[0].Port)})
		}
		for i, port := range service.Ports {
			container.Env = append(container.Env,
				apiv1.EnvVar{
					Name:  fmt.Sprintf("SELF_PORT%d", i),
					Value: fmt.Sprintf("%d", port.Port),
				})
		}

		requestmem, _ := container.Resources.Requests.Memory().AsInt64()
		limitmem, _ := container.Resources.Limits.Memory().AsInt64()
		container.Env = append(container.Env,
			apiv1.EnvVar{
				Name:  "DICE_CPU_ORIGIN",
				Value: fmt.Sprintf("%f", service.Resources.Cpu)},
			apiv1.EnvVar{
				Name:  "DICE_MEM_ORIGIN",
				Value: fmt.Sprintf("%f", service.Resources.Mem)},
			apiv1.EnvVar{
				Name:  "DICE_CPU_REQUEST",
				Value: container.Resources.Requests.Cpu().AsDec().String()},
			apiv1.EnvVar{
				Name:  "DICE_MEM_REQUEST",
				Value: fmt.Sprintf("%d", requestmem/1024/1024)},
			apiv1.EnvVar{
				Name:  "DICE_CPU_LIMIT",
				Value: container.Resources.Limits.Cpu().AsDec().String()},
			apiv1.EnvVar{
				Name:  "DICE_MEM_LIMIT",
				Value: fmt.Sprintf("%d", limitmem/1024/1024)},
		)
	}

	container.Env = append(container.Env, apiv1.EnvVar{
		Name: "POD_IP",
		ValueFrom: &apiv1.EnvVarSource{
			FieldRef: &apiv1.ObjectFieldSelector{
				APIVersion: "v1",
				FieldPath:  "status.podIP",
			},
		},
	}, apiv1.EnvVar{
		Name: "HOST_IP",
		ValueFrom: &apiv1.EnvVarSource{
			FieldRef: &apiv1.ObjectFieldSelector{
				APIVersion: "v1",
				FieldPath:  "status.hostIP",
			},
		},
	}, apiv1.EnvVar{
		Name: "SELF_HOST",
		ValueFrom: &apiv1.EnvVarSource{
			FieldRef: &apiv1.ObjectFieldSelector{
				APIVersion: "v1",
				FieldPath:  "status.podIP",
			},
		},
	}, apiv1.EnvVar{
		Name: "POD_NAME",
		ValueFrom: &apiv1.EnvVarSource{
			FieldRef: &apiv1.ObjectFieldSelector{
				APIVersion: "v1",
				FieldPath:  "metadata.name",
			},
		},
	}, apiv1.EnvVar{
		Name: "POD_UUID",
		ValueFrom: &apiv1.EnvVarSource{
			FieldRef: &apiv1.ObjectFieldSelector{
				APIVersion: "v1",
				FieldPath:  "metadata.uid",
			},
		},
	})
}

func convertStatus(status apiv1.PodStatus) apistructs.StatusCode {
	switch status.Phase {
	case apiv1.PodRunning:
		for _, cond := range status.Conditions {
			if cond.Status == "False" {
				return apistructs.StatusProgressing
			}
		}
		return apistructs.StatusReady
	}
	return apistructs.StatusProgressing
}

// statefulset The name is defined as set by the user id
func statefulsetName(sg *apistructs.ServiceGroup) string {
	statefulName, ok := getGroupID(&sg.Services[0])
	if !ok {
		logrus.Errorf("failed to get groupID from service labels, name: %s", sg.ID)
		return sg.ID
	}
	return statefulName
}

// ParseJobHostBindTemplate Analyze the hostPath template and convert it to the cluster info value
func ParseJobHostBindTemplate(hostPath string, clusterInfo map[string]string) (string, error) {
	var b bytes.Buffer

	if hostPath == "" {
		return "", errors.New("hostPath is empty")
	}

	t, err := template.New("jobBind").
		Option("missingkey=error").
		Parse(hostPath)
	if err != nil {
		return "", errors.Errorf("failed to parse bind, hostPath: %s, (%v)", hostPath, err)
	}

	err = t.Execute(&b, &clusterInfo)
	if err != nil {
		return "", errors.Errorf("failed to execute bind, hostPath: %s, (%v)", hostPath, err)
	}

	return b.String(), nil
}

func (k *Kubernetes) setStatefulSetServiceVolumes(set *appsv1.StatefulSet, container *apiv1.Container, service *apistructs.Service) error {

	//step 1: set hostpath volume if service.Binds not empty
	if err := k.setBind(set, container, service); err != nil {
		return err
	}

	//step 2: set pvc volume if service.Volumes not empty
	sc := localStorage
	capacity := "20Gi"
	for _, vol := range service.Volumes {
		if vol.ContainerPath == "" {
			return errors.New("volume targetPath is empty")
		}

		if vol.SCVolume.Capacity >= diceyml.AddonVolumeSizeMin && vol.SCVolume.Capacity <= diceyml.AddonVolumeSizeMax {
			capacity = fmt.Sprintf("%dGi", vol.SCVolume.Capacity)
		}

		if vol.SCVolume.Capacity > diceyml.AddonVolumeSizeMax {
			capacity = fmt.Sprintf("%dGi", diceyml.AddonVolumeSizeMax)
		}

		if vol.SCVolume.StorageClassName != "" {
			sc = vol.SCVolume.StorageClassName
		}

		// 校验 sc 是否存在（volume 的 type + vendor 的组合可能会产生当前不支持的 sc）
		_, err := k.storageClass.Get(sc)
		if err != nil {
			if err.Error() == "not found" {
				logrus.Errorf("failed to set volume for sevice %s: storageclass %s not found.", service.Name, sc)
				return errors.Errorf("failed to set volume for sevice %s: storageclass %s not found.", service.Name, sc)
			} else {
				logrus.Errorf("failed to set volume for sevice %s: get storageclass %s from cluster failed: %#v", service.Name, sc, err)
				return errors.Errorf("failed to set volume for sevice %s: get storageclass %s from cluster failed: %#v", service.Name, sc, err)
			}
		}

		pvcName := fmt.Sprintf("%s-%s-%s", service.Name, container.Name, vol.ID)
		pvc := apiv1.PersistentVolumeClaim{
			ObjectMeta: metav1.ObjectMeta{
				Name: pvcName,
			},
			Spec: apiv1.PersistentVolumeClaimSpec{
				AccessModes: []apiv1.PersistentVolumeAccessMode{apiv1.ReadWriteOnce},
				Resources: apiv1.ResourceRequirements{
					Requests: apiv1.ResourceList{
						apiv1.ResourceStorage: resource.MustParse(capacity),
					},
				},
				StorageClassName: &sc,
			},
		}

		if vol.SCVolume.Snapshot != nil && vol.SCVolume.Snapshot.MaxHistory > 0 {
			if sc == apistructs.AlibabaSSDSC {
				pvc.Annotations = make(map[string]string)
				vs := diceyml.VolumeSnapshot{
					MaxHistory: vol.SCVolume.Snapshot.MaxHistory,
				}
				vsMap := map[string]diceyml.VolumeSnapshot{}
				vsMap[sc] = vs
				data, _ := json.Marshal(vsMap)
				pvc.Annotations[apistructs.CSISnapshotMaxHistory] = string(data)
			} else {
				logrus.Warnf("Service %s pvc volume use storageclass %v, it do not support snapshot. Only volume.type SSD for Alibaba disk SSD support snapshot\n", service.Name, sc)
			}
		}
		set.Spec.VolumeClaimTemplates = append(set.Spec.VolumeClaimTemplates, pvc)

		container.VolumeMounts = append(container.VolumeMounts,
			apiv1.VolumeMount{
				Name:      pvcName,
				MountPath: vol.TargetPath,
				ReadOnly:  vol.ReadOnly,
			})

	}

	return nil
}

// scaleStatefulSet scale statefulset application
func (k *Kubernetes) scaleStatefulSet(ctx context.Context, sg *apistructs.ServiceGroup) error {
	// only support scale the first one service
	ns := sg.Services[0].Namespace
	if ns == "" {
		ns = MakeNamespace(sg)
	}

	scalingService := sg.Services[0]
	//statefulSetName := statefulsetName(sg)

	statefulSetName, ok := getGroupID(&sg.Services[0])
	if !ok {
		statefulSetName = sg.ID
	}

	logrus.Infof("scaleStatefulSet for name %s in namespace: %s sg.ID: %s sg %#v", statefulSetName, ns, sg.ID, *sg)
	sts, err := k.sts.Get(ns, statefulSetName)
	if err != nil {
		getErr := fmt.Errorf("failed to get the statefulset %s in namespace %s, err is: %s", statefulSetName, ns, err.Error())
		return getErr
	}

	oldCPU, oldMem := getRequestsResources(sts.Spec.Template.Spec.Containers)
	if sts.Spec.Replicas != nil {
		oldCPU *= int64(*sts.Spec.Replicas)
		oldMem *= int64(*sts.Spec.Replicas)
	}

	if scalingService.Scale == 0 {
		// 表示停止 statefulset
		sts.Spec.Replicas = func(i int32) *int32 { return &i }(int32(scalingService.Scale))
	} else {
		// 表示恢复 statefulset
		sts.Spec.Replicas = func(i int32) *int32 { return &i }(int32(len(sg.Services)))
	}

	// only support one container on Erda currently
	for index := range sts.Spec.Template.Spec.Containers {
		container := sts.Spec.Template.Spec.Containers[index]

		err = k.setContainerResources(scalingService, &container)
		if err != nil {
			setContainerErr := fmt.Errorf("failed to set container resource, err is: %s", err.Error())
			return setContainerErr
		}

		k.UpdateContainerResourceEnv(scalingService.Resources, &container)

		sts.Spec.Template.Spec.Containers[index] = container
	}
	sts.ResourceVersion = ""

	newCPU, newMem := getRequestsResources(sts.Spec.Template.Spec.Containers)
	newCPU *= int64(*sts.Spec.Replicas)
	newMem *= int64(*sts.Spec.Replicas)

	_, projectID, workspace, runtimeID := extractContainerEnvs(sts.Spec.Template.Spec.Containers)
	ok, reason, err := k.CheckQuota(ctx, projectID, workspace, runtimeID, newCPU-oldCPU, newMem-oldMem, "scale", scalingService.Name)
	if err != nil {
		return err
	}
	if !ok {
		return errors.New(reason)
	}

	err = k.sts.Put(sts)
	if err != nil {
		updateErr := fmt.Errorf("failed to update the statefulset %s in namespace %s, err is: %s", sts.Name, sts.Namespace, err.Error())
		return updateErr
	}
	return nil
}
