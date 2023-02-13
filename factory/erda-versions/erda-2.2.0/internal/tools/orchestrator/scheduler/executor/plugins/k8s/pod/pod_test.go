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

package pod

import (
	"testing"

	"github.com/sirupsen/logrus"
	"gotest.tools/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func TestPod_GetNamespacedPodsStatus(t *testing.T) {
	pod := Pod{}
	pods := []corev1.Pod{
		{
			ObjectMeta: metav1.ObjectMeta{
				Name: "test-pod",
			},
			Spec: corev1.PodSpec{},
			Status: corev1.PodStatus{
				Phase: corev1.PodPending,
				Conditions: []corev1.PodCondition{
					{
						Type:               corev1.PodInitialized,
						Status:             "True",
						LastTransitionTime: metav1.Time{},
					},
					{
						Type:               corev1.ContainersReady,
						Status:             "False",
						LastTransitionTime: metav1.Time{},
						Reason:             "ContainersNotReady",
						Message:            "containers with unready status: [test-pod]",
					},
					{
						Type:               corev1.PodScheduled,
						Status:             "True",
						LastTransitionTime: metav1.Time{},
					},
				},
				Message: "containers with unready status: [test-pod]",
				Reason:  "ContainersNotReady",
				HostIP:  "10.1.1.1",
				PodIP:   "9.8.7.6",
				PodIPs: []corev1.PodIP{
					{
						IP: "9.8.7.6",
					},
				},
				ContainerStatuses: []corev1.ContainerStatus{
					{
						Image: "test-pod:v1",
						Name:  "test-pod",
						State: corev1.ContainerState{
							Waiting: &corev1.ContainerStateWaiting{
								Reason:  "ImagePullBackOff",
								Message: "Back-off pulling image \"test:v1\"",
							},
						},
					},
				},
				StartTime: &metav1.Time{},
			},
		},
	}
	podStatus, err := pod.GetNamespacedPodsStatus(pods, "")
	if err != nil {
		logrus.Fatal(err)
	}
	assert.DeepEqual(t, podStatus, []PodStatus{})

	podStatus, err = pod.GetNamespacedPodsStatus(pods, "test-pod")
	if err != nil {
		logrus.Fatal(err)
	}
	assert.DeepEqual(t, podStatus, []PodStatus{
		{
			Reason:  ImagePullFailed,
			Message: "Back-off pulling image \"test:v1\"",
		},
	})

}
