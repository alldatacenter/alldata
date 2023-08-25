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
	"testing"

	"github.com/stretchr/testify/assert"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/utils/pointer"

	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
)

func TestRegisterCallbacksAndRun(t *testing.T) {
	type args struct {
		objType     RegisterType
		name        string
		description string
	}
	tests := []struct {
		name string
		args args
	}{
		{
			name: "register RegisterTypeNodeSLOSpec and run",
			args: args{
				objType:     RegisterTypeNodeSLOSpec,
				name:        "set-bool-var",
				description: "set test bool var as true",
			},
		},
		{
			name: "register RegisterTypeAllPods and run",
			args: args{
				objType:     RegisterTypeAllPods,
				name:        "set-bool-var",
				description: "set test bool var as true",
			},
		},
		{
			name: "register RegisterTypeNodeSLOSpec and run",
			args: args{
				objType:     RegisterTypeNodeSLOSpec,
				name:        "set-bool-var",
				description: "set test bool var as true",
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			testVar := pointer.BoolPtr(false)
			callbackFn := func(t RegisterType, obj interface{}, pods []*PodMeta) {
				*testVar = true
			}
			si := &callbackRunner{
				stateUpdateCallbacks: map[RegisterType][]updateCallback{
					RegisterTypeNodeSLOSpec:  {},
					RegisterTypeAllPods:      {},
					RegisterTypeNodeTopology: {},
				},
				statesInformer: &statesInformer{
					states: &pluginState{
						informerPlugins: map[pluginName]informerPlugin{
							nodeSLOInformerName: &nodeSLOInformer{
								nodeSLO: &slov1alpha1.NodeSLO{},
							},
							podsInformerName: &podsInformer{
								podMap: map[string]*PodMeta{},
							},
						},
					},
				},
			}
			si.RegisterCallbacks(tt.args.objType, tt.args.name, tt.args.description, callbackFn)
			si.getObjByType(tt.args.objType, UpdateCbCtx{})
			si.runCallbacks(tt.args.objType, &slov1alpha1.NodeSLO{})
			assert.Equal(t, *testVar, true)
		})
	}
}

func Test_statesInformer_startCallbackRunners(t *testing.T) {
	output := make(chan string, 1)
	nodeSLO := &slov1alpha1.NodeSLO{
		ObjectMeta: metav1.ObjectMeta{
			Labels: map[string]string{
				"test-label-key": "test-label-val1",
			},
		},
	}
	stopCh := make(chan struct{}, 1)
	type args struct {
		objType     RegisterType
		nodeSLO     *slov1alpha1.NodeSLO
		name        string
		description string
		fn          UpdateCbFn
	}
	tests := []struct {
		name       string
		args       args
		wantOutput string
	}{
		{
			name: "callback get nodeslo label",
			args: args{
				objType:     RegisterTypeNodeSLOSpec,
				nodeSLO:     nodeSLO,
				name:        "get value from node slo label",
				description: "get value from node slo label",
				fn: func(t RegisterType, obj interface{}, pods []*PodMeta) {
					output <- nodeSLO.Labels["test-label-key"]
					stopCh <- struct{}{}
				},
			},
			wantOutput: "test-label-val1",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			cr := &callbackRunner{
				callbackChans: map[RegisterType]chan UpdateCbCtx{
					tt.args.objType: make(chan UpdateCbCtx, 1),
				},
				stateUpdateCallbacks: map[RegisterType][]updateCallback{
					tt.args.objType: {},
				},
			}
			si := &statesInformer{
				states: &pluginState{
					callbackRunner: cr,
					informerPlugins: map[pluginName]informerPlugin{
						nodeSLOInformerName: &nodeSLOInformer{
							nodeSLO: tt.args.nodeSLO,
						},
						podsInformerName: &podsInformer{
							podMap: map[string]*PodMeta{},
						},
					},
				},
			}
			cr.Setup(si)
			cr.RegisterCallbacks(tt.args.objType, tt.args.name, tt.args.description, tt.args.fn)
			cr.Start(stopCh)
			cr.SendCallback(tt.args.objType)
			gotOutput := <-output
			assert.Equal(t, tt.wantOutput, gotOutput, "send callback for type %v got wrong",
				tt.args.objType.String())
		})
	}
}
