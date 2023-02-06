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
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"
	batchv1 "k8s.io/api/batch/v1"
	apiv1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/clusterinfo"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/job"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/secret"
)

func TestNewJob(t *testing.T) {
	service := &apistructs.Service{
		Name:          "test-job",
		Namespace:     "test",
		Image:         "test",
		ImageUsername: "",
		ImagePassword: "",
		Cmd:           "",
		Resources: apistructs.Resources{
			Cpu: 0.1,
			Mem: 512,
		},
		Env:                nil,
		Labels:             nil,
		DeploymentLabels:   nil,
		Selectors:          nil,
		Binds:              nil,
		Volumes:            nil,
		HealthCheck:        nil,
		NewHealthCheck:     nil,
		SideCars:           nil,
		InitContainer:      nil,
		InstanceInfos:      nil,
		WorkLoad:           "",
		ProjectServiceName: "",
		StatusDesc:         apistructs.StatusDesc{},
	}

	servicegroup := &apistructs.ServiceGroup{
		Dice: apistructs.Dice{
			ID:                   "test",
			Type:                 "service",
			Labels:               map[string]string{},
			Services:             []apistructs.Service{*service},
			ServiceDiscoveryKind: "",
			ServiceDiscoveryMode: "",
			ProjectNamespace:     "",
		},
	}

	k := &Kubernetes{
		secret: &secret.Secret{},
	}

	monkey.PatchInstanceMethod(reflect.TypeOf(k.secret), "Get", func(sec *secret.Secret, namespace, name string) (*apiv1.Secret, error) {
		return &apiv1.Secret{}, nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(k), "CopyErdaSecrets", func(kube *Kubernetes, originns, dstns string) ([]apiv1.Secret, error) {
		return []apiv1.Secret{}, nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(k), "AddPodMountVolume", func(kube *Kubernetes, service *apistructs.Service, podSpec *apiv1.PodSpec,
		secretvolmounts []apiv1.VolumeMount, secretvolumes []apiv1.Volume) error {
		return nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(k.ClusterInfo), "Get", func(clusterInfo *clusterinfo.ClusterInfo) (map[string]string, error) {
		return nil, nil
	})
	job, _ := k.newJob(service, servicegroup)
	assert.Equal(t, job.Spec.Template.Namespace, "")

}

func TestDeleteHistoryJob(t *testing.T) {
	k := &Kubernetes{
		job: &job.Job{},
	}
	monkey.PatchInstanceMethod(reflect.TypeOf(k.job), "List", func(job *job.Job, namespace string, labelSelector map[string]string) (batchv1.JobList, error) {
		return batchv1.JobList{
			TypeMeta: metav1.TypeMeta{},
			ListMeta: metav1.ListMeta{},
			Items: []batchv1.Job{batchv1.Job{
				TypeMeta: metav1.TypeMeta{},
				ObjectMeta: metav1.ObjectMeta{
					Name:      "test-job",
					Namespace: "test-ns",
				},
				Status: batchv1.JobStatus{
					Succeeded: 1,
				},
			}},
		}, nil
	})
	err := k.deleteHistoryJob("test-ns", "test-job")
	assert.Equal(t, err, nil)
}

func TestGetJobStatusFromMap(t *testing.T) {
	k := &Kubernetes{
		job: &job.Job{},
	}
	service := &apistructs.Service{
		Name:       "test-job",
		Namespace:  "test-ns",
		StatusDesc: apistructs.StatusDesc{},
	}
	monkey.PatchInstanceMethod(reflect.TypeOf(k.job), "List", func(job *job.Job, namespace string, labelSelector map[string]string) (batchv1.JobList, error) {
		return batchv1.JobList{
			TypeMeta: metav1.TypeMeta{},
			ListMeta: metav1.ListMeta{},
			Items: []batchv1.Job{batchv1.Job{
				TypeMeta: metav1.TypeMeta{},
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-job",
				},
				Status: batchv1.JobStatus{
					Succeeded: 1,
				},
			}},
		}, nil
	})
	status, _ := k.getJobStatusFromMap(service, "test-ns")
	assert.Equal(t, status.Status, apistructs.StatusReady)
}
