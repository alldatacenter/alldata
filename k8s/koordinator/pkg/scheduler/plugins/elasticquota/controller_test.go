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

package elasticquota

import (
	"context"
	"encoding/json"
	"fmt"
	"testing"
	"time"

	v1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	quotav1 "k8s.io/apiserver/pkg/quota/v1"
	"k8s.io/klog/v2"
	testing2 "k8s.io/kubernetes/pkg/scheduler/testing"
	"sigs.k8s.io/scheduler-plugins/pkg/apis/scheduling/v1alpha1"

	"github.com/koordinator-sh/koordinator/apis/extension"
)

func TestController_Run(t *testing.T) {
	ctx := context.TODO()
	cases := []struct {
		name          string
		elasticQuotas []*v1alpha1.ElasticQuota
		pods          []*v1.Pod
		want          []*v1alpha1.ElasticQuota
	}{
		{
			name: "no init Containers pod",
			elasticQuotas: []*v1alpha1.ElasticQuota{
				MakeEQ("t1-ns1", "t1-ns1").Min(MakeResourceList().CPU(3).Mem(5).Obj()).
					Max(MakeResourceList().CPU(5).Mem(15).GPU(1).Obj()).Obj(),
			},
			pods: []*v1.Pod{
				MakePod("t1-ns1", "pod1").Phase(v1.PodRunning).Container(
					MakeResourceList().CPU(1).Mem(2).GPU(1).Obj()).UID("pod1").Obj(),
				MakePod("t1-ns1", "pod2").Phase(v1.PodPending).Container(
					MakeResourceList().CPU(1).Mem(2).GPU(0).Obj()).UID("pod2").Obj(),
			},
			want: []*v1alpha1.ElasticQuota{
				MakeEQ("t1-ns1", "t1-ns1").
					Used(MakeResourceList().CPU(2).Mem(4).GPU(1).Obj()).Obj(),
			},
		},
		{
			name: "have init Containers pod",
			elasticQuotas: []*v1alpha1.ElasticQuota{
				MakeEQ("t2-ns1", "t2-ns1").
					Min(MakeResourceList().CPU(3).Mem(5).Obj()).
					Max(MakeResourceList().CPU(5).Mem(15).Obj()).Obj(),
			},
			pods: []*v1.Pod{
				// CPU: 2 Mem: 4
				MakePod("t2-ns1", "pod1").Phase(v1.PodRunning).
					Container(
						MakeResourceList().CPU(1).Mem(2).Obj()).
					Container(MakeResourceList().CPU(1).Mem(2).Obj()).UID("pod1").Obj(),
				//CPU: 3 Mem: 3
				MakePod("t2-ns1", "pod2").Phase(v1.PodRunning).
					InitContainerRequest(
						MakeResourceList().CPU(2).Mem(1).Obj()).
					InitContainerRequest(
						MakeResourceList().CPU(2).Mem(3).Obj()).
					Container(
						MakeResourceList().CPU(2).Mem(1).Obj()).
					Container(
						MakeResourceList().CPU(1).Mem(1).Obj()).UID("pod2").Obj(),
			},
			want: []*v1alpha1.ElasticQuota{
				MakeEQ("t2-ns1", "t2-ns1").
					Used(MakeResourceList().CPU(5).Mem(7).Obj()).Obj(),
			},
		},
		{
			name: "pods belongs to different quota",
			elasticQuotas: []*v1alpha1.ElasticQuota{
				MakeEQ("t3-ns1", "t3-ns1").
					Min(MakeResourceList().CPU(3).Mem(5).Obj()).
					Max(MakeResourceList().CPU(5).Mem(15).Obj()).Obj(),
				MakeEQ("t3-ns2", "t3-ns2").
					Min(MakeResourceList().CPU(3).Mem(5).Obj()).
					Max(MakeResourceList().CPU(5).Mem(15).Obj()).Obj(),
			},
			pods: []*v1.Pod{
				// CPU: 3, Mem: 3
				MakePod("t3-ns1", "pod1").Phase(v1.PodPending).
					InitContainerRequest(MakeResourceList().CPU(2).Mem(1).Obj()).
					InitContainerRequest(MakeResourceList().CPU(2).Mem(3).Obj()).
					Container(MakeResourceList().CPU(2).Mem(1).Obj()).
					Container(MakeResourceList().CPU(1).Mem(1).Obj()).
					ResourceVersion("2").UID("pod1").Obj(),
				// CPU: 4, Mem: 3
				MakePod("t3-ns2", "pod2").Phase(v1.PodRunning).
					InitContainerRequest(MakeResourceList().CPU(2).Mem(1).Obj()).
					InitContainerRequest(MakeResourceList().CPU(2).Mem(3).Obj()).
					Container(MakeResourceList().CPU(3).Mem(1).Obj()).
					Container(MakeResourceList().CPU(1).Mem(1).Obj()).UID("pod2").Obj(),
			},
			want: []*v1alpha1.ElasticQuota{
				MakeEQ("t3-ns1", "t3-ns1").
					Used(MakeResourceList().CPU(3).Mem(3).Obj()).Obj(),
				MakeEQ("t3-ns2", "t3-ns2").
					Used(MakeResourceList().CPU(4).Mem(3).Obj()).Obj(),
			},
		},
		{
			name: "min and max have the same fields",
			elasticQuotas: []*v1alpha1.ElasticQuota{
				MakeEQ("t4-ns1", "t4-ns1").
					Min(MakeResourceList().CPU(3).Mem(5).Obj()).
					Max(MakeResourceList().CPU(5).Mem(15).Obj()).Obj(),
			},
			pods: []*v1.Pod{},
			want: []*v1alpha1.ElasticQuota{
				MakeEQ("t4-ns1", "t4-ns1").Obj(),
			},
		},
		{
			name: "min and max have the different fields",
			elasticQuotas: []*v1alpha1.ElasticQuota{
				MakeEQ("t5-ns1", "t5-ns1").
					Min(MakeResourceList().CPU(3).Mem(5).GPU(2).Obj()).
					Max(MakeResourceList().CPU(5).Mem(15).Obj()).Obj(),
			},
			pods: []*v1.Pod{},
			want: []*v1alpha1.ElasticQuota{
				MakeEQ("t5-ns1", "t5-ns1").Obj(),
			},
		},
		{
			name: "pod and eq in the different namespaces",
			elasticQuotas: []*v1alpha1.ElasticQuota{
				MakeEQ("t6-ns1", "t6-ns1").
					Min(MakeResourceList().CPU(3).Mem(5).GPU(2).Obj()).
					Max(MakeResourceList().CPU(50).Mem(15).Obj()).Obj(),
				MakeEQ("t6-ns2", "t6-ns2").
					Min(MakeResourceList().CPU(3).Mem(5).GPU(2).Obj()).
					Max(MakeResourceList().CPU(50).Mem(15).Obj()).Obj(),
			},
			pods: []*v1.Pod{
				MakePod("t6-ns3", "pod1").Phase(v1.PodRunning).
					Container(MakeResourceList().CPU(1).Mem(2).GPU(1).Obj()).
					Container(MakeResourceList().CPU(1).Mem(2).Obj()).UID("pod1").Obj(),
			},
			want: []*v1alpha1.ElasticQuota{
				MakeEQ("t6-ns1", "t6-ns1").Obj(),
				MakeEQ("t6-ns2", "t6-ns2").Obj(),
			},
		},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			nodes := []*v1.Node{
				{
					ObjectMeta: metav1.ObjectMeta{
						DeletionTimestamp: &metav1.Time{Time: time.Now()},
					},
					Status: v1.NodeStatus{
						Allocatable: MakeResourceList().CPU(100).Mem(100).GPU(100).Obj(),
					},
				},
			}
			suit := newPluginTestSuitWithPod(t, nodes, nil)
			p := suit.plugin.(*Plugin)
			ctrl := NewElasticQuotaController(p.client, p.quotaLister, p.groupQuotaManager)
			for _, v := range c.elasticQuotas {
				suit.client.SchedulingV1alpha1().ElasticQuotas(v.Namespace).Create(ctx, v, metav1.CreateOptions{})
			}
			time.Sleep(10 * time.Millisecond)
			for _, p := range c.pods {
				suit.Handle.ClientSet().CoreV1().Pods(p.Namespace).Create(ctx, p, metav1.CreateOptions{})
			}
			time.Sleep(100 * time.Millisecond)
			ctrl.Start()
			var err error
			for i := 0; i < 10; i++ {
				for _, v := range c.want {
					get, _ := suit.client.SchedulingV1alpha1().ElasticQuotas(v.Namespace).Get(ctx, v.Name, metav1.GetOptions{})
					if get == nil {
						continue
					}
					var request v1.ResourceList
					json.Unmarshal([]byte(get.Annotations[extension.AnnotationRequest]), &request)
					if !quotav1.Equals(request, v.Status.Used) {
						err = fmt.Errorf("want %v, got %v,quotaName:%v", v.Status.Used, get.Status.Used, get.Name)
						time.Sleep(1 * time.Second)
						continue
					} else {
						err = nil
						break
					}
				}
			}
			if err != nil {
				klog.ErrorS(err, "Elastic Quota Test Failed\n")
			}
		})
	}
}

type eqWrapper struct{ *v1alpha1.ElasticQuota }

func MakeEQ(namespace, name string) *eqWrapper {
	eq := &v1alpha1.ElasticQuota{
		TypeMeta: metav1.TypeMeta{Kind: "ElasticQuota", APIVersion: "scheduling.sigs.k8s.io/v1alpha1"},
		ObjectMeta: metav1.ObjectMeta{
			Name:        name,
			Namespace:   namespace,
			Annotations: make(map[string]string),
		},
	}
	return &eqWrapper{eq}
}

func (e *eqWrapper) Min(min v1.ResourceList) *eqWrapper {
	e.ElasticQuota.Spec.Min = min
	return e
}

func (e *eqWrapper) Max(max v1.ResourceList) *eqWrapper {
	e.ElasticQuota.Spec.Max = max
	return e
}

func (e *eqWrapper) Used(used v1.ResourceList) *eqWrapper {
	e.ElasticQuota.Status.Used = used
	return e
}

func (e *eqWrapper) Obj() *v1alpha1.ElasticQuota {
	return e.ElasticQuota
}

type podWrapper struct{ *v1.Pod }

func MakePod(namespace, name string) *podWrapper {
	pod := testing2.MakePod().Namespace(namespace).Name(name).Obj()

	return &podWrapper{pod}
}

func (p *podWrapper) UID(id string) *podWrapper {
	p.SetUID(types.UID(id))
	return p
}

func (p *podWrapper) Label(string1, string2 string) *podWrapper {
	if p.Labels == nil {
		p.Labels = make(map[string]string)
	}
	p.Labels[string1] = string2
	return p
}

func (p *podWrapper) Phase(phase v1.PodPhase) *podWrapper {
	p.Pod.Status.Phase = phase
	return p
}

func (p *podWrapper) Container(request v1.ResourceList) *podWrapper {
	p.Pod.Spec.Containers = append(p.Pod.Spec.Containers, v1.Container{
		Name:  fmt.Sprintf("con%d", len(p.Pod.Spec.Containers)),
		Image: "image",
		Resources: v1.ResourceRequirements{
			Requests: request,
		},
	})
	return p
}

func (p *podWrapper) ResourceVersion(version string) *podWrapper {
	p.SetResourceVersion(version)
	return p
}

func (p *podWrapper) InitContainerRequest(request v1.ResourceList) *podWrapper {
	p.Pod.Spec.InitContainers = append(p.Pod.Spec.InitContainers, v1.Container{
		Name:  fmt.Sprintf("con%d", len(p.Pod.Spec.Containers)),
		Image: "image",
		Resources: v1.ResourceRequirements{
			Requests: request,
		},
	})
	return p
}

func (p *podWrapper) Obj() *v1.Pod {
	return p.Pod
}

type resourceWrapper struct{ v1.ResourceList }

func MakeResourceList() *resourceWrapper {
	return &resourceWrapper{v1.ResourceList{}}
}

func (r *resourceWrapper) CPU(val int64) *resourceWrapper {
	r.ResourceList[v1.ResourceCPU] = *resource.NewQuantity(val, resource.DecimalSI)
	return r
}

func (r *resourceWrapper) Mem(val int64) *resourceWrapper {
	r.ResourceList[v1.ResourceMemory] = *resource.NewQuantity(val, resource.DecimalSI)
	return r
}

func (r *resourceWrapper) GPU(val int64) *resourceWrapper {
	r.ResourceList["nvidia.com/gpu"] = *resource.NewQuantity(val, resource.DecimalSI)
	return r
}

func (r *resourceWrapper) Obj() v1.ResourceList {
	return r.ResourceList
}
