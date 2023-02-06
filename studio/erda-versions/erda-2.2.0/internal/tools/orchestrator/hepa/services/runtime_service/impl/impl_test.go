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

package impl

import (
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

func TestDemo(t *testing.T) {
	bdl := bundle.New()

	defer monkey.UnpatchAll()
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetProjectWithSetter", func(*bundle.Bundle,
		uint64, ...httpclient.RequestSetter) (*apistructs.ProjectDTO, error) {
		return &apistructs.ProjectDTO{
			Name: "fake-project",
		}, nil
	})

	type args struct {
		endpoints []diceyml.Endpoint
	}

	tests := []struct {
		name    string
		args    args
		want    []diceyml.Endpoint
		wantErr bool
	}{
		{
			name: "test-1",
			args: args{
				endpoints: []diceyml.Endpoint{
					{Domain: "domain.*"},
				},
			},
			want: []diceyml.Endpoint{
				{Domain: "domain.*"},
			},
			wantErr: false,
		},
		{
			name: "test-2",
			args: args{
				endpoints: []diceyml.Endpoint{
					{Domain: "domain.${platform.DICE_PROJECT_NAME}-${platform.DICE_PROJECT_NAME}.*"},
				},
			},
			want: []diceyml.Endpoint{
				{Domain: "domain.fake-project-fake-project.*"},
			},
			wantErr: false,
		},
		{
			name: "test-3",
			args: args{
				endpoints: []diceyml.Endpoint{
					{Domain: "domain.${platform.DICE_APPLICATION_NAME}.*"},
				},
			},
			wantErr: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := renderPlatformInfo(tt.args.endpoints, "1")
			if (err != nil) != tt.wantErr {
				t.Errorf("renderPlatformInfo error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			assert.Equal(t, got, tt.want)
		})
	}
}
