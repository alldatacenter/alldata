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
	"sort"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	batchv1 "k8s.io/api/batch/v1"
	apiv1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/toleration"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/constraintbuilders"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/constraintbuilders/constraints"
)

func (k *Kubernetes) createJob(ctx context.Context, service *apistructs.Service, sg *apistructs.ServiceGroup) error {
	//delete history job
	k.deleteHistoryJob(service.Namespace, service.Name)
	job, err := k.newJob(service, sg)
	if err != nil {
		return errors.Errorf("failed to generate job struct, name: %s, (%v)", service.Name, err)
	}

	err = k.job.Create(job)
	if err != nil {
		return errors.Errorf("failed to create job, name: %s, (%v)", service.Name, err)
	}
	return nil
}

func (k *Kubernetes) newJob(service *apistructs.Service, serviceGroup *apistructs.ServiceGroup) (*batchv1.Job, error) {
	jobName := getjobName(service)

	job := &batchv1.Job{
		TypeMeta: metav1.TypeMeta{
			Kind:       "Job",
			APIVersion: "batch/v1",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:        jobName,
			Namespace:   service.Namespace,
			Labels:      make(map[string]string),
			Annotations: make(map[string]string),
		},
		Spec: batchv1.JobSpec{
			Template: apiv1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name:   jobName,
					Labels: make(map[string]string),
				},
				Spec: apiv1.PodSpec{
					ShareProcessNamespace: func(b bool) *bool { return &b }(false),
					Tolerations:           toleration.GenTolerations(),
				},
			},
			BackoffLimit: func(i int32) *int32 { return &i }(0),
		},
	}
	imagePullSecrets, err := k.setImagePullSecrets(service.Namespace)
	if err != nil {
		return nil, err
	}
	job.Spec.Template.Spec.ImagePullSecrets = imagePullSecrets

	affinity := constraintbuilders.K8S(&serviceGroup.ScheduleInfo2, service, []constraints.PodLabelsForAffinity{
		{PodLabels: map[string]string{"app": service.Name}}}, k).Affinity

	if v, ok := service.Env[DiceWorkSpace]; ok {
		affinity.NodeAffinity.PreferredDuringSchedulingIgnoredDuringExecution = append(
			affinity.NodeAffinity.PreferredDuringSchedulingIgnoredDuringExecution,
			k.composeDeploymentNodeAntiAffinityPreferred(v)...)
	}

	job.Spec.Template.Spec.Affinity = &affinity
	// inject hosts
	job.Spec.Template.Spec.HostAliases = ConvertToHostAlias(service.Hosts)

	container := apiv1.Container{
		Name:  service.Name,
		Image: service.Image,
	}

	err = k.setContainerResources(*service, &container)
	if err != nil {
		errMsg := fmt.Sprintf("set container resource err: %v", err)
		logrus.Errorf(errMsg)
		return nil, fmt.Errorf(errMsg)
	}

	logrus.Debugf("container name: %s, container resource spec: %+v", container.Name, container.Resources)

	// Generate initcontainer configuration
	initContainers := k.generateInitContainer(service.InitContainer)

	containers := []apiv1.Container{container}
	job.Spec.Template.Spec.Containers = containers
	if len(initContainers) > 0 {
		job.Spec.Template.Spec.InitContainers = initContainers
	}

	// k8s job Must have app label to manage pod
	job.Labels["app"] = service.Name
	job.Spec.Template.Labels["app"] = service.Name

	if job.Spec.Template.Annotations == nil {
		job.Spec.Template.Annotations = make(map[string]string)
	}
	podAnnotations(service, job.Spec.Template.Annotations)

	// set pod Annotations from service.Labels and service.JobLabels
	setPodAnnotationsFromLabels(service, job.Spec.Template.Annotations)

	// According to the current setting, there is only one user container in a pod
	if service.Cmd != "" {
		for i := range containers {
			//TODO:
			//cmds := strings.Split(service.Cmd, " ")
			cmds := []string{"sh", "-c", service.Cmd}
			containers[i].Command = cmds
		}
	}

	// ECI Pod inject fluent-bit sidecar container
	useECI := UseECI(job.Labels, job.Spec.Template.Labels)
	if useECI {
		sidecar, err := GenerateECIPodSidecarContainers(k.DeployInEdgeCluster())
		if err != nil {
			logrus.Errorf("%v", err)
			return nil, err
		}
		SetPodContainerLifecycleAndSharedVolumes(&job.Spec.Template.Spec)
		job.Spec.Template.Spec.Containers = append(job.Spec.Template.Spec.Containers, sidecar)
	}

	if err := k.AddContainersEnv(job.Spec.Template.Spec.Containers /*containers*/, service, serviceGroup); err != nil {
		return nil, err
	}

	SetPodAnnotationsBaseContainerEnvs(job.Spec.Template.Spec.Containers[0], job.Spec.Template.Annotations)

	// TODO: Delete this logic
	//Mobil temporary demand:
	// Inject the secret under the "secret" namespace into the business container

	secrets, err := k.CopyErdaSecrets("secret", service.Namespace)
	if err != nil {
		logrus.Errorf("failed to copy secret: %v", err)
		return nil, err
	}
	secretvolumes := []apiv1.Volume{}
	secretvolmounts := []apiv1.VolumeMount{}
	for _, secret := range secrets {
		secretvol, volmount := k.SecretVolume(&secret)
		secretvolumes = append(secretvolumes, secretvol)
		secretvolmounts = append(secretvolmounts, volmount)
	}

	err = k.AddPodMountVolume(service, &job.Spec.Template.Spec, secretvolmounts, secretvolumes)
	if err != nil {
		logrus.Errorf("failed to AddPodMountVolume for job %s/%s: %v", job.Namespace, job.Name, err)
		return nil, err
	}

	if err = DereferenceEnvs(&job.Spec.Template); err != nil {
		return nil, err
	}
	k.AddSpotEmptyDir(&job.Spec.Template.Spec, service.Resources.EmptyDirCapacity)

	job.Spec.Template.Spec.RestartPolicy = apiv1.RestartPolicyNever
	return job, nil
}

func (k *Kubernetes) deleteJob(namespace, name string) error {
	var err error
	var list batchv1.JobList
	if list, err = k.job.List(namespace, map[string]string{"app": name}); err != nil {
		logrus.Errorf("failed to list job in namespcae %s: %+\n", namespace, err)
		return err
	}
	for _, job := range list.Items {
		logrus.Errorf("job name for deleted: %+v\n", job.Name)
		if err = k.job.Delete(namespace, job.Name); err != nil {
			logrus.Errorf("failed to delete job %s in namespace %s: %+n", job.Name, namespace, err)
			return err
		}
	}
	return nil
}

func (k *Kubernetes) deleteHistoryJob(namespace, name string) error {
	var err error
	var list batchv1.JobList
	if list, err = k.job.List(namespace, map[string]string{"app": name}); err != nil {
		logrus.Errorf("failed to list job in namespcae %s: %+\n", namespace, err)
		return err
	}
	sort.Slice(list.Items, func(i, j int) bool {
		return list.Items[i].CreationTimestamp.After(list.Items[j].CreationTimestamp.Time)
	})
	if len(list.Items) < 2 {
		return nil
	}
	for _, job := range list.Items[2:] {
		logrus.Errorf("job name for deleted: %+v\n", job.Name)
		if err = k.job.Delete(namespace, job.Name); err != nil {
			logrus.Errorf("failed to delete job %s in namespace %s: %+n", job.Name, namespace, err)
			return err
		}
	}
	return nil
}

func (k *Kubernetes) getJobStatusFromMap(service *apistructs.Service, ns string) (apistructs.StatusDesc, error) {
	var (
		err        error
		jobName    string
		statusDesc apistructs.StatusDesc
	)
	if service.ProjectServiceName != "" {
		jobName = service.ProjectServiceName
	}
	jobName = service.Name
	var list batchv1.JobList
	if list, err = k.job.List(ns, map[string]string{"app": jobName}); err != nil {
		logrus.Errorf("failed to list job in namespcae %s: %+\n", ns, err)
		return statusDesc, err
	}
	sort.Slice(list.Items, func(i, j int) bool {
		return list.Items[j].CreationTimestamp.Before(&list.Items[i].CreationTimestamp)
	})
	statusDesc.Status = apistructs.StatusUnknown
	if len(list.Items) < 1 {
		return statusDesc, nil
	}
	if list.Items[0].Status.Succeeded == 1 {
		statusDesc.Status = apistructs.StatusReady
	} else if list.Items[0].Status.Active == 1 {
		statusDesc.Status = apistructs.StatusProgressing
	} else if list.Items[0].Status.Failed == 1 {
		statusDesc.Status = apistructs.StatusFailed
		statusDesc.Reason = fmt.Sprintf("This job execution failed.")
	} else {
		statusDesc.Status = apistructs.StatusUnknown
	}
	return statusDesc, nil
}
func getjobName(service *apistructs.Service) string {
	if service.ProjectServiceName != "" {
		return service.ProjectServiceName + uuid.UUID()[:6]
	}
	return service.Name + uuid.UUID()[:6]
}
