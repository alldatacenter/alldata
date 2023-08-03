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

package sharedlisterext

import (
	"encoding/json"
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/kubernetes/pkg/scheduler/framework"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

func makeAllocatableResources(cpu, memory, pods, BatchCPU, storage, BatchMem string) corev1.ResourceList {
	return corev1.ResourceList{
		corev1.ResourceCPU:              resource.MustParse(cpu),
		corev1.ResourceMemory:           resource.MustParse(memory),
		corev1.ResourcePods:             resource.MustParse(pods),
		corev1.ResourceEphemeralStorage: resource.MustParse(storage),
		apiext.BatchCPU:                 resource.MustParse(BatchCPU),
		apiext.BatchMemory:              resource.MustParse(BatchMem),
	}
}

func generateFakeNode(reserved apiext.NodeReservation) *corev1.Node {
	reservedStr, err := json.Marshal(reserved)
	if err != nil {
		return nil
	}

	node := corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Annotations: map[string]string{
				apiext.AnnotationNodeReservation: string(reservedStr),
			},
		},
		Status: corev1.NodeStatus{
			Allocatable: makeAllocatableResources("10", "10Gi", "200", "1", "10Gi", "1Gi"),
		},
	}

	return &node
}

func generateFakeNodeWithoutAnno() *corev1.Node {
	node := corev1.Node{
		Status: corev1.NodeStatus{
			Allocatable: makeAllocatableResources("10", "10Gi", "200", "1", "10Gi", "1Gi"),
		},
	}

	return &node
}

func generateFakeNodeWithoutReservAnno() *corev1.Node {
	node := corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Annotations: map[string]string{
				"k": "v",
			},
		},
		Status: corev1.NodeStatus{
			Allocatable: makeAllocatableResources("10", "10Gi", "200", "1", "10Gi", "1Gi"),
		},
	}

	return &node
}

func TestNodeReservationTransformer(t *testing.T) {

	type args struct {
		nodeAnnoReserved apiext.NodeReservation
	}
	tests := []struct {
		name string
		args args
	}{
		// TODO: Add test cases.
		{
			name: "reserve nothing",
			args: args{
				apiext.NodeReservation{},
			},
		},
		{
			name: "reserve cpu by quantity",
			args: args{
				apiext.NodeReservation{
					Resources: corev1.ResourceList{
						corev1.ResourceCPU: resource.MustParse("1"),
					},
				},
			},
		},
		{
			name: "reserve specific cores",
			args: args{
				apiext.NodeReservation{
					ReservedCPUs: "0-1",
				},
			},
		},
		{
			name: "reserve specific cores and quantity",
			args: args{
				apiext.NodeReservation{
					ReservedCPUs: "0-1",
					Resources: corev1.ResourceList{
						corev1.ResourceCPU: resource.MustParse("1"),
					},
				},
			},
		},
		{
			name: "reserve memory by quantity",
			args: args{
				apiext.NodeReservation{
					Resources: corev1.ResourceList{
						corev1.ResourceMemory: resource.MustParse("2Gi"),
					},
				},
			},
		},
		{
			name: "reserve memory and cpu by quantity",
			args: args{
				apiext.NodeReservation{
					Resources: corev1.ResourceList{
						corev1.ResourceMemory: resource.MustParse("2Gi"),
						corev1.ResourceCPU:    resource.MustParse("1"),
					},
				},
			},
		},
		{
			name: "reserve memory by quantity and reserve some specific cores",
			args: args{
				apiext.NodeReservation{
					Resources: corev1.ResourceList{
						corev1.ResourceMemory: resource.MustParse("1Gi"),
					},
					ReservedCPUs: "2",
				},
			},
		},
		{
			name: "reserve batch memory by quantity",
			args: args{
				apiext.NodeReservation{
					Resources: corev1.ResourceList{
						apiext.BatchMemory: resource.MustParse("1Gi"),
					},
				},
			},
		},
		{
			name: "reserve batch cpu by quantity",
			args: args{
				apiext.NodeReservation{
					Resources: corev1.ResourceList{
						apiext.BatchCPU: resource.MustParse("1"),
					},
				},
			},
		},
	}

	for _, tt := range tests {
		nodes := []*corev1.Node{
			generateFakeNode(tt.args.nodeAnnoReserved),
			generateFakeNodeWithoutAnno(),
			generateFakeNodeWithoutReservAnno(),
		}

		for _, node := range nodes {
			t.Run(tt.name, func(t *testing.T) {
				nodeinfo := framework.NewNodeInfo()
				nodeinfo.SetNode(node)
				fmt.Println(nodeinfo.Allocatable.ScalarResources)
				rl := util.GetNodeReservationFromAnnotation(node.Annotations)

				originAlloc := nodeinfo.Allocatable.Clone()

				expectedResource := &framework.Resource{
					MilliCPU:         originAlloc.MilliCPU - rl.Cpu().MilliValue(),
					Memory:           originAlloc.Memory - rl.Memory().Value(),
					EphemeralStorage: originAlloc.EphemeralStorage - rl.StorageEphemeral().Value(),
					AllowedPodNumber: originAlloc.AllowedPodNumber - int(rl.Pods().Value()),
					ScalarResources:  map[corev1.ResourceName]int64{},
				}

				for name, originAllocQ := range originAlloc.ScalarResources {
					expectedResource.ScalarResources[name] = originAllocQ
					if name == apiext.BatchCPU || name == apiext.BatchMemory {
						continue
					}

					if reservedQ, ok := rl[name]; ok {
						expectedResource.ScalarResources[name] = originAllocQ - reservedQ.Value()
					}
				}

				nodeReservationTransformer(nodeinfo)
				assert.Equal(t, expectedResource, nodeinfo.Allocatable)
			})
		}
	}

}
