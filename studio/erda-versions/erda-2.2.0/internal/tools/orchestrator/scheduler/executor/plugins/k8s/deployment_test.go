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
	appsv1 "k8s.io/api/apps/v1"
	apiv1 "k8s.io/api/core/v1"
	storagev1 "k8s.io/api/storage/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/clusterinfo"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/persistentvolumeclaim"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/secret"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/storageclass"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

func TestNewDeployment(t *testing.T) {
	service := &apistructs.Service{
		Name:          "test-service",
		Namespace:     "test",
		Image:         "test",
		ImageUsername: "",
		ImagePassword: "",
		Cmd:           "",
		Ports:         nil,
		ProxyPorts:    nil,
		Vip:           "",
		ShortVIP:      "",
		ProxyIp:       "",
		PublicIp:      "",
		Scale:         0,
		Resources: apistructs.Resources{
			Cpu: 0.1,
			Mem: 512,
		},
		Depends:            nil,
		Env:                nil,
		Labels:             nil,
		DeploymentLabels:   nil,
		Selectors:          nil,
		Binds:              nil,
		Volumes:            nil,
		Hosts:              nil,
		HealthCheck:        nil,
		NewHealthCheck:     nil,
		SideCars:           nil,
		InitContainer:      nil,
		InstanceInfos:      nil,
		MeshEnable:         nil,
		TrafficSecurity:    diceyml.TrafficSecurity{},
		WorkLoad:           "",
		ProjectServiceName: "",
		K8SSnippet:         nil,
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
	deploy, err := k.newDeployment(service, servicegroup)
	assert.Equal(t, err, nil)
	k.setDeploymentZeroReplica(deploy)
	assert.Equal(t, *deploy.Spec.Replicas, int32(0))
}

func TestUpdateContainerResourceEnv(t *testing.T) {
	k := Kubernetes{}

	container := apiv1.Container{
		Name:       "",
		Image:      "",
		Command:    nil,
		Args:       nil,
		WorkingDir: "",
		Ports:      nil,
		EnvFrom:    nil,
		Env: []apiv1.EnvVar{
			{
				Name:  "DICE_CPU_ORIGIN",
				Value: "0.100000",
			},
			{
				Name:  "DICE_CPU_REQUEST",
				Value: "0.010",
			},
			{
				Name:  "DICE_CPU_LIMIT",
				Value: "0.100",
			},
			{
				Name:  "DICE_MEM_ORIGIN",
				Value: "1024.000000",
			},
			{
				Name:  "DICE_MEM_REQUEST",
				Value: "1024",
			},
			{
				Name:  "DICE_MEM_LIMIT",
				Value: "1024",
			},
		},
		Resources: apiv1.ResourceRequirements{
			Limits: apiv1.ResourceList{
				"cpu":    resource.MustParse("100m"),
				"memory": resource.MustParse("1024Mi"),
			},
			Requests: apiv1.ResourceList{
				"cpu":    resource.MustParse("10m"),
				"memory": resource.MustParse("1024Mi"),
			},
		},
	}
	originResource := apistructs.Resources{Cpu: 0.1, Mem: 1024}
	k.UpdateContainerResourceEnv(originResource, &container)
	assert.Equal(t, container.Env[0].Value, "0.100000")
	assert.Equal(t, container.Env[5].Value, "1024")
}

func TestSetPodAnnotationsBaseContainerEnvs(t *testing.T) {
	type args struct {
		container      apiv1.Container
		podAnnotations map[string]string
	}
	tests := []struct {
		name string
		args args
	}{
		// TODO: Add test cases.
		{
			name: "test_01",
			args: args{
				container: apiv1.Container{
					Env: []apiv1.EnvVar{
						{
							Name:  "DICE_ORG_NAME",
							Value: "xxx",
						},
						{
							Name:  "DICE_CLUSTER_NAME",
							Value: "xxx",
						},
						{
							Name:  "DICE_PROJECT_NAME",
							Value: "xxx",
						},
						{
							Name:  "DICE_SERVICE_NAME",
							Value: "xxx",
						},
						{
							Name:  "DICE_APPLICATION_NAME",
							Value: "xxx",
						},
						{
							Name:  "DICE_WORKSPACE",
							Value: "xxx",
						},
						{
							Name:  "DICE_RUNTIME_NAME",
							Value: "xxx",
						},
						{
							Name:  "DICE_RUNTIME_ID",
							Value: "xxx",
						},
						{
							Name:  "DICE_APPLICATION_ID",
							Value: "xxx",
						},
						{
							Name:  "DICE_ORG_ID",
							Value: "xxx",
						},
						{
							Name:  "DICE_PROJECT_ID",
							Value: "xxx",
						},
						{
							Name:  "DICE_DEPLOYMENT_ID",
							Value: "xxx",
						},
						{
							Name:  "TERMINUS_LOG_KEY",
							Value: "xxx",
						},
						{
							Name:  "MONITOR_LOG_KEY",
							Value: "xxx",
						},
						{
							Name:  "MSP_ENV_ID",
							Value: "xxx",
						},
						{
							Name:  "MSP_LOG_ATTACH",
							Value: "xxx",
						},
						{
							Name:  "MONITOR_LOG_COLLECTOR",
							Value: "xxx",
						},
						{
							Name:  "TERMINUS_KEY",
							Value: "xxx",
						},
					},
				},
				podAnnotations: make(map[string]string),
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
		})
	}
}

func TestDereferenceEnvs(t *testing.T) {
	var d = new(appsv1.Deployment)
	d.Spec.Template.Spec.Containers = []apiv1.Container{{
		Env: []apiv1.EnvVar{
			{Name: "ENV_A", Value: "homework"},
			{Name: "ENV_B", Value: "do ${env.ENV_A}"},
			{Name: "COOKIE_DOMAIN", Value: "${domain-prefix}${env.DICE_ROOT_DOMAIN}"},
			{Name: "REPORT_DB_PSWD", Value: "${env.MYSQL_PASSWORD}"},
			{Name: "REPORT_DB_USER", Value: "${env.MYSQL_USERNAME}"},
			{Name: "DICE_ROOT_DOMAIN", Value: "erda.cloud"},
			{Name: "MYSQL_PASSWORD", Value: "1234"},
			{Name: "MYSQL_USERNAME", Value: "root"},
			{Name: "MYSQL_HOST", Value: "localhost"},
			{Name: "MYSQL_PORT", Value: "${env.port: 3306}"},
		}},
	}
	if err := DereferenceEnvs(&d.Spec.Template); err != nil {
		t.Fatal(err)
	}
	for _, env := range d.Spec.Template.Spec.Containers[0].Env {
		t.Logf("Name: %s, Value: %s", env.Name, env.Value)
	}
}

func BenchmarkName(b *testing.B) {
	for i := 0; i < b.N; i++ {
		var d = new(appsv1.Deployment)
		d.Spec.Template.Spec.Containers = []apiv1.Container{{
			Env: []apiv1.EnvVar{
				{Name: "ENV_A", Value: "homework"},
				{Name: "ENV_B", Value: "do ${env.ENV_A}"},
				{Name: "COOKIE_DOMAIN", Value: "${env.DICE_ROOT_DOMAIN}"},
				{Name: "REPORT_DB_PSWD", Value: "${env.MYSQL_PASSWORD}"},
				{Name: "REPORT_DB_USER", Value: "${env.MYSQL_USERNAME}"},
				{Name: "DICE_ROOT_DOMAIN", Value: "erda.cloud"},
				{Name: "MYSQL_PASSWORD", Value: "1234"},
				{Name: "MYSQL_USERNAME", Value: "root"},
				{Name: "MYSQL_HOST", Value: "localhost"},
				{Name: "MYSQL_PORT", Value: "3306"},
			}},
		}
		if err := DereferenceEnvs(&d.Spec.Template); err != nil {
			b.Fatal(err)
		}
		for _, env := range d.Spec.Template.Spec.Containers[0].Env {
			b.Logf("Name: %s, Value: %s", env.Name, env.Value)
		}
	}
}

func Test_inheritDeploymentLabels1(t *testing.T) {
	type args struct {
		service    *apistructs.Service
		deployment *appsv1.Deployment
	}

	labels := map[string]string{
		"app_kind":             "deployment",
		"alibabacloud.com/eci": "true",
	}

	deploymentlabels := map[string]string{
		"app_kind":             "deployment",
		"alibabacloud.com/eci": "true",
		"platform":             "erda",
	}

	binds01 := []apistructs.ServiceBind{
		{
			Bind: apistructs.Bind{
				HostPath:      "/mnt/test",
				ContainerPath: "/mnt/test",
			},
		},
	}

	binds02 := []apistructs.ServiceBind{
		{
			Bind: apistructs.Bind{
				ContainerPath: "/mnt/test",
			},
		},
	}

	service01 := &apistructs.Service{
		Binds:            binds01,
		Labels:           labels,
		DeploymentLabels: deploymentlabels,
		Volumes: []apistructs.Volume{
			{
				SCVolume: apistructs.SCVolume{
					TargetPath: "/opt/data",
				},
			},
		},
	}

	service02 := &apistructs.Service{
		Binds:            binds02,
		Labels:           labels,
		DeploymentLabels: deploymentlabels,
		Volumes: []apistructs.Volume{
			{
				SCVolume: apistructs.SCVolume{
					TargetPath: "/opt/data",
				},
			},
		},
	}

	dp := &appsv1.Deployment{
		TypeMeta: metav1.TypeMeta{
			Kind:       "Deployment",
			APIVersion: "apps/v1",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      "test",
			Namespace: "default",
			Labels:    make(map[string]string),
		},
		Spec: appsv1.DeploymentSpec{
			RevisionHistoryLimit: func(i int32) *int32 { return &i }(int32(3)),
			Replicas:             func(i int32) *int32 { return &i }(int32(2)),
			Template: apiv1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name:   "test",
					Labels: make(map[string]string),
				},
			},
			Selector: &metav1.LabelSelector{
				MatchLabels: map[string]string{"app": "testdeployment"},
			},
		},
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "case_01",
			args: args{
				service:    service01,
				deployment: dp,
			},
			wantErr: true,
		},
		{
			name: "case_02",
			args: args{
				service:    service02,
				deployment: dp,
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := inheritDeploymentLabels(tt.args.service, tt.args.deployment); (err != nil) != tt.wantErr {
				t.Errorf("inheritDeploymentLabels() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func Test_setPodAnnotationsFromLabels(t *testing.T) {
	type args struct {
		service        *apistructs.Service
		podannotations map[string]string
	}

	labels := map[string]string{
		"app_kind":             "deployment",
		"alibabacloud.com/eci": "true",
	}

	deploymentlabels := map[string]string{
		"app_kind":             "deployment",
		"alibabacloud.com/eci": "true",
		"platform":             "erda",
	}

	service01 := &apistructs.Service{
		Labels:           labels,
		DeploymentLabels: deploymentlabels,
	}

	anns := map[string]string{
		"eci_enabled": "true",
	}

	tests := []struct {
		name string
		args args
	}{
		{
			name: "case_01",
			args: args{
				service:        service01,
				podannotations: anns,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
		})
	}
}

func TestKubernetes_setStatelessServiceVolumes(t *testing.T) {
	service := &apistructs.Service{
		Name:          "test-service",
		Namespace:     "test",
		Image:         "test",
		ImageUsername: "",
		ImagePassword: "",
		Cmd:           "",
		Ports:         nil,
		ProxyPorts:    nil,
		Vip:           "",
		ShortVIP:      "",
		ProxyIp:       "",
		PublicIp:      "",
		Scale:         0,
		Resources: apistructs.Resources{
			Cpu: 0.1,
			Mem: 512,
		},
		Depends:            nil,
		Env:                nil,
		Labels:             nil,
		DeploymentLabels:   nil,
		Selectors:          nil,
		Binds:              nil,
		Volumes:            nil,
		Hosts:              nil,
		HealthCheck:        nil,
		NewHealthCheck:     nil,
		SideCars:           nil,
		InitContainer:      nil,
		InstanceInfos:      nil,
		MeshEnable:         nil,
		TrafficSecurity:    diceyml.TrafficSecurity{},
		WorkLoad:           "",
		ProjectServiceName: "",
		K8SSnippet:         nil,
		StatusDesc:         apistructs.StatusDesc{},
	}

	service.Volumes = make([]apistructs.Volume, 0)
	service.Volumes = append(service.Volumes, apistructs.Volume{
		ContainerPath: "/opt/data/xxx01",
		SCVolume: apistructs.SCVolume{
			//SourcePath: "",
			Snapshot: &apistructs.VolumeSnapshot{
				MaxHistory: int32(2),
			},
		},
	})

	service.Volumes = append(service.Volumes, apistructs.Volume{
		ContainerPath: "/opt/data/xxx02",
		SCVolume:      apistructs.SCVolume{
			//SourcePath: "/opt/test",
		},
	})

	service.Volumes = append(service.Volumes, apistructs.Volume{
		ContainerPath: "/opt/data/xxx03",
		SCVolume:      apistructs.SCVolume{
			//SourcePath: "test/data",
		},
	})

	podSpec := &apiv1.PodSpec{}
	podSpec.Volumes = make([]apiv1.Volume, 0)
	podSpec.Volumes = append(podSpec.Volumes,
		apiv1.Volume{
			Name: "vol1",
			VolumeSource: apiv1.VolumeSource{
				HostPath: &apiv1.HostPathVolumeSource{
					Path: "/data/xxx",
				},
			},
		})

	podSpec.Containers = make([]apiv1.Container, 0)
	vol01 := apiv1.VolumeMount{
		Name:      "vol1",
		MountPath: "/opt/xxx",
		ReadOnly:  false,
	}

	envs := make([]apiv1.EnvVar, 0)
	envs = append(envs, apiv1.EnvVar{
		Name:  "DICE_RUNTIME_NAME",
		Value: "feature/develop",
	})

	envs = append(envs, apiv1.EnvVar{
		Name:  "DICE_WORKSPACE",
		Value: "dev",
	})

	envs = append(envs, apiv1.EnvVar{
		Name:  "DICE_APPLICATION_ID",
		Value: "1",
	})

	podSpec.Containers = append(podSpec.Containers, apiv1.Container{
		Name:         "vol1",
		VolumeMounts: make([]apiv1.VolumeMount, 0),
		Env:          envs,
	})

	podSpec.Containers[0].VolumeMounts = append(podSpec.Containers[0].VolumeMounts, vol01)

	k := &Kubernetes{
		secret:       &secret.Secret{},
		pvc:          &persistentvolumeclaim.PersistentVolumeClaim{},
		storageClass: &storageclass.StorageClass{},
	}

	monkey.PatchInstanceMethod(reflect.TypeOf(k.pvc), "CreateIfNotExists", func(pvcl *persistentvolumeclaim.PersistentVolumeClaim, pvc *apiv1.PersistentVolumeClaim) error {
		return nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(k.storageClass), "Get", func(sc *storageclass.StorageClass, name string) (*storagev1.StorageClass, error) {
		return &storagev1.StorageClass{}, nil
	})

	err := k.setStatelessServiceVolumes(service, podSpec)
	assert.Equal(t, err, nil)
}

/*
func TestGenerateECIPodSidecarContainers(t *testing.T) {
	wantContainer := apiv1.Container{
		Name: "fluent-bit",
		//Image: sidecar.Image,
		Resources: apiv1.ResourceRequirements{
			Requests: apiv1.ResourceList{
				apiv1.ResourceCPU:    resource.MustParse("0.1"),
				apiv1.ResourceMemory: resource.MustParse("256Mi"),
			},
			Limits: apiv1.ResourceList{
				apiv1.ResourceCPU:    resource.MustParse("1"),
				apiv1.ResourceMemory: resource.MustParse("1Gi"),
			},
		},
		Command:      []string{"./fluent-bit/bin/fluent-bit"},
		Args:         []string{"-c", "/fluent-bit/etc/sidecar/fluent-bit.conf"},
		VolumeMounts: []apiv1.VolumeMount{},
	}
	tests := []struct {
		name    string
		want    apiv1.Container
		wantErr bool
	}{
		// TODO: Add test cases.
		{
			name:    "Test_01",
			want:    wantContainer,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := GenerateECIPodSidecarContainers(false)
			if (err != nil) != tt.wantErr {
				t.Errorf("GenerateECIPodSidecarContainers() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
*/

func TestGenerateECIPodSidecarContainers(t *testing.T) {
	type args struct {
		inEdge bool
	}

	wantContainer := apiv1.Container{
		Name: "fluent-bit",
		//Image: sidecar.Image,
		Resources: apiv1.ResourceRequirements{
			Requests: apiv1.ResourceList{
				apiv1.ResourceCPU:    resource.MustParse("0.1"),
				apiv1.ResourceMemory: resource.MustParse("256Mi"),
			},
			Limits: apiv1.ResourceList{
				apiv1.ResourceCPU:    resource.MustParse("1"),
				apiv1.ResourceMemory: resource.MustParse("1Gi"),
			},
		},
		Command:      []string{"./fluent-bit/bin/fluent-bit"},
		Args:         []string{"-c", "/fluent-bit/etc/sidecar/fluent-bit.conf"},
		VolumeMounts: []apiv1.VolumeMount{},
	}

	tests := []struct {
		name    string
		args    args
		want    apiv1.Container
		wantErr bool
	}{
		{
			name:    "Test_01",
			args:    args{inEdge: false},
			want:    wantContainer,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := GenerateECIPodSidecarContainers(tt.args.inEdge)
			if (err != nil) != tt.wantErr {
				t.Errorf("GenerateECIPodSidecarContainers() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}
