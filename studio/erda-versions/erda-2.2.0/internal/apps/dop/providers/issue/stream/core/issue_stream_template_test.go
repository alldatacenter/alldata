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

package core

import (
	"testing"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/stream/common"
)

func Test_getIssueStreamTemplate(t *testing.T) {
	type args struct {
		locale string
		ist    string
	}
	tests := []struct {
		name    string
		args    args
		want    string
		wantErr bool
	}{
		{
			name: "zh exist",
			args: args{
				locale: "zh",
				ist:    common.ISTChangeIteration,
			},
			want:    apistructs.IssueTemplate["zh"][apistructs.ISTChangeIteration],
			wantErr: false,
		},
		{
			name: "en exist",
			args: args{
				locale: "en",
				ist:    common.ISTComment,
			},
			want:    apistructs.IssueTemplate["zh"][apistructs.ISTComment],
			wantErr: false,
		},
		{
			name: "zh not exist",
			args: args{
				locale: "zh",
				ist:    "not exist",
			},
			wantErr: true,
		},
		{
			name: "en not exist",
			args: args{
				locale: "en",
				ist:    "not exist",
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := getIssueStreamTemplate(tt.args.locale, tt.args.ist)
			if (err != nil) != tt.wantErr {
				t.Errorf("getIssueStreamTemplate() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("getIssueStreamTemplate() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_getIssueStreamTemplateForMsgSending(t *testing.T) {
	type args struct {
		locale string
		ist    string
	}
	tests := []struct {
		name    string
		args    args
		want    string
		wantErr bool
	}{
		{
			name: "zh override exist",
			args: args{
				locale: "zh",
				ist:    common.ISTComment,
			},
			want:    apistructs.IssueTemplateOverrideForMsgSending["zh"][apistructs.ISTComment],
			wantErr: false,
		},
		{
			name: "zh exist, but override not exist",
			args: args{
				locale: "zh",
				ist:    common.ISTChangeIteration,
			},
			want:    apistructs.IssueTemplate["zh"][apistructs.ISTChangeIteration],
			wantErr: false,
		},
		{
			name: "zh not exist",
			args: args{
				locale: "zh",
				ist:    "not exist",
			},
			wantErr: true,
		},
		{
			name: "en override exist",
			args: args{
				locale: "en",
				ist:    common.ISTComment,
			},
			want:    apistructs.IssueTemplateOverrideForMsgSending["en"][apistructs.ISTComment],
			wantErr: false,
		},
		{
			name: "en exist, but override not exist",
			args: args{
				locale: "en",
				ist:    common.ISTChangeIteration,
			},
			want:    apistructs.IssueTemplate["en"][apistructs.ISTChangeIteration],
			wantErr: false,
		},
		{
			name: "en not exist",
			args: args{
				locale: "en",
				ist:    "not exist",
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := getIssueStreamTemplateForMsgSending(tt.args.locale, tt.args.ist)
			if (err != nil) != tt.wantErr {
				t.Errorf("getIssueStreamTemplateForMsgSending() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("getIssueStreamTemplateForMsgSending() got = %v, want %v", got, tt.want)
			}
		})
	}
}
