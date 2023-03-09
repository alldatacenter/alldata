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

package metricsadvisor

import (
	"github.com/stretchr/testify/assert"

	"sync"
	"testing"
	"time"
)

func Test_collectState_HasSynced(t *testing.T) {
	type fields struct {
		updateTimeMap map[string]*time.Time
	}
	totalTime := time.Now()
	tests := []struct {
		name   string
		fields fields
		want   bool
	}{
		{
			name: "new-state",
			fields: fields{
				updateTimeMap: map[string]*time.Time{
					nodeResUsedUpdateTime: nil,
					podResUsedUpdateTime:  nil,
					nodeCPUInfoUpdateTime: nil,
				},
			},
			want: false,
		},
		{
			name: "synced-state",
			fields: fields{
				updateTimeMap: map[string]*time.Time{
					nodeResUsedUpdateTime: &totalTime,
					podResUsedUpdateTime:  &totalTime,
					nodeCPUInfoUpdateTime: &totalTime,
				},
			},
			want: true,
		},
		{
			name: "sync-state",
			fields: fields{
				updateTimeMap: map[string]*time.Time{
					nodeResUsedUpdateTime: &totalTime,
					podResUsedUpdateTime:  nil,
					nodeCPUInfoUpdateTime: &totalTime,
				},
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			c := &collectState{
				mu:            sync.RWMutex{},
				updateTimeMap: tt.fields.updateTimeMap,
			}
			assert.Equalf(t, tt.want, c.HasSynced(), "HasSynced()")
		})
	}
}

func Test_collectState_RefreshTime(t *testing.T) {
	type fields struct {
		updateTimeMap map[string]*time.Time
	}
	type args struct {
		key string
	}
	tests := []struct {
		name   string
		fields fields
		args   args
	}{
		{
			name: "new-state",
			fields: fields{
				updateTimeMap: map[string]*time.Time{
					nodeResUsedUpdateTime: nil,
					podResUsedUpdateTime:  nil,
					nodeCPUInfoUpdateTime: nil,
				},
			},
			args: args{
				key: nodeResUsedUpdateTime,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			c := &collectState{
				updateTimeMap: tt.fields.updateTimeMap,
			}
			c.RefreshTime(tt.args.key)
		})
	}
}

func Test_newCollectState(t *testing.T) {
	tests := []struct {
		name string
		want *collectState
	}{
		{
			name: "new-state",
			want: &collectState{
				mu: sync.RWMutex{},
				updateTimeMap: map[string]*time.Time{
					nodeResUsedUpdateTime: nil,
					podResUsedUpdateTime:  nil,
					nodeCPUInfoUpdateTime: nil,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			assert.Equalf(t, tt.want, newCollectState(), "newCollectState()")
		})
	}
}
