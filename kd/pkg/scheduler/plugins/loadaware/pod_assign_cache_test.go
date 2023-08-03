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

package loadaware

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
)

var fakeTimeNowFn = func() time.Time {
	t := time.Time{}
	t.Add(100 * time.Second)
	return t
}

func TestPodAssignCache_OnAdd(t *testing.T) {
	tests := []struct {
		name      string
		pod       *corev1.Pod
		wantCache map[string]map[types.UID]*podAssignInfo
	}{
		{
			name:      "update pending pod",
			pod:       &corev1.Pod{},
			wantCache: map[string]map[types.UID]*podAssignInfo{},
		},
		{
			name: "update terminated pod",
			pod: &corev1.Pod{
				Spec: corev1.PodSpec{
					NodeName: "test-node",
				},
				Status: corev1.PodStatus{
					Phase: corev1.PodFailed,
				},
			},
			wantCache: map[string]map[types.UID]*podAssignInfo{},
		},
		{
			name: "update scheduled running pod",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:       "123456789",
					Namespace: "default",
					Name:      "test",
				},
				Spec: corev1.PodSpec{
					NodeName: "test-node",
				},
				Status: corev1.PodStatus{
					Phase: corev1.PodRunning,
				},
			},
			wantCache: map[string]map[types.UID]*podAssignInfo{
				"test-node": {
					"123456789": &podAssignInfo{
						pod: &corev1.Pod{
							ObjectMeta: metav1.ObjectMeta{
								UID:       "123456789",
								Namespace: "default",
								Name:      "test",
							},
							Spec: corev1.PodSpec{
								NodeName: "test-node",
							},
							Status: corev1.PodStatus{
								Phase: corev1.PodRunning,
							},
						},
						timestamp: fakeTimeNowFn(),
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			preTimeNowFn := timeNowFn
			defer func() {
				timeNowFn = preTimeNowFn
			}()
			timeNowFn = fakeTimeNowFn
			assignCache := newPodAssignCache()
			assignCache.OnAdd(tt.pod)
			assert.Equal(t, tt.wantCache, assignCache.podInfoItems)
		})
	}
}

func TestPodAssignCache_OnUpdate(t *testing.T) {
	tests := []struct {
		name        string
		pod         *corev1.Pod
		assignCache *podAssignCache
		wantCache   map[string]map[types.UID]*podAssignInfo
	}{
		{
			name:      "update pending pod",
			pod:       &corev1.Pod{},
			wantCache: map[string]map[types.UID]*podAssignInfo{},
		},
		{
			name: "update terminated pod",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:       "123456789",
					Namespace: "default",
					Name:      "test",
				},
				Spec: corev1.PodSpec{
					NodeName: "test-node",
				},
				Status: corev1.PodStatus{
					Phase: corev1.PodFailed,
				},
			},
			assignCache: &podAssignCache{
				podInfoItems: map[string]map[types.UID]*podAssignInfo{
					"test-node": {
						"123456789": &podAssignInfo{
							pod: &corev1.Pod{
								ObjectMeta: metav1.ObjectMeta{
									UID:       "123456789",
									Namespace: "default",
									Name:      "test",
								},
								Spec: corev1.PodSpec{
									NodeName: "test-node",
								},
								Status: corev1.PodStatus{
									Phase: corev1.PodRunning,
								},
							},
							timestamp: fakeTimeNowFn(),
						},
					},
				},
			},
			wantCache: map[string]map[types.UID]*podAssignInfo{},
		},
		{
			name: "update scheduled running pod",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:       "123456789",
					Namespace: "default",
					Name:      "test",
				},
				Spec: corev1.PodSpec{
					NodeName: "test-node",
				},
				Status: corev1.PodStatus{
					Phase: corev1.PodRunning,
				},
			},
			wantCache: map[string]map[types.UID]*podAssignInfo{
				"test-node": {
					"123456789": &podAssignInfo{
						pod: &corev1.Pod{
							ObjectMeta: metav1.ObjectMeta{
								UID:       "123456789",
								Namespace: "default",
								Name:      "test",
							},
							Spec: corev1.PodSpec{
								NodeName: "test-node",
							},
							Status: corev1.PodStatus{
								Phase: corev1.PodRunning,
							},
						},
						timestamp: fakeTimeNowFn(),
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			preTimeNowFn := timeNowFn
			defer func() {
				timeNowFn = preTimeNowFn
			}()
			timeNowFn = fakeTimeNowFn
			assignCache := tt.assignCache
			if assignCache == nil {
				assignCache = newPodAssignCache()
			}
			assignCache.OnUpdate(nil, tt.pod)
			assert.Equal(t, tt.wantCache, assignCache.podInfoItems)
		})
	}
}

func TestPodAssignCache_OnDelete(t *testing.T) {
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			UID:       "123456789",
			Namespace: "default",
			Name:      "test",
		},
		Spec: corev1.PodSpec{
			NodeName: "test-node",
		},
		Status: corev1.PodStatus{
			Phase: corev1.PodFailed,
		},
	}
	assignCache := &podAssignCache{
		podInfoItems: map[string]map[types.UID]*podAssignInfo{
			"test-node": {
				"123456789": &podAssignInfo{
					pod: &corev1.Pod{
						ObjectMeta: metav1.ObjectMeta{
							UID:       "123456789",
							Namespace: "default",
							Name:      "test",
						},
						Spec: corev1.PodSpec{
							NodeName: "test-node",
						},
						Status: corev1.PodStatus{
							Phase: corev1.PodRunning,
						},
					},
					timestamp: fakeTimeNowFn(),
				},
			},
		},
	}
	assignCache.OnDelete(pod)
	wantCache := map[string]map[types.UID]*podAssignInfo{}
	assert.Equal(t, wantCache, assignCache.podInfoItems)
}
