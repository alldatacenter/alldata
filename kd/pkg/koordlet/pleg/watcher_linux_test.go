//go:build linux
// +build linux

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
	"os"
	"path"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestWatcher(t *testing.T) {
	watcher, err := NewWatcher()
	assert.NoError(t, err, "create watcher failed")
	defer watcher.Close()

	tempDir := t.TempDir()

	err = watcher.AddWatch(tempDir)
	assert.NoError(t, err, "watch path: %v failed", tempDir)

	testCases := []struct {
		name        string
		before      func()
		expected    EventType
		notRequired bool
	}{
		{
			name: "create dir",
			before: func() {
				os.Mkdir(path.Join(tempDir, "dir1"), 0644)
			},
			expected: DirCreated,
		},
		{
			name: "create file",
			before: func() {
				os.Create(path.Join(tempDir, "file1"))
			},
			expected: UnknownType,
		},
		{
			name: "remove file",
			before: func() {
				os.Remove(path.Join(tempDir, "file1"))
			},
			expected: UnknownType,
		},
		{
			name: "remove dir",
			before: func() {
				os.Remove(path.Join(tempDir, "dir1"))
			},
			expected: DirRemoved,
		},
		{
			name: "remove watch",
			before: func() {
				watcher.RemoveWatch(tempDir)
				os.Mkdir(path.Join(tempDir, "dir2"), 0644)
			},
			// ingore event after remove watch in some os
			expected:    UnknownType,
			notRequired: true,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			tc.before()
			timer := time.NewTimer(100 * time.Millisecond)
			defer timer.Stop()
			select {
			case evt := <-watcher.Event():
				assert.Equal(t, tc.expected, TypeOf(evt), "unexpected event type")
			case <-timer.C:
				if !tc.notRequired {
					assert.Fail(t, "failed to received event")
				}
			}
		})
	}
}
