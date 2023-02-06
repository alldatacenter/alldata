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

package query

import (
	"reflect"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/pkg/cache"
)

func Test_issueCache_TryGetIteration(t *testing.T) {
	type args struct {
		iterationID int64
	}

	var c *cache.Cache
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(c), "LoadWithUpdate",
		func(d *cache.Cache, key interface{}) (interface{}, bool) {
			return &dao.Iteration{
				ProjectID: 1,
			}, true
		},
	)
	defer p1.Unpatch()
	tests := []struct {
		name    string
		args    args
		want    *dao.Iteration
		wantErr bool
	}{
		{
			args: args{0},
			want: nil,
		},
		{
			args: args{1},
			want: &dao.Iteration{
				ProjectID: 1,
			},
		},
	}

	v := &issueCache{iterationCache: c}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := v.TryGetIteration(tt.args.iterationID)
			if (err != nil) != tt.wantErr {
				t.Errorf("issueCache.TryGetIteration() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("issueCache.TryGetIteration() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_issueCache_TryGetState(t *testing.T) {
	var c *cache.Cache
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(c), "LoadWithUpdate",
		func(d *cache.Cache, key interface{}) (interface{}, bool) {
			return &dao.IssueState{
				ProjectID: 1,
			}, true
		},
	)
	defer p1.Unpatch()

	type args struct {
		stateID int64
	}
	tests := []struct {
		name    string
		args    args
		want    *dao.IssueState
		wantErr bool
	}{
		{
			args:    args{0},
			wantErr: true,
		},
		{
			args: args{1},
			want: &dao.IssueState{
				ProjectID: 1,
			},
		},
	}

	v := &issueCache{stateCache: c}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := v.TryGetState(tt.args.stateID)
			if (err != nil) != tt.wantErr {
				t.Errorf("issueCache.TryGetState() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("issueCache.TryGetState() = %v, want %v", got, tt.want)
			}
		})
	}
}
