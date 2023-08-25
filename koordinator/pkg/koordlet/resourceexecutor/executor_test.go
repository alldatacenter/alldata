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

package resourceexecutor

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/audit"
	sysutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
	"github.com/koordinator-sh/koordinator/pkg/util/cache"
)

func TestNewResourceUpdateExecutor(t *testing.T) {
	t.Run("", func(t *testing.T) {
		e := NewResourceUpdateExecutor()
		assert.NotNil(t, e)
	})
}

func TestNewResourceUpdateExecutor_Run(t *testing.T) {
	t.Run("", func(t *testing.T) {
		e := &ResourceUpdateExecutorImpl{
			ResourceCache: cache.NewCacheDefault(),
			Config:        NewDefaultConfig(),
		}
		stop := make(chan struct{})
		defer func() {
			close(stop)
		}()

		e.Run(stop)
	})
}

func TestResourceUpdateExecutor_Update(t *testing.T) {
	testUpdater, err := DefaultCgroupUpdaterFactory.New(sysutil.CPUCFSQuotaName, "test", "-1", &audit.EventHelper{})
	assert.NoError(t, err)
	testUpdater1, err := DefaultCgroupUpdaterFactory.New(sysutil.MemoryLimitName, "test", "1048576", &audit.EventHelper{})
	assert.NoError(t, err)
	type fields struct {
		notStarted bool
		updateErr  bool
	}
	type args struct {
		isCacheable bool
		resource    ResourceUpdater
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    bool
		wantErr bool
	}{
		{
			name: "non-cacheable update",
			args: args{
				isCacheable: false,
				resource:    testUpdater,
			},
			want:    true,
			wantErr: false,
		},
		{
			name: "cacheable update",
			args: args{
				isCacheable: true,
				resource:    testUpdater1,
			},
			want:    true,
			wantErr: false,
		},
		{
			name: "cacheable update but not started",
			fields: fields{
				notStarted: true,
			},
			args: args{
				isCacheable: true,
				resource:    testUpdater1,
			},
			want:    false,
			wantErr: true,
		},
		{
			name: "update error",
			fields: fields{
				updateErr: true,
			},
			args: args{
				isCacheable: true,
				resource:    testUpdater1,
			},
			want:    false,
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := sysutil.NewFileTestUtil(t)
			defer helper.Cleanup()
			if !tt.fields.updateErr { // make update non-exist file error
				helper.WriteFileContents(tt.args.resource.Path(), "")
			}

			e := &ResourceUpdateExecutorImpl{
				ResourceCache: cache.NewCacheDefault(),
				Config:        NewDefaultConfig(),
			}
			if !tt.fields.notStarted {
				stop := make(chan struct{})
				defer func() {
					close(stop)
				}()

				e.Run(stop)
			}

			got, gotErr := e.Update(tt.args.isCacheable, tt.args.resource)
			assert.Equal(t, tt.want, got)
			assert.Equal(t, tt.wantErr, gotErr != nil)
		})
	}
}

func TestResourceUpdateExecutor_UpdateBatch(t *testing.T) {
	testUpdater, err := DefaultCgroupUpdaterFactory.New(sysutil.CPUCFSQuotaName, "test", "-1", &audit.EventHelper{})
	assert.NoError(t, err)
	testUpdater1, err := DefaultCgroupUpdaterFactory.New(sysutil.MemoryLimitName, "test", "1048576", &audit.EventHelper{})
	assert.NoError(t, err)
	type fields struct {
		notStarted bool
	}
	type args struct {
		isCacheable bool
		resources   []ResourceUpdater
	}
	tests := []struct {
		name   string
		fields fields
		args   args
	}{
		{
			name: "nothing to update",
			args: args{
				isCacheable: false,
			},
		},
		{
			name: "non-cacheable update a batch of resources",
			args: args{
				isCacheable: false,
				resources: []ResourceUpdater{
					testUpdater,
					testUpdater1,
				},
			},
		},
		{
			name: "cacheable update a batch of resource",
			args: args{
				isCacheable: true,
				resources: []ResourceUpdater{
					testUpdater,
					testUpdater1,
				},
			},
		},
		{
			name: "abort cacheable update when GC is not started",
			fields: fields{
				notStarted: true,
			},
			args: args{
				isCacheable: true,
				resources: []ResourceUpdater{
					testUpdater,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := sysutil.NewFileTestUtil(t)
			defer helper.Cleanup()
			e := &ResourceUpdateExecutorImpl{
				ResourceCache: cache.NewCacheDefault(),
				Config:        NewDefaultConfig(),
			}
			if !tt.fields.notStarted {
				stop := make(chan struct{})
				defer func() {
					close(stop)
				}()

				e.Run(stop)
			}

			e.UpdateBatch(tt.args.isCacheable, tt.args.resources...)
		})
	}
}
