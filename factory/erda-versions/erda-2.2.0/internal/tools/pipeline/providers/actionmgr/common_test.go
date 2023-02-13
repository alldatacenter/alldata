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

package actionmgr

import (
	"fmt"
	"reflect"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

func Test_getActionTypeVersion(t *testing.T) {
	tests := []struct {
		name        string
		wantType    string
		wantVersion string
	}{
		{
			name:        "git",
			wantType:    "git",
			wantVersion: "",
		},
		{
			name:        "git@1.0",
			wantType:    "git",
			wantVersion: "1.0",
		},
		{
			name:        "git@1.0@1.0",
			wantType:    "git",
			wantVersion: "1.0@1.0",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, got1 := getActionNameVersion(tt.name)
			assert.Equalf(t, tt.wantType, got, "getActionNameVersion(%v)", tt.name)
			assert.Equalf(t, tt.wantVersion, got1, "getActionNameVersion(%v)", tt.name)
		})
	}
}

func Test_makeActionTypeVersion(t *testing.T) {
	type args struct {
		typ     string
		version string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "git@1.0",
			args: args{
				typ:     "git",
				version: "1.0",
			},
			want: "git@1.0",
		},
		{
			name: "git",
			args: args{
				typ:     "git",
				version: "",
			},
			want: "git",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			assert.Equalf(t, tt.want, makeActionNameVersion(tt.args.typ, tt.args.version), "makeActionNameVersion(%v, %v)", tt.args.typ, tt.args.version)
		})
	}
}

func Test_provider_updateExtensionCache(t *testing.T) {
	p := &provider{Cfg: &config{RefreshInterval: time.Minute, PoolSize: 20}}
	p.actionsCache = make(map[string]apistructs.ExtensionVersion)
	p.defaultActionsCache = make(map[string]apistructs.ExtensionVersion)
	p.bdl = bundle.New(bundle.WithAllAvailableClients())

	// before: not-in-cache
	// mock bundle
	bdl := &bundle.Bundle{}
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "QueryExtensionVersions",
		func(_ *bundle.Bundle, req apistructs.ExtensionVersionQueryRequest) ([]apistructs.ExtensionVersion, error) {
			// check name
			if len(req.Name) == 0 {
				return nil, fmt.Errorf("empty name")
			}
			// mock two versions
			versions := []apistructs.ExtensionVersion{
				{Name: req.Name, Version: "1.0", IsDefault: true, Public: true},
				{Name: req.Name, Version: "2.0", IsDefault: false, Public: true},
			}
			return versions, nil
		})
	monkey.Unpatch(reflect.TypeOf(bdl))
	action := apistructs.Extension{Name: "git-checkout"}
	// preset a mock default action to test delete logic
	p.defaultActionsCache[action.Name] = apistructs.ExtensionVersion{Name: action.Name, Version: "mock", IsDefault: true}
	p.updateExtensionCache(action)
	if _, ok := p.actionsCache[makeActionNameVersion(action.Name, "1.0")]; !ok {
		t.Fatalf("1.0 not exist")
	}
	if _, ok := p.actionsCache[makeActionNameVersion(action.Name, "2.0")]; !ok {
		t.Fatalf("2.0 not exist")
	}
	if _, ok := p.defaultActionsCache[action.Name]; !ok {
		t.Fatalf("default not exist")
	}
	if p.defaultActionsCache[action.Name].Version != "1.0" {
		t.Fatalf("1.0 is default")
	}
}

func Test_getActionNameVersion(t *testing.T) {
	type args struct {
		nameVersion string
	}
	tests := []struct {
		name  string
		args  args
		want  string
		want1 string
	}{
		{
			name: "test default",
			args: args{
				nameVersion: "test@default",
			},
			want:  "test",
			want1: "",
		},
		{
			name: "test version",
			args: args{
				nameVersion: "test@1.0",
			},
			want1: "1.0",
			want:  "test",
		},
		{
			name: "test empty version",
			args: args{
				nameVersion: "test@",
			},
			want:  "test",
			want1: "",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, got1 := getActionNameVersion(tt.args.nameVersion)
			if got != tt.want {
				t.Errorf("getActionNameVersion() got = %v, want %v", got, tt.want)
			}
			if got1 != tt.want1 {
				t.Errorf("getActionNameVersion() got1 = %v, want %v", got1, tt.want1)
			}
		})
	}
}
