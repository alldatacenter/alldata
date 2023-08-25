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

package pleg

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/utils/inotify"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func NewTestWatcher() (Watcher, error) {
	return &testWatcher{
		events: make(chan *inotify.Event, 16),
	}, nil
}

type testWatcher struct {
	events chan *inotify.Event
}

func (w *testWatcher) Close() error {
	return nil
}

func (w *testWatcher) AddWatch(path string) error {
	return nil
}

func (w *testWatcher) RemoveWatch(path string) error {
	return nil
}

func (w *testWatcher) Event() chan *inotify.Event {
	return w.events
}

func (w *testWatcher) Error() chan error {
	return make(chan error)
}

func NewTestHandler() *testHandler {
	return &testHandler{events: make(chan *event, 16)}
}

type testHandler struct {
	events chan *event
}

func (h *testHandler) OnPodAdded(podID string) {
	h.events <- newPodEvent(podID, podAdded)
}

func (h *testHandler) OnPodDeleted(podID string) {
	h.events <- newPodEvent(podID, podDeleted)
}

func (h *testHandler) OnContainerAdded(podID, containerID string) {
	h.events <- newContainerEvent(podID, containerID, containerAdded)
}

func (h *testHandler) OnContainerDeleted(podID, containerID string) {
	h.events <- newContainerEvent(podID, containerID, containerDeleted)
}

func TestPlegHandlePodEvents(t *testing.T) {
	stopCh := make(chan struct{})
	defer close(stopCh)

	pg, err := NewPLEG("./test")
	if err != nil {
		t.Fatal(err)
	}
	pg.(*pleg).podWatcher, _ = NewTestWatcher()
	pg.(*pleg).containerWatcher, _ = NewTestWatcher()
	go pg.Run(stopCh)

	handler := NewTestHandler()
	id := pg.AddHandler(handler)
	defer pg.RemoverHandler(id)

	testCases := []struct {
		name      string
		mockEvent *inotify.Event
		want      bool
		wantEvent *event
	}{
		{
			name:      "create pod",
			mockEvent: &inotify.Event{Mask: IN_CREATE | IN_ISDIR, Name: "./test/kubepods-pod12345.slice"},
			want:      true,
			wantEvent: newPodEvent("12345", podAdded),
		},
		{
			name:      "remove pod",
			mockEvent: &inotify.Event{Mask: IN_DELETE | IN_ISDIR, Name: "./test/kubepods-pod12345.slice"},
			want:      true,
			wantEvent: newPodEvent("12345", podDeleted),
		},
		{
			name:      "invalid pod",
			mockEvent: &inotify.Event{Mask: IN_DELETE | IN_ISDIR, Name: "./test/12345"},
			want:      false,
		},
		{
			name:      "burstable pod",
			mockEvent: &inotify.Event{Mask: IN_CREATE | IN_ISDIR, Name: "./test/kubepods-burstable.slice/kubepods-burstable-pod12345.slice"},
			want:      true,
			wantEvent: newPodEvent("12345", podAdded),
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			pg.(*pleg).podWatcher.Event() <- tc.mockEvent
			timer := time.NewTimer(100 * time.Millisecond)
			defer timer.Stop()
			if tc.want {
				select {
				case evt := <-handler.events:
					assert.Equal(t, tc.wantEvent, evt, "unexpected event received "+evt.podID)
				case <-timer.C:
					t.Errorf("read event timeout")
				}
			} else {
				select {
				case evt := <-handler.events:
					t.Errorf("unexpceted event received: %v", evt)
				case <-timer.C:
				}
			}
		})
	}
}

func TestPlegHandleContainerEvents(t *testing.T) {
	stopCh := make(chan struct{})
	defer close(stopCh)

	pg, err := NewPLEG("./test")
	if err != nil {
		t.Fatal(err)
	}
	pg.(*pleg).podWatcher, _ = NewTestWatcher()
	pg.(*pleg).containerWatcher, _ = NewTestWatcher()
	go pg.Run(stopCh)

	handler := NewTestHandler()
	id := pg.AddHandler(handler)
	defer pg.RemoverHandler(id)

	testCases := []struct {
		name      string
		mockEvent *inotify.Event
		want      bool
		wantEvent *event
	}{
		{
			name:      "create container",
			mockEvent: &inotify.Event{Mask: IN_CREATE | IN_ISDIR, Name: "./test/kubepods-pod12345.slice/cri-containerd-container1.scope"},
			want:      true,
			wantEvent: newContainerEvent("12345", "container1", containerAdded),
		},
		{
			name:      "remove container",
			mockEvent: &inotify.Event{Mask: IN_DELETE | IN_ISDIR, Name: "./test/kubepods-pod12345.slice/cri-containerd-container1.scope"},
			want:      true,
			wantEvent: newContainerEvent("12345", "container1", containerDeleted),
		},
		{
			name:      "create container with invalid pod prefix",
			mockEvent: &inotify.Event{Mask: IN_CREATE | IN_ISDIR, Name: "./test/kubepods-burstable.slice/12345/cri-containerd-container1.scope"},
			want:      false,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			pg.(*pleg).containerWatcher.Event() <- tc.mockEvent
			timer := time.NewTimer(100 * time.Millisecond)
			defer timer.Stop()
			if tc.want {
				select {
				case evt := <-handler.events:
					assert.Equal(t, tc.wantEvent, evt, "unexpected event received")
				case <-timer.C:
					t.Errorf("read event timeout")
				}
			} else {
				select {
				case evt := <-handler.events:
					t.Errorf("unexpceted event received: %v", evt)
				case <-timer.C:
				}
			}
		})
	}
}

func Test_getWatchCgroupPath(t *testing.T) {
	type args struct {
		cgroupRootDir string
		qosClass      corev1.PodQOSClass
	}
	type fields struct {
		UseCgroupsV2 bool
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   string
	}{
		{
			name: "guaranteed qos",
			args: args{
				cgroupRootDir: "/host-cgroup",
				qosClass:      corev1.PodQOSGuaranteed,
			},
			want: "/host-cgroup/cpu/kubepods.slice",
		},
		{
			name: "besteffort qos",
			args: args{
				cgroupRootDir: "/test",
				qosClass:      corev1.PodQOSBestEffort,
			},
			want: "/test/cpu/kubepods.slice/kubepods-besteffort.slice",
		},
		{
			name: "guaranteed qos (v2)",
			fields: fields{
				UseCgroupsV2: true,
			},
			args: args{
				cgroupRootDir: "/host-cgroup",
				qosClass:      corev1.PodQOSGuaranteed,
			},
			want: "/host-cgroup/kubepods.slice",
		},
		{
			name: "burstable qos (v2)",
			fields: fields{
				UseCgroupsV2: true,
			},
			args: args{
				cgroupRootDir: "/test",
				qosClass:      corev1.PodQOSBurstable,
			},
			want: "/test/kubepods.slice/kubepods-burstable.slice",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			oldUseCgroupV2 := system.UseCgroupsV2
			system.UseCgroupsV2 = tt.fields.UseCgroupsV2
			defer func() {
				system.UseCgroupsV2 = oldUseCgroupV2
			}()

			got := getWatchCgroupPath(tt.args.cgroupRootDir, tt.args.qosClass)
			assert.Equal(t, tt.want, got)
		})
	}
}
