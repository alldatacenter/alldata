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

package frameworkext

import (
	"context"
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/kubernetes/pkg/scheduler/framework"
	"k8s.io/kubernetes/pkg/scheduler/framework/plugins/defaultbinder"
	"k8s.io/kubernetes/pkg/scheduler/framework/plugins/queuesort"
	schedulertesting "k8s.io/kubernetes/pkg/scheduler/testing"
)

var (
	_ PreFilterPhaseHook = &TestHook{}
	_ FilterPhaseHook    = &TestHook{}
	_ ScorePhaseHook     = &TestHook{}
)

type TestHook struct {
	index int
}

func (h *TestHook) Name() string { return "TestHook" }

func (h *TestHook) PreFilterHook(handle ExtendedHandle, state *framework.CycleState, pod *corev1.Pod) (*corev1.Pod, bool) {
	if pod.Annotations == nil {
		pod.Annotations = map[string]string{}
	}
	pod.Annotations[fmt.Sprintf("%d", h.index)] = fmt.Sprintf("%d", h.index)
	return pod, true
}

func (h *TestHook) FilterHook(handle ExtendedHandle, cycleState *framework.CycleState, pod *corev1.Pod, nodeInfo *framework.NodeInfo) (*corev1.Pod, *framework.NodeInfo, bool) {
	if pod.Annotations == nil {
		pod.Annotations = map[string]string{}
	}
	pod.Annotations[fmt.Sprintf("%d", h.index)] = fmt.Sprintf("%d", h.index)
	return pod, nodeInfo, true
}

func (h *TestHook) ScoreHook(handle ExtendedHandle, cycleState *framework.CycleState, pod *corev1.Pod, nodes []*corev1.Node) (*corev1.Pod, []*corev1.Node, bool) {
	if pod.Annotations == nil {
		pod.Annotations = map[string]string{}
	}
	pod.Annotations[fmt.Sprintf("%d", h.index)] = fmt.Sprintf("%d", h.index)
	return pod, nodes, true
}

func Test_frameworkExtenderImpl_RunPreFilterPlugins(t *testing.T) {
	type args struct {
		ctx        context.Context
		cycleState *framework.CycleState
		pod        *corev1.Pod
	}
	tests := []struct {
		name string
		args args
		want *framework.Status
	}{
		{
			name: "normal RunPreFilterPlugins",
			args: args{
				ctx:        context.TODO(),
				cycleState: framework.NewCycleState(),
				pod:        &corev1.Pod{},
			},
			want: nil,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			schedulingHooks := []SchedulingPhaseHook{
				&TestHook{index: 1},
				&TestHook{index: 2},
			}
			extendedHandle := NewExtendedHandle()
			extendedFrameworkFactory := NewFrameworkExtenderFactory(extendedHandle, schedulingHooks...)
			registeredPlugins := []schedulertesting.RegisterPluginFunc{
				schedulertesting.RegisterBindPlugin(defaultbinder.Name, defaultbinder.New),
				schedulertesting.RegisterQueueSortPlugin(queuesort.Name, queuesort.New),
			}
			fh, err := schedulertesting.NewFramework(
				registeredPlugins,
				"koord-scheduler",
			)
			assert.NoError(t, err)
			extendedFramework := extendedFrameworkFactory.New(fh)
			assert.Equal(t, tt.want, extendedFramework.RunPreFilterPlugins(tt.args.ctx, tt.args.cycleState, tt.args.pod))
			assert.Len(t, tt.args.pod.Annotations, 2)
			assert.Equal(t, "1", tt.args.pod.Annotations["1"])
			assert.Equal(t, "2", tt.args.pod.Annotations["2"])
		})
	}
}

func Test_frameworkExtenderImpl_RunFilterPluginsWithNominatedPods(t *testing.T) {
	type args struct {
		ctx        context.Context
		cycleState *framework.CycleState
		pod        *corev1.Pod
		nodeInfo   *framework.NodeInfo
	}
	tests := []struct {
		name string
		args args
		want *framework.Status
	}{
		{
			name: "normal RunFilterPluginsWithNominatedPods",
			args: args{
				ctx:        context.TODO(),
				cycleState: framework.NewCycleState(),
				pod:        &corev1.Pod{},
				nodeInfo:   framework.NewNodeInfo(),
			},
			want: nil,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			schedulingHooks := []SchedulingPhaseHook{
				&TestHook{index: 1},
				&TestHook{index: 2},
			}
			extendedHandle := NewExtendedHandle()
			extendedFrameworkFactory := NewFrameworkExtenderFactory(extendedHandle, schedulingHooks...)
			registeredPlugins := []schedulertesting.RegisterPluginFunc{
				schedulertesting.RegisterBindPlugin(defaultbinder.Name, defaultbinder.New),
				schedulertesting.RegisterQueueSortPlugin(queuesort.Name, queuesort.New),
			}
			fh, err := schedulertesting.NewFramework(
				registeredPlugins,
				"koord-scheduler",
			)
			assert.NoError(t, err)
			extendedFramework := extendedFrameworkFactory.New(fh)
			assert.Equal(t, tt.want, extendedFramework.RunFilterPluginsWithNominatedPods(tt.args.ctx, tt.args.cycleState, tt.args.pod, tt.args.nodeInfo))
			assert.Len(t, tt.args.pod.Annotations, 2)
			assert.Equal(t, "1", tt.args.pod.Annotations["1"])
			assert.Equal(t, "2", tt.args.pod.Annotations["2"])
		})
	}
}

func Test_frameworkExtenderImpl_RunScorePlugins(t *testing.T) {
	type fields struct {
		Framework      framework.Framework
		handle         ExtendedHandle
		preFilterHooks []PreFilterPhaseHook
		filterHooks    []FilterPhaseHook
		scoreHooks     []ScorePhaseHook
	}
	type args struct {
		ctx   context.Context
		state *framework.CycleState
		pod   *corev1.Pod
		nodes []*corev1.Node
	}
	tests := []struct {
		name       string
		fields     fields
		args       args
		wantScore  framework.PluginToNodeScores
		wantStatus *framework.Status
	}{
		{
			name: "normal RunScorePlugins",
			args: args{
				ctx:   context.TODO(),
				state: framework.NewCycleState(),
				pod:   &corev1.Pod{},
				nodes: []*corev1.Node{{}},
			},
			wantScore:  framework.PluginToNodeScores{},
			wantStatus: nil,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			schedulingHooks := []SchedulingPhaseHook{
				&TestHook{index: 1},
				&TestHook{index: 2},
			}
			extendedHandle := NewExtendedHandle()
			extendedFrameworkFactory := NewFrameworkExtenderFactory(extendedHandle, schedulingHooks...)
			registeredPlugins := []schedulertesting.RegisterPluginFunc{
				schedulertesting.RegisterBindPlugin(defaultbinder.Name, defaultbinder.New),
				schedulertesting.RegisterQueueSortPlugin(queuesort.Name, queuesort.New),
			}
			fh, err := schedulertesting.NewFramework(
				registeredPlugins,
				"koord-scheduler",
			)
			assert.NoError(t, err)
			extendedFramework := extendedFrameworkFactory.New(fh)
			score, status := extendedFramework.RunScorePlugins(tt.args.ctx, tt.args.state, tt.args.pod, tt.args.nodes)
			assert.Equalf(t, tt.wantScore, score, "RunScorePlugins(%v, %v, %v, %v)", tt.args.ctx, tt.args.state, tt.args.pod, tt.args.nodes)
			assert.Equalf(t, tt.wantStatus, status, "RunScorePlugins(%v, %v, %v, %v)", tt.args.ctx, tt.args.state, tt.args.pod, tt.args.nodes)
			assert.Len(t, tt.args.pod.Annotations, 2)
			assert.Equal(t, "1", tt.args.pod.Annotations["1"])
			assert.Equal(t, "2", tt.args.pod.Annotations["2"])
		})
	}
}
