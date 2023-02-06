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

package cms

import (
	"testing"

	"github.com/erda-project/erda/pkg/strutil"
)

func TestMakeAppBranchPrefixSecretNamespace(t *testing.T) {
	type args struct {
		appID  string
		branch string
	}
	tests := []struct {
		name    string
		args    args
		want    string
		wantErr bool
	}{
		{
			name: "invalid branch",
			args: args{
				appID:  "1",
				branch: "xxx",
			},
			want:    "",
			wantErr: true,
		},
		{
			name: "develop",
			args: args{
				appID:  "1",
				branch: "develop",
			},
			want:    strutil.Concat(PipelineAppConfigNameSpacePrefix, "-", "1", "-", "develop"),
			wantErr: false,
		},
		{
			name: "release/1.4",
			args: args{
				appID:  "1",
				branch: "release/1.4",
			},
			want:    strutil.Concat(PipelineAppConfigNameSpacePrefix, "-", "1", "-", "release"),
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := MakeAppBranchPrefixSecretNamespace(tt.args.appID, tt.args.branch)
			if (err != nil) != tt.wantErr {
				t.Errorf("MakeAppBranchPrefixSecretNamespace() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("MakeAppBranchPrefixSecretNamespace() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestMakeAppBranchPrefixSecretNamespaceByBranchPrefix(t *testing.T) {
	type args struct {
		appID        string
		branchPrefix string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "invalid branch",
			args: args{
				appID:        "1",
				branchPrefix: "xxx",
			},
			want: strutil.Concat(PipelineAppConfigNameSpacePrefix, "-", "1", "-", "xxx"),
		},
		{
			name: "develop",
			args: args{
				appID:        "1",
				branchPrefix: "develop",
			},
			want: strutil.Concat(PipelineAppConfigNameSpacePrefix, "-", "1", "-", "develop"),
		},
		{
			name: "release/1.4",
			args: args{
				appID:        "1",
				branchPrefix: "release",
			},
			want: strutil.Concat(PipelineAppConfigNameSpacePrefix, "-", "1", "-", "release"),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := MakeAppBranchPrefixSecretNamespaceByBranchPrefix(tt.args.appID, tt.args.branchPrefix); got != tt.want {
				t.Errorf("MakeAppBranchPrefixSecretNamespaceByBranchPrefix() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestMakeAppDefaultSecretNamespace(t *testing.T) {
	type args struct {
		appID string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "app id",
			args: args{
				appID: "1",
			},
			want: strutil.Concat(PipelineAppConfigNameSpacePrefix, "-", "1", "-", "default"),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := MakeAppDefaultSecretNamespace(tt.args.appID); got != tt.want {
				t.Errorf("MakeAppDefaultSecretNamespace() = %v, want %v", got, tt.want)
			}
		})
	}
}
