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

package compatibledefaultpreemption

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/client-go/informers"
	kubefake "k8s.io/client-go/kubernetes/fake"
	scheduledconfig "k8s.io/kubernetes/pkg/scheduler/apis/config"
	"k8s.io/kubernetes/pkg/scheduler/framework/plugins/defaultbinder"
	"k8s.io/kubernetes/pkg/scheduler/framework/plugins/defaultpreemption"
	"k8s.io/kubernetes/pkg/scheduler/framework/plugins/queuesort"
	scheduledruntime "k8s.io/kubernetes/pkg/scheduler/framework/runtime"
	schedulertesting "k8s.io/kubernetes/pkg/scheduler/testing"
)

func TestNew(t *testing.T) {
	defaultArgs, err := getDefaultPreemptionArgs()
	assert.NoError(t, err)

	tests := []struct {
		name     string
		args     runtime.Object
		wantErr  bool
		wantArgs *scheduledconfig.DefaultPreemptionArgs
	}{
		{
			name:     "nil args",
			wantArgs: defaultArgs,
		},
		{
			name: "args with unknown object",
			args: &runtime.Unknown{
				ContentType: runtime.ContentTypeJSON,
				Raw:         []byte(`{"minCandidateNodesPercentage": 20, "minCandidateNodesAbsolute": 80}`),
			},
			wantArgs: &scheduledconfig.DefaultPreemptionArgs{
				MinCandidateNodesPercentage: 20,
				MinCandidateNodesAbsolute:   80,
			},
		},
		{
			name: "args with invalid fields in args",
			args: &runtime.Unknown{
				ContentType: runtime.ContentTypeJSON,
				Raw:         []byte(`{"minCandidateNodesAbsolute": -1}`),
			},
			wantErr: true,
		},
		{
			name:    "args with other plugin args object",
			args:    &scheduledconfig.NodeResourcesFitArgs{},
			wantErr: true,
		},
		{
			name: "args with invalid json unknown object",
			args: &runtime.Unknown{
				ContentType: runtime.ContentTypeJSON,
				Raw:         []byte(`{"minCandidateNodesPercentage": 20, "minCandi`),
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			registeredPlugins := []schedulertesting.RegisterPluginFunc{
				func(reg *scheduledruntime.Registry, profile *scheduledconfig.KubeSchedulerProfile) {
					reg.Register(Name, New)
					profile.PluginConfig = []scheduledconfig.PluginConfig{
						{
							Name: Name,
							Args: nil,
						},
					}
					profile.Plugins.PostFilter.Disabled = append(profile.Plugins.PostFilter.Disabled, scheduledconfig.Plugin{
						Name: defaultpreemption.Name,
					})
					profile.Plugins.PostFilter.Enabled = append(profile.Plugins.PostFilter.Enabled, scheduledconfig.Plugin{
						Name: Name,
					})
				},

				schedulertesting.RegisterBindPlugin(defaultbinder.Name, defaultbinder.New),
				schedulertesting.RegisterQueueSortPlugin(queuesort.Name, queuesort.New),
			}

			cs := kubefake.NewSimpleClientset()
			informerFactory := informers.NewSharedInformerFactory(cs, 0)
			fh, err := schedulertesting.NewFramework(registeredPlugins, "koord-scheduler",
				scheduledruntime.WithClientSet(cs),
				scheduledruntime.WithInformerFactory(informerFactory),
			)
			assert.Nil(t, err)

			p, err := New(tt.args, fh)
			if tt.wantErr && err == nil {
				t.Fatal("expect err but got nil")
			}
			if tt.wantErr {
				return
			}
			assert.NoError(t, err)
			assert.NotNil(t, p)
			assert.Equal(t, Name, p.Name())
			assert.Equal(t, tt.wantArgs, p.(*CompatibleDefaultPreemption).args)
		})
	}
}
