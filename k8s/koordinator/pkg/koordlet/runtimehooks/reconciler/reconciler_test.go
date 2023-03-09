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

package reconciler

import (
	"strings"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/protocol"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
	mock_statesinformer "github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer/mockstatesinformer"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func Test_doKubeQOSCgroup(t *testing.T) {
	type args struct {
		resource     system.Resource
		targetOutput map[corev1.PodQOSClass]string
	}
	type wants struct {
		kubeQOSVal map[corev1.PodQOSClass]string
	}
	type gots struct {
		kubeQOSVal map[corev1.PodQOSClass]string
	}
	tests := []struct {
		name  string
		args  args
		gots  gots
		wants wants
	}{
		{
			name: "exec kube qos level function",
			args: args{
				resource: system.CPUBVTWarpNs,
				targetOutput: map[corev1.PodQOSClass]string{
					corev1.PodQOSGuaranteed: "test-guaranteed",
					corev1.PodQOSBurstable:  "test-burstable",
					corev1.PodQOSBestEffort: "test-besteffort",
				},
			},
			gots: gots{
				kubeQOSVal: map[corev1.PodQOSClass]string{},
			},
			wants: wants{
				kubeQOSVal: map[corev1.PodQOSClass]string{
					corev1.PodQOSGuaranteed: "test-guaranteed",
					corev1.PodQOSBurstable:  "test-burstable",
					corev1.PodQOSBestEffort: "test-besteffort",
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			reconcilerFn := func(proto protocol.HooksProtocol) error {
				kubeQOSCtx := proto.(*protocol.KubeQOSContext)
				kubeQOS := kubeQOSCtx.Request.KubeQOSClass
				tt.gots.kubeQOSVal[kubeQOS] = tt.args.targetOutput[kubeQOS]
				return nil
			}
			RegisterCgroupReconciler(KubeQOSLevel, tt.args.resource, tt.name, reconcilerFn, NoneFilter())
			doKubeQOSCgroup()
			assert.Equal(t, tt.wants.kubeQOSVal, tt.gots.kubeQOSVal, "kube qos map value should be equal")
		})
	}
}

func Test_reconciler_reconcilePodCgroup(t *testing.T) {
	stopCh := make(chan struct{}, 1)
	tryStopFn := func() {
		select {
		case stopCh <- struct{}{}:
		default:
		}
	}

	genPodKey := func(ns, name string) string {
		return strings.Join([]string{ns, name}, "/")
	}
	genContainerKey := func(ns, podName, containerName string) string {
		return strings.Join([]string{ns, podName, containerName}, "/")
	}
	podLevelOutput := map[string]string{}
	containerLevelOutput := map[string]string{}

	podReconcilerFn := func(proto protocol.HooksProtocol) error {
		podCtx := proto.(*protocol.PodContext)
		podKey := genPodKey(podCtx.Request.PodMeta.Namespace, podCtx.Request.PodMeta.Name)
		podLevelOutput[podKey] = podCtx.Request.PodMeta.UID
		tryStopFn()
		return nil
	}
	containerReconcilerFn := func(proto protocol.HooksProtocol) error {
		containerCtx := proto.(*protocol.ContainerContext)
		containerKey := genContainerKey(containerCtx.Request.PodMeta.Namespace, containerCtx.Request.PodMeta.Name,
			containerCtx.Request.ContainerMeta.Name)
		containerLevelOutput[containerKey] = containerCtx.Request.ContainerMeta.ID
		tryStopFn()
		return nil
	}
	RegisterCgroupReconciler(PodLevel, system.CPUBVTWarpNs, "get pod uid", podReconcilerFn, NoneFilter())
	RegisterCgroupReconciler(ContainerLevel, system.CPUBVTWarpNs, "get container uid", containerReconcilerFn, NoneFilter())

	type fields struct {
		podsMeta *statesinformer.PodMeta
	}
	type wants struct {
		wantPods       map[string]string
		wantContainers map[string]string
	}

	test := struct {
		name   string
		fields fields
		wants  wants
	}{

		name: "reconcile pod cgroup to get uid",
		fields: fields{
			podsMeta: &statesinformer.PodMeta{
				Pod: &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Namespace: "test-ns",
						Name:      "test-pod-name",
						UID:       "test-pod-uid",
					},
					Spec: corev1.PodSpec{
						Containers: []corev1.Container{
							{
								Name: "test-container-name",
							},
						},
					},
					Status: corev1.PodStatus{
						ContainerStatuses: []corev1.ContainerStatus{
							{
								Name:        "test-container-name",
								ContainerID: "test-container-id",
							},
						},
					},
				},
			},
		},
		wants: wants{
			wantPods: map[string]string{
				genPodKey("test-ns", "test-pod-name"): "test-pod-uid",
			},
			wantContainers: map[string]string{
				genContainerKey("test-ns", "test-pod-name", "test-container-name"): "test-container-id",
			},
		},
	}
	t.Run(test.name, func(t *testing.T) {
		c := &reconciler{
			podsMeta:   []*statesinformer.PodMeta{test.fields.podsMeta},
			podUpdated: make(chan struct{}, 1),
		}
		c.podUpdated <- struct{}{}
		c.reconcilePodCgroup(stopCh)
		assert.Equal(t, test.wants.wantPods, podLevelOutput, "pod reconciler should be equal")
		assert.Equal(t, test.wants.wantContainers, containerLevelOutput, "container reconciler should be equal")
	})
}

func Test_reconciler_podRefreshCallback(t *testing.T) {
	type args struct {
		podsMeta []*statesinformer.PodMeta
	}
	tests := []struct {
		name string
		args args
	}{
		{
			name: "callback refresh pod meta",
			args: args{
				podsMeta: []*statesinformer.PodMeta{
					{
						Pod: &corev1.Pod{
							ObjectMeta: metav1.ObjectMeta{
								Namespace: "test-ns",
								Name:      "test-name",
							},
						},
						CgroupDir: "test-dir",
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			c := &reconciler{
				podUpdated: make(chan struct{}, 1),
			}
			c.podRefreshCallback(statesinformer.RegisterTypeAllPods, nil, tt.args.podsMeta)
			assert.Equal(t, c.podsMeta, tt.args.podsMeta, "callback update pod meta")
		})
	}
}

func TestNewReconciler(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	si := mock_statesinformer.NewMockStatesInformer(ctrl)
	si.EXPECT().RegisterCallbacks(statesinformer.RegisterTypeAllPods, gomock.Any(), gomock.Any(), gomock.Any())
	r := NewReconciler(si)
	nr := r.(*reconciler)
	stopCh := make(chan struct{}, 1)
	stopCh <- struct{}{}
	nr.reconcilePodCgroup(stopCh)
	stopCh <- struct{}{}
	err := r.Run(stopCh)
	assert.NoError(t, err, "run reconciler without error")
}
