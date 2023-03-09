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

package eventhandlers

import (
	corev1 "k8s.io/api/core/v1"
	"k8s.io/kubernetes/pkg/scheduler"
	"k8s.io/kubernetes/pkg/scheduler/framework"
)

var assignedPodDelete = framework.ClusterEvent{Resource: framework.Pod, ActionType: framework.Delete, Label: "AssignedPodDelete"}

// SchedulerInternalHandler exports scheduler internal cache and queue interface for testability.
type SchedulerInternalHandler interface {
	GetCache() SchedulerInternalCacheHandler
	GetQueue() SchedulerInternalQueueHandler
	MoveAllToActiveOrBackoffQueue(event framework.ClusterEvent)
}

type SchedulerInternalCacheHandler interface {
	AddPod(pod *corev1.Pod) error
	UpdatePod(oldPod, newPod *corev1.Pod) error
	RemovePod(pod *corev1.Pod) error
	IsAssumedPod(pod *corev1.Pod) (bool, error)
	GetPod(pod *corev1.Pod) (*corev1.Pod, error)
}

type SchedulerInternalQueueHandler interface {
	Add(pod *corev1.Pod) error
	Update(oldPod, newPod *corev1.Pod) error
	Delete(pod *corev1.Pod) error
	AddUnschedulableIfNotPresent(pod *framework.QueuedPodInfo, podSchedulingCycle int64) error
	SchedulingCycle() int64
	AssignedPodAdded(pod *corev1.Pod)
	AssignedPodUpdated(pod *corev1.Pod)
}

var _ SchedulerInternalHandler = &SchedulerInternalHandlerImpl{}

type SchedulerInternalHandlerImpl struct {
	Scheduler *scheduler.Scheduler
}

func (s *SchedulerInternalHandlerImpl) GetCache() SchedulerInternalCacheHandler {
	return s.Scheduler.SchedulerCache
}

func (s *SchedulerInternalHandlerImpl) GetQueue() SchedulerInternalQueueHandler {
	return s.Scheduler.SchedulingQueue
}

func (s *SchedulerInternalHandlerImpl) MoveAllToActiveOrBackoffQueue(event framework.ClusterEvent) {
	s.Scheduler.SchedulingQueue.MoveAllToActiveOrBackoffQueue(event, nil)
}

var _ SchedulerInternalHandler = &fakeSchedulerInternalHandler{}

type fakeSchedulerInternalHandler struct{}

func (f *fakeSchedulerInternalHandler) GetCache() SchedulerInternalCacheHandler {
	return f
}

func (f *fakeSchedulerInternalHandler) GetQueue() SchedulerInternalQueueHandler {
	return f
}

func (f *fakeSchedulerInternalHandler) MoveAllToActiveOrBackoffQueue(event framework.ClusterEvent) {
}

func (f *fakeSchedulerInternalHandler) AddPod(pod *corev1.Pod) error {
	return nil
}

func (f *fakeSchedulerInternalHandler) UpdatePod(oldPod, newPod *corev1.Pod) error {
	return nil
}

func (f *fakeSchedulerInternalHandler) RemovePod(pod *corev1.Pod) error {
	return nil
}

func (f *fakeSchedulerInternalHandler) IsAssumedPod(pod *corev1.Pod) (bool, error) {
	return false, nil
}

func (f *fakeSchedulerInternalHandler) GetPod(pod *corev1.Pod) (*corev1.Pod, error) {
	return nil, nil
}

func (f *fakeSchedulerInternalHandler) Add(pod *corev1.Pod) error {
	return nil
}

func (f *fakeSchedulerInternalHandler) Update(oldPod, newPod *corev1.Pod) error {
	return nil
}

func (f *fakeSchedulerInternalHandler) Delete(pod *corev1.Pod) error {
	return nil
}

func (f *fakeSchedulerInternalHandler) AddUnschedulableIfNotPresent(pod *framework.QueuedPodInfo, podSchedulingCycle int64) error {
	return nil
}

func (f *fakeSchedulerInternalHandler) SchedulingCycle() int64 {
	return 0
}

func (f *fakeSchedulerInternalHandler) AssignedPodAdded(pod *corev1.Pod) {
}

func (f *fakeSchedulerInternalHandler) AssignedPodUpdated(pod *corev1.Pod) {
}
