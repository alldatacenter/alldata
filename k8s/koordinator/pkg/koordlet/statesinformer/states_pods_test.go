/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package statesinformer

import (
	"errors"
	"net"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"go.uber.org/atomic"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/clientcmd"
	kubeletconfiginternal "k8s.io/kubernetes/pkg/kubelet/apis/config"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metrics"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func Test_genPodCgroupParentDirWithCgroupfsDriver(t *testing.T) {
	system.SetupCgroupPathFormatter(system.Cgroupfs)
	defer system.SetupCgroupPathFormatter(system.Systemd)
	tests := []struct {
		name string
		args *corev1.Pod
		want string
	}{
		{
			name: "Guaranteed",
			args: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID: "111-222-333",
				},
				Status: corev1.PodStatus{
					QOSClass: corev1.PodQOSGuaranteed,
				},
			},
			want: "/pod111-222-333",
		},
		{
			name: "BestEffort",
			args: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID: "111-222-333",
				},
				Status: corev1.PodStatus{
					QOSClass: corev1.PodQOSBestEffort,
				},
			},
			want: "/besteffort/pod111-222-333",
		},
		{
			name: "Burstable",
			args: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID: "111-222-333",
				},
				Status: corev1.PodStatus{
					QOSClass: corev1.PodQOSBurstable,
				},
			},
			want: "/burstable/pod111-222-333",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := filepath.Join("/", genPodCgroupParentDir(tt.args))
			if tt.want != got {
				t.Errorf("genPodCgroupParentDir want %v but got %v", tt.want, got)
			}
		})
	}
}

type testKubeletStub struct {
	pods   corev1.PodList
	config *kubeletconfiginternal.KubeletConfiguration
}

func (t *testKubeletStub) GetAllPods() (corev1.PodList, error) {
	return t.pods, nil
}

func (t *testKubeletStub) GetKubeletConfiguration() (*kubeletconfiginternal.KubeletConfiguration, error) {
	return t.config, nil
}

type testErrorKubeletStub struct {
}

func (t *testErrorKubeletStub) GetAllPods() (corev1.PodList, error) {
	return corev1.PodList{}, errors.New("test error")
}

func (t *testErrorKubeletStub) GetKubeletConfiguration() (*kubeletconfiginternal.KubeletConfiguration, error) {
	return nil, errors.New("test error")
}

func Test_statesInformer_syncPods(t *testing.T) {
	stopCh := make(chan struct{}, 1)
	defer close(stopCh)
	testingNode := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name:   "test",
			Labels: map[string]string{},
		},
	}
	c := NewDefaultConfig()
	c.KubeletSyncInterval = 60 * time.Second
	m := &podsInformer{
		nodeInformer: &nodeInformer{
			node: testingNode,
		},
		kubelet: &testKubeletStub{pods: corev1.PodList{
			Items: []corev1.Pod{
				{},
			},
		}},
		podHasSynced:   atomic.NewBool(false),
		callbackRunner: NewCallbackRunner(),
	}

	err := m.syncPods()
	assert.NoError(t, err)
	if len(m.GetAllPods()) != 1 {
		t.Fatal("failed to update pods")
	}

	m.kubelet = &testErrorKubeletStub{}

	err = m.syncPods()
	assert.Error(t, err)
}

func Test_newKubeletStub(t *testing.T) {
	testingNode := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name:   "test",
			Labels: map[string]string{},
		},
		Status: corev1.NodeStatus{
			DaemonEndpoints: corev1.NodeDaemonEndpoints{
				KubeletEndpoint: corev1.DaemonEndpoint{
					Port: 10250,
				},
			},
			Addresses: []corev1.NodeAddress{
				{Type: corev1.NodeInternalIP, Address: "127.0.0.1"},
			},
		},
	}

	dir := t.TempDir()
	cfg := &rest.Config{
		Host:        net.JoinHostPort("127.0.0.1", "10250"),
		BearerToken: token,
		TLSClientConfig: rest.TLSClientConfig{
			Insecure: true,
		},
	}
	setConfigs(t, dir)

	kubeStub, _ := NewKubeletStub("127.0.0.1", 10250, "https", 10, cfg)
	type args struct {
		node *corev1.Node
		cfg  *Config
	}
	tests := []struct {
		name    string
		args    args
		want    KubeletStub
		wantErr bool
	}{
		{
			name: "NodeInternalIP",
			args: args{
				node: testingNode,
				cfg: &Config{
					KubeletPreferredAddressType: string(corev1.NodeInternalIP),
					KubeletSyncTimeout:          10 * time.Second,
					InsecureKubeletTLS:          true,
					KubeletReadOnlyPort:         10250,
				},
			},
			want:    kubeStub,
			wantErr: false,
		},
		{
			name: "Empty IP",
			args: args{
				node: testingNode,
				cfg: &Config{
					KubeletPreferredAddressType: "",
					KubeletSyncTimeout:          10 * time.Second,
					InsecureKubeletTLS:          true,
					KubeletReadOnlyPort:         10250,
				},
			},
			want:    kubeStub,
			wantErr: false,
		},
		{
			name: "HTTPS",
			args: args{
				node: testingNode,
				cfg: &Config{
					KubeletPreferredAddressType: "",
					KubeletSyncTimeout:          10 * time.Second,
					InsecureKubeletTLS:          false,
					KubeletReadOnlyPort:         10250,
				},
			},
			want:    nil,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := newKubeletStubFromConfig(tt.args.node, tt.args.cfg)
			if (err != nil) != tt.wantErr {
				t.Errorf("newKubeletStub() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if tt.wantErr && got != nil {
				t.Errorf("newKubeletStub() = %v, want %v", got, tt.want)
			}
		})
	}
}

func setConfigs(t *testing.T, dir string) {
	// Set KUBECONFIG env value
	kubeconfigEnvPath := filepath.Join(dir, "kubeconfig-text-context")
	err := os.WriteFile(kubeconfigEnvPath, []byte(genKubeconfig("from-env")), 0644)
	assert.NoError(t, err)
	t.Setenv(clientcmd.RecommendedConfigPathEnvVar, kubeconfigEnvPath)
}

func genKubeconfig(contexts ...string) string {
	var sb strings.Builder
	sb.WriteString("---\napiVersion: v1\nkind: Config\nclusters:\n")
	for _, ctx := range contexts {
		sb.WriteString("- cluster:\n    server: " + ctx + "\n  name: " + ctx + "\n")
	}
	sb.WriteString("contexts:\n")
	for _, ctx := range contexts {
		sb.WriteString("- context:\n    cluster: " + ctx + "\n    user: " + ctx + "\n  name: " + ctx + "\n")
	}

	sb.WriteString("users:\n")
	for _, ctx := range contexts {
		sb.WriteString("- name: " + ctx + "\n")
	}
	sb.WriteString("preferences: {}\n")
	if len(contexts) > 0 {
		sb.WriteString("current-context: " + contexts[0] + "\n")
	}
	return sb.String()
}

func Test_statesInformer_syncKubeletLoop(t *testing.T) {
	stopCh := make(chan struct{}, 1)

	c := NewDefaultConfig()
	c.KubeletSyncInterval = 3 * time.Second

	m := &podsInformer{
		kubelet: &testKubeletStub{pods: corev1.PodList{
			Items: []corev1.Pod{
				{},
			},
		}},
		callbackRunner: NewCallbackRunner(),
		podHasSynced:   atomic.NewBool(false),
	}
	go m.syncKubeletLoop(c.KubeletSyncInterval, stopCh)
	time.Sleep(5 * time.Second)
	close(stopCh)
}

func Test_resetPodMetrics(t *testing.T) {
	testingNode := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name:   "test-node",
			Labels: map[string]string{},
		},
		Status: corev1.NodeStatus{
			Allocatable: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse("100"),
				corev1.ResourceMemory: resource.MustParse("200Gi"),
				apiext.BatchCPU:       resource.MustParse("50000"),
				apiext.BatchMemory:    resource.MustParse("80Gi"),
			},
			Capacity: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse("100"),
				corev1.ResourceMemory: resource.MustParse("200Gi"),
				apiext.BatchCPU:       resource.MustParse("50000"),
				apiext.BatchMemory:    resource.MustParse("80Gi"),
			},
		},
	}
	assert.NotPanics(t, func() {
		metrics.Register(testingNode)
		defer metrics.Register(nil)

		resetPodMetrics()
	})
}

func Test_recordPodResourceMetrics(t *testing.T) {
	testingNode := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name:   "test-node",
			Labels: map[string]string{},
		},
		Status: corev1.NodeStatus{
			Allocatable: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse("100"),
				corev1.ResourceMemory: resource.MustParse("200Gi"),
				apiext.BatchCPU:       resource.MustParse("50000"),
				apiext.BatchMemory:    resource.MustParse("80Gi"),
			},
			Capacity: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse("100"),
				corev1.ResourceMemory: resource.MustParse("200Gi"),
				apiext.BatchCPU:       resource.MustParse("50000"),
				apiext.BatchMemory:    resource.MustParse("80Gi"),
			},
		},
	}
	testingPod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "test_pod",
			Namespace: "test_pod_namespace",
			UID:       "test01",
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Name: "test_container",
					Resources: corev1.ResourceRequirements{
						Requests: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("1"),
							corev1.ResourceMemory: resource.MustParse("2Gi"),
						},
						Limits: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("2"),
							corev1.ResourceMemory: resource.MustParse("4Gi"),
						},
					},
				},
			},
		},
		Status: corev1.PodStatus{
			ContainerStatuses: []corev1.ContainerStatus{
				{
					Name:        "test_container",
					ContainerID: "containerd://testxxx",
				},
			},
		},
	}
	testingBatchPod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "test_batch_pod",
			Namespace: "test_batch_pod_namespace",
			UID:       "batch01",
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Name: "test_batch_container",
					Resources: corev1.ResourceRequirements{
						Requests: corev1.ResourceList{
							apiext.BatchCPU:    resource.MustParse("1000"),
							apiext.BatchMemory: resource.MustParse("2Gi"),
						},
						Limits: corev1.ResourceList{
							apiext.BatchCPU:    resource.MustParse("1000"),
							apiext.BatchMemory: resource.MustParse("2Gi"),
						},
					},
				},
			},
		},
		Status: corev1.PodStatus{
			ContainerStatuses: []corev1.ContainerStatus{
				{
					Name:        "test_batch_container",
					ContainerID: "containerd://batchxxx",
				},
			},
		},
	}
	tests := []struct {
		name string
		arg  *PodMeta
	}{
		{
			name: "pod meta is invalid",
			arg:  nil,
		},
		{
			name: "pod meta is invalid 1",
			arg:  &PodMeta{},
		},
		{
			name: "record a normally pod",
			arg: &PodMeta{
				Pod: testingPod,
			},
		},
		{
			name: "record a batch pod",
			arg: &PodMeta{
				Pod: testingBatchPod,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			metrics.Register(testingNode)
			defer metrics.Register(nil)

			recordPodResourceMetrics(tt.arg)
		})
	}
}
