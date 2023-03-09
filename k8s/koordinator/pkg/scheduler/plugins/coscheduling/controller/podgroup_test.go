/*
Copyright 2022 The Koordinator Authors.
Copyright 2020 The Kubernetes Authors.

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
package controller

import (
	"context"
	"fmt"
	"sort"
	"strings"
	"testing"
	"time"

	v1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/wait"
	"k8s.io/client-go/informers"
	"k8s.io/client-go/kubernetes/fake"
	"k8s.io/kubernetes/pkg/controller"
	st "k8s.io/kubernetes/pkg/scheduler/testing"
	"k8s.io/utils/pointer"
	"sigs.k8s.io/scheduler-plugins/pkg/apis/scheduling/v1alpha1"
	pgfake "sigs.k8s.io/scheduler-plugins/pkg/generated/clientset/versioned/fake"
	schedinformer "sigs.k8s.io/scheduler-plugins/pkg/generated/informers/externalversions"

	"github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/coscheduling/core"
)

func Test_Run(t *testing.T) {
	ctx := context.TODO()
	createTime := metav1.Time{Time: time.Now().Add(-72 * time.Hour)}
	cases := []struct {
		name               string
		pgName             string
		minMember          int32
		podNames           []string
		podNextPhase       v1.PodPhase
		podPhase           v1.PodPhase
		previousPhase      v1alpha1.PodGroupPhase
		desiredGroupPhase  v1alpha1.PodGroupPhase
		podGroupCreateTime *metav1.Time
	}{
		{
			name:              "Group running",
			pgName:            "pg1",
			minMember:         2,
			podNames:          []string{"pod1", "pod2"},
			podPhase:          v1.PodRunning,
			previousPhase:     v1alpha1.PodGroupScheduled,
			desiredGroupPhase: v1alpha1.PodGroupRunning,
		},
		{
			name:              "Group running, more than min member",
			pgName:            "pg11",
			minMember:         2,
			podNames:          []string{"pod11", "pod21"},
			podPhase:          v1.PodRunning,
			previousPhase:     v1alpha1.PodGroupScheduled,
			desiredGroupPhase: v1alpha1.PodGroupRunning,
		},
		{
			name:              "Group failed",
			pgName:            "pg2",
			minMember:         2,
			podNames:          []string{"pod1", "pod2"},
			podPhase:          v1.PodFailed,
			previousPhase:     v1alpha1.PodGroupScheduled,
			desiredGroupPhase: v1alpha1.PodGroupFailed,
		},
		{
			name:              "Group finished",
			pgName:            "pg3",
			minMember:         2,
			podNames:          []string{"pod1", "pod2"},
			podPhase:          v1.PodSucceeded,
			previousPhase:     v1alpha1.PodGroupScheduled,
			desiredGroupPhase: v1alpha1.PodGroupFinished,
		},
		{
			name:              "Group status convert from scheduling to scheduled",
			pgName:            "pg4",
			minMember:         2,
			podNames:          []string{"pod1", "pod2"},
			podPhase:          v1.PodPending,
			previousPhase:     v1alpha1.PodGroupScheduling,
			desiredGroupPhase: v1alpha1.PodGroupScheduled,
		},
		{
			name:              "Group status convert from scheduling to succeed",
			pgName:            "pg5",
			minMember:         2,
			podNames:          []string{"pod1", "pod2"},
			podPhase:          v1.PodPending,
			previousPhase:     v1alpha1.PodGroupScheduling,
			desiredGroupPhase: v1alpha1.PodGroupFinished,
			podNextPhase:      v1.PodSucceeded,
		},
		{
			name:              "Group status convert from scheduling to succeed",
			pgName:            "pg6",
			minMember:         2,
			podNames:          []string{"pod1", "pod2"},
			podPhase:          v1.PodPending,
			previousPhase:     v1alpha1.PodGroupScheduling,
			desiredGroupPhase: v1alpha1.PodGroupFinished,
			podNextPhase:      v1.PodSucceeded,
		},
		{
			name:              "Group status convert from pending to prescheduling",
			pgName:            "pg7",
			minMember:         2,
			podNames:          []string{"pod1", "pod2"},
			podPhase:          v1.PodPending,
			previousPhase:     v1alpha1.PodGroupPending,
			desiredGroupPhase: v1alpha1.PodGroupFinished,
			podNextPhase:      v1.PodSucceeded,
		},
		{
			name:               "Group should not enqueue, created too long",
			pgName:             "pg8",
			minMember:          2,
			podNames:           []string{"pod1", "pod2"},
			podPhase:           v1.PodRunning,
			previousPhase:      v1alpha1.PodGroupPending,
			desiredGroupPhase:  v1alpha1.PodGroupPending,
			podGroupCreateTime: &createTime,
		},
		{
			name:              "Group min member more than Pod number",
			pgName:            "pg9",
			minMember:         3,
			podNames:          []string{"pod91", "pod92"},
			podPhase:          v1.PodPending,
			previousPhase:     v1alpha1.PodGroupPending,
			desiredGroupPhase: v1alpha1.PodGroupPending,
		},
		{
			name:              "Group status convert from running to pending",
			pgName:            "pg10",
			minMember:         2,
			podNames:          []string{},
			podPhase:          v1.PodPending,
			previousPhase:     v1alpha1.PodGroupRunning,
			desiredGroupPhase: v1alpha1.PodGroupPending,
		},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			ctrl, kubeClient, pgClient := setUp(ctx, c.podNames, c.pgName, c.podPhase, c.minMember, c.previousPhase, c.podGroupCreateTime, nil)
			// 0 means not set
			if len(c.podNextPhase) != 0 {
				ps := makePods(c.podNames, c.pgName, c.podNextPhase, nil)
				for _, p := range ps {
					kubeClient.CoreV1().Pods(p.Namespace).UpdateStatus(ctx, p, metav1.UpdateOptions{})
				}
			}
			go ctrl.Start()
			err := wait.Poll(200*time.Millisecond, 1*time.Second, func() (done bool, err error) {
				pg, err := pgClient.SchedulingV1alpha1().PodGroups("default").Get(ctx, c.pgName, metav1.GetOptions{})
				if err != nil {
					return false, err
				}
				if pg.Status.Phase != c.desiredGroupPhase {
					return false, fmt.Errorf("want %v, got %v", c.desiredGroupPhase, pg.Status.Phase)
				}
				return true, nil
			})
			if err != nil {
				t.Fatal("Unexpected error", err)
			}
		})
	}

}

func TestFillGroupStatusOccupied(t *testing.T) {
	ctx := context.TODO()
	cases := []struct {
		name                 string
		pgName               string
		minMember            int32
		podNames             []string
		podPhase             v1.PodPhase
		podOwnerReference    []metav1.OwnerReference
		groupPhase           v1alpha1.PodGroupPhase
		desiredGroupOccupied []string
	}{
		{
			name:      "fill the Occupied of PodGroup with a single ownerReference",
			pgName:    "pg",
			minMember: 2,
			podNames:  []string{"pod1", "pod2"},
			podPhase:  v1.PodPending,
			podOwnerReference: []metav1.OwnerReference{
				{
					Name: "new-occupied",
				},
			},
			groupPhase:           v1alpha1.PodGroupPending,
			desiredGroupOccupied: []string{"default/new-occupied"},
		},
		{
			name:      "fill the Occupied of PodGroup with multi ownerReferences",
			pgName:    "pg",
			minMember: 2,
			podNames:  []string{"pod1", "pod2"},
			podPhase:  v1.PodPending,
			podOwnerReference: []metav1.OwnerReference{
				{
					Name: "new-occupied-1",
				},
				{
					Name: "new-occupied-2",
				},
			},
			groupPhase:           v1alpha1.PodGroupPending,
			desiredGroupOccupied: []string{"default/new-occupied-1", "default/new-occupied-2"},
		},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			ctrl, _, pgClient := setUp(ctx, c.podNames, c.pgName, c.podPhase, c.minMember, c.groupPhase, nil, c.podOwnerReference)
			go ctrl.Start()
			err := wait.Poll(200*time.Millisecond, 1*time.Second, func() (done bool, err error) {
				pg, err := pgClient.SchedulingV1alpha1().PodGroups("default").Get(ctx, c.pgName, metav1.GetOptions{})
				if err != nil {
					return false, err
				}
				sort.Strings(c.desiredGroupOccupied)
				desiredGroupOccupied := strings.Join(c.desiredGroupOccupied, ",")
				if pg.Status.OccupiedBy != desiredGroupOccupied {
					return false, fmt.Errorf("want %v, got %v", desiredGroupOccupied, pg.Status.OccupiedBy)
				}
				return true, nil
			})
			if err != nil {
				t.Fatal("Unexpected error", err)
			}
		})
	}
}

func setUp(ctx context.Context, podNames []string, pgName string, podPhase v1.PodPhase, minMember int32, groupPhase v1alpha1.PodGroupPhase, podGroupCreateTime *metav1.Time, podOwnerReference []metav1.OwnerReference) (*PodGroupController, *fake.Clientset, *pgfake.Clientset) {
	var kubeClient *fake.Clientset
	if len(podNames) == 0 {
		kubeClient = fake.NewSimpleClientset()
	} else {
		ps := makePods(podNames, pgName, podPhase, podOwnerReference)
		kubeClient = fake.NewSimpleClientset(ps[0], ps[1])
	}
	pg := makePG(pgName, minMember, groupPhase, podGroupCreateTime)
	pgClient := pgfake.NewSimpleClientset(pg)

	informerFactory := informers.NewSharedInformerFactory(kubeClient, controller.NoResyncPeriodFunc())
	pgInformerFactory := schedinformer.NewSharedInformerFactory(pgClient, controller.NoResyncPeriodFunc())
	podInformer := informerFactory.Core().V1().Pods()
	pgInformer := pgInformerFactory.Scheduling().V1alpha1().PodGroups()

	pgMgr := core.NewPodGroupManager(pgClient, pgInformerFactory, informerFactory, &config.CoschedulingArgs{DefaultTimeout: &metav1.Duration{Duration: time.Second}})
	ctrl := NewPodGroupController(pgInformer, podInformer, pgClient, pgMgr, 1)
	return ctrl, kubeClient, pgClient
}

func makePods(podNames []string, pgName string, phase v1.PodPhase, reference []metav1.OwnerReference) []*v1.Pod {
	pds := make([]*v1.Pod, 0)
	for _, name := range podNames {
		pod := st.MakePod().Namespace("default").Name(name).Obj()
		pod.Labels = map[string]string{v1alpha1.PodGroupLabel: pgName}
		pod.Status.Phase = phase
		if reference != nil && len(reference) != 0 {
			pod.OwnerReferences = reference
		}
		pds = append(pds, pod)
	}
	return pds
}

func makePG(pgName string, minMember int32, previousPhase v1alpha1.PodGroupPhase, createTime *metav1.Time) *v1alpha1.PodGroup {
	pg := &v1alpha1.PodGroup{
		ObjectMeta: metav1.ObjectMeta{
			Name:              pgName,
			Namespace:         "default",
			CreationTimestamp: metav1.Time{Time: time.Now()},
		},
		Spec: v1alpha1.PodGroupSpec{
			MinMember:              minMember,
			ScheduleTimeoutSeconds: pointer.Int32(10),
		},
		Status: v1alpha1.PodGroupStatus{
			OccupiedBy:        "test",
			Scheduled:         minMember,
			ScheduleStartTime: metav1.Time{Time: time.Now()},
			Phase:             previousPhase,
		},
	}
	if createTime != nil {
		pg.CreationTimestamp = *createTime
	}
	return pg
}
