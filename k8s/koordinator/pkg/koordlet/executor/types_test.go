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

package executor

import (
	"errors"
	"reflect"
	"testing"
	"time"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func updateValueFunc(resource ResourceUpdater) error {
	resource.SetValue("bar")
	return nil
}

func mergeUpdateValueFunc(resource MergeableResourceUpdater) (MergeableResourceUpdater, error) {
	clone := resource.Clone()
	clone.SetValue("bar")

	if mru, ok := clone.(MergeableResourceUpdater); ok {
		return mru, nil
	} else {
		return nil, errors.New("assert MergeableResourceUpdater error")
	}
}

func TestCgroupResourceUpdater_MergeUpdate(t *testing.T) {
	type fields struct {
		owner               *OwnerRef
		value               string
		ParentDir           string
		resource            system.Resource
		lastUpdateTimestamp time.Time
		updateFunc          UpdateFunc
		mergeUpdateFunc     MergeUpdateFunc
		needMerge           bool
	}
	tests := []struct {
		name    string
		fields  fields
		want    string
		wantErr bool
	}{
		{
			fields: fields{
				value:           "foo",
				mergeUpdateFunc: mergeUpdateValueFunc,
			},
			want:    "bar",
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			c := &CgroupResourceUpdater{
				owner:               tt.fields.owner,
				value:               tt.fields.value,
				ParentDir:           tt.fields.ParentDir,
				resource:            tt.fields.resource,
				lastUpdateTimestamp: tt.fields.lastUpdateTimestamp,
				updateFunc:          tt.fields.updateFunc,
				mergeUpdateFunc:     tt.fields.mergeUpdateFunc,
				needMerge:           tt.fields.needMerge,
			}
			got, err := c.MergeUpdate()
			if (err != nil) != tt.wantErr {
				t.Errorf("MergeUpdate() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got.Value(), tt.want) {
				t.Errorf("MergeUpdate() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestCgroupResourceUpdater_Update(t *testing.T) {
	type fields struct {
		owner               *OwnerRef
		value               string
		ParentDir           string
		resource            system.Resource
		lastUpdateTimestamp time.Time
		updateFunc          UpdateFunc
		mergeUpdateFunc     MergeUpdateFunc
		needMerge           bool
	}
	tests := []struct {
		name    string
		fields  fields
		want    string
		wantErr bool
	}{
		{
			fields: fields{
				value:      "foo",
				updateFunc: updateValueFunc,
			},
			want:    "bar",
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			c := &CgroupResourceUpdater{
				owner:               tt.fields.owner,
				value:               tt.fields.value,
				ParentDir:           tt.fields.ParentDir,
				resource:            tt.fields.resource,
				lastUpdateTimestamp: tt.fields.lastUpdateTimestamp,
				updateFunc:          tt.fields.updateFunc,
				mergeUpdateFunc:     tt.fields.mergeUpdateFunc,
				needMerge:           tt.fields.needMerge,
			}
			if err := c.Update(); (err != nil) != tt.wantErr {
				t.Errorf("Update() error = %v, wantErr %v", err, tt.wantErr)
			}
			if !reflect.DeepEqual(c.Value(), tt.want) {
				t.Errorf("MergeUpdate() value = %v, want %v", c.Value(), tt.want)
			}
		})
	}
}
