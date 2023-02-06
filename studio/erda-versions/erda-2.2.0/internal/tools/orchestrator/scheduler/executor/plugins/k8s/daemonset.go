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
	"context"
	"fmt"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8sapi"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/toleration"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/constraintbuilders"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/constraintbuilders/constraints"
	"github.com/erda-project/erda/pkg/strutil"
)

func (k *Kubernetes) createDaemonSet(ctx context.Context, service *apistructs.Service, sg *apistructs.ServiceGroup) error {
	daemonset, err := k.newDaemonSet(service, sg)
	if err != nil {
		return errors.Errorf("failed to generate daemonset struct, name: %s, (%v)", service.Name, err)
	}

	return k.ds.Create(daemonset)
}

func (k *Kubernetes) getDaemonSetStatusFromMap(service *apistructs.Service, daemonsets map[string]appsv1.DaemonSet) (apistructs.StatusDesc, error) {
	var (
		statusDesc apistructs.StatusDesc
	)
	dsName := getDeployName(service)

	if daemonSet, ok := daemonsets[dsName]; ok {
		status := daemonSet.Status

		statusDesc.Status = apistructs.StatusUnknown

		if status.NumberAvailable == status.DesiredNumberScheduled {
			statusDesc.Status = apistructs.StatusReady
		} else {
			statusDesc.Status = apistructs.StatusUnHealthy
		}
	}

	return statusDesc, nil
}

func (k *Kubernetes) deleteDaemonSet(namespace, name string) error {
	logrus.Debugf("delete daemonset %s on namespace %s", name, namespace)
	return k.ds.Delete(namespace, name)
}

func (k *Kubernetes) updateDaemonSet(ctx context.Context, ds *appsv1.DaemonSet, service *apistructs.Service) error {
	_, projectID, workspace, runtimeID := extractContainerEnvs(ds.Spec.Template.Spec.Containers)
	deltaCPU, deltaMem, err := k.getDaemonSetDeltaResource(ctx, ds)
	if err != nil {
		logrus.Errorf("faield to get delta resource for daemonSet %s, %v", ds.Name, err)
	} else {
		ok, reason, err := k.CheckQuota(ctx, projectID, workspace, runtimeID, deltaCPU, deltaMem, "update", service.Name)
		if err != nil {
			return err
		}
		if !ok {
			return errors.New(reason)
		}
	}
	return k.ds.Update(ds)
}

func (k *Kubernetes) getDaemonSetDeltaResource(ctx context.Context, ds *appsv1.DaemonSet) (deltaCPU, deltaMemory int64, err error) {
	oldDs, err := k.k8sClient.ClientSet.AppsV1().DaemonSets(ds.Namespace).Get(ctx, ds.Name, metav1.GetOptions{})
	if err != nil {
		return 0, 0, err
	}
	oldCPU, oldMem := getRequestsResources(oldDs.Spec.Template.Spec.Containers)
	newCPU, newMem := getRequestsResources(ds.Spec.Template.Spec.Containers)

	deltaCPU = newCPU - oldCPU
	deltaMemory = newMem - oldMem
	return
}

func (k *Kubernetes) getDaemonSet(namespace, name string) (*appsv1.DaemonSet, error) {
	return k.ds.Get(namespace, name)
}

func (k *Kubernetes) newDaemonSet(service *apistructs.Service, sg *apistructs.ServiceGroup) (*appsv1.DaemonSet, error) {
	deployName := getDeployName(service)
	enableServiceLinks := false
	if _, ok := sg.Labels[EnableServiceLinks]; ok {
		enableServiceLinks = true
	}
	daemonset := &appsv1.DaemonSet{
		TypeMeta: metav1.TypeMeta{
			Kind:       "DaemonSet",
			APIVersion: "apps/v1",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      deployName,
			Namespace: service.Namespace,
			Labels:    make(map[string]string),
		},
		Spec: appsv1.DaemonSetSpec{
			Selector: &metav1.LabelSelector{
				MatchLabels: map[string]string{"app": service.Name},
			},
			Template: corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name:   deployName,
					Labels: make(map[string]string),
				},
				Spec: corev1.PodSpec{
					EnableServiceLinks:    func(enable bool) *bool { return &enable }(enableServiceLinks),
					ShareProcessNamespace: func(b bool) *bool { return &b }(false),
					Tolerations:           toleration.GenTolerations(),
				},
			},
			UpdateStrategy:       appsv1.DaemonSetUpdateStrategy{},
			MinReadySeconds:      0,
			RevisionHistoryLimit: func(i int32) *int32 { return &i }(int32(3)),
		},
		Status: appsv1.DaemonSetStatus{},
	}

	if v := k.options["FORCE_BLUE_GREEN_DEPLOY"]; v != "true" &&
		(strutil.ToUpper(service.Env[DiceWorkSpace]) == apistructs.DevWorkspace.String() ||
			strutil.ToUpper(service.Env[DiceWorkSpace]) == apistructs.TestWorkspace.String()) {
		daemonset.Spec.UpdateStrategy = appsv1.DaemonSetUpdateStrategy{Type: appsv1.RollingUpdateDaemonSetStrategyType}
	}

	affinity := constraintbuilders.K8S(&sg.ScheduleInfo2, service, []constraints.PodLabelsForAffinity{
		{PodLabels: map[string]string{"app": deployName}}}, k).Affinity
	daemonset.Spec.Template.Spec.Affinity = &affinity

	imagePullSecrets, err := k.setImagePullSecrets(service.Namespace)
	if err != nil {
		return nil, err
	}
	daemonset.Spec.Template.Spec.ImagePullSecrets = imagePullSecrets

	cpu := fmt.Sprintf("%.fm", service.Resources.Cpu*1000)
	memory := fmt.Sprintf("%.fMi", service.Resources.Mem)

	maxCpu := fmt.Sprintf("%.fm", service.Resources.MaxCPU*1000)
	maxMem := fmt.Sprintf("%.fMi", service.Resources.MaxMem)

	container := corev1.Container{
		// TODO, container name e.g. redis-1528180634
		Name:  deployName,
		Image: service.Image,
		Resources: corev1.ResourceRequirements{
			Requests: corev1.ResourceList{
				corev1.ResourceCPU:              resource.MustParse(cpu),
				corev1.ResourceMemory:           resource.MustParse(memory),
				corev1.ResourceEphemeralStorage: resource.MustParse(k8sapi.EphemeralStorageSizeRequest),
			},
			Limits: corev1.ResourceList{
				corev1.ResourceCPU:              resource.MustParse(maxCpu),
				corev1.ResourceMemory:           resource.MustParse(maxMem),
				corev1.ResourceEphemeralStorage: resource.MustParse(k8sapi.EphemeralStorageSizeLimit),
			},
		},
	}

	if service.Resources.EphemeralStorageCapacity > 1 {
		maxEphemeral := fmt.Sprintf("%dGi", service.Resources.EphemeralStorageCapacity)
		container.Resources.Limits[corev1.ResourceEphemeralStorage] = resource.MustParse(maxEphemeral)
	}

	//Set the over-score ratio according to the environment
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
	if err := k.SetFineGrainedCPU(&container, sg.Extra, cpuSubscribeRatio); err != nil {
		return nil, err
	}

	if err := k.SetOverCommitMem(&container, memSubscribeRatio); err != nil {
		return nil, err
	}

	// Generate sidecars container configuration
	sidecars := k.generateSidecarContainers(service.SideCars)

	// Generate initcontainer configuration
	initcontainers := k.generateInitContainer(service.InitContainer)

	containers := []corev1.Container{container}
	containers = append(containers, sidecars...)
	daemonset.Spec.Template.Spec.Containers = containers
	if len(initcontainers) > 0 {
		daemonset.Spec.Template.Spec.InitContainers = initcontainers
	}

	daemonset.Spec.Selector.MatchLabels[LabelServiceGroupID] = sg.ID
	daemonset.Spec.Template.Labels[LabelServiceGroupID] = sg.ID
	daemonset.Labels[LabelServiceGroupID] = sg.ID
	daemonset.Labels["app"] = service.Name
	daemonset.Spec.Template.Labels["app"] = service.Name

	if daemonset.Spec.Template.Annotations == nil {
		daemonset.Spec.Template.Annotations = make(map[string]string)
	}
	podAnnotations(service, daemonset.Spec.Template.Annotations)

	// inherit Labels from service.Labels and service.DeploymentLabels
	err = inheritDaemonsetLabels(service, daemonset)
	if err != nil {
		logrus.Errorf("failed to set labels for service %s for Pod with error: %v\n", service.Name, err)
		return nil, err
	}

	// set pod Annotations from service.Labels and service.DeploymentLabels
	setPodAnnotationsFromLabels(service, daemonset.Spec.Template.Annotations)

	// daemonset pod should not deployed on ECI
	daemonset.Spec.Template.Spec.Affinity = &corev1.Affinity{
		NodeAffinity: &corev1.NodeAffinity{
			RequiredDuringSchedulingIgnoredDuringExecution: &corev1.NodeSelector{
				NodeSelectorTerms: []corev1.NodeSelectorTerm{
					{
						MatchExpressions: []corev1.NodeSelectorRequirement{
							{
								Key:      "type",
								Operator: corev1.NodeSelectorOpNotIn,
								Values:   []string{"virtual-kubelet"},
							},
						},
					},
				},
			},
		},
	}

	// According to the current setting, there is only one user container in a pod
	if service.Cmd != "" {
		for i := range containers {
			cmds := []string{"sh", "-c", service.Cmd}
			containers[i].Command = cmds
		}
	}

	SetHealthCheck(&daemonset.Spec.Template.Spec.Containers[0], service)

	if err := k.AddContainersEnv(containers, service, sg); err != nil {
		return nil, err
	}

	SetPodAnnotationsBaseContainerEnvs(daemonset.Spec.Template.Spec.Containers[0], daemonset.Spec.Template.Annotations)

	secrets, err := k.CopyErdaSecrets("secret", service.Namespace)
	if err != nil {
		logrus.Errorf("failed to copy secret: %v", err)
		return nil, err
	}
	secretvolumes := []corev1.Volume{}
	secretvolmounts := []corev1.VolumeMount{}
	for _, secret := range secrets {
		secretvol, volmount := k.SecretVolume(&secret)
		secretvolumes = append(secretvolumes, secretvol)
		secretvolmounts = append(secretvolmounts, volmount)
	}

	err = k.AddPodMountVolume(service, &daemonset.Spec.Template.Spec, secretvolmounts, secretvolumes)
	if err != nil {
		logrus.Errorf("failed to AddPodMountVolume for daemonset %s/%s: %v", daemonset.Namespace, daemonset.Name, err)
		return nil, err
	}
	k.AddSpotEmptyDir(&daemonset.Spec.Template.Spec, service.Resources.EmptyDirCapacity)

	logrus.Debugf("show k8s daemonset, name: %s, daemonset: %+v", deployName, daemonset)

	return daemonset, nil
}
