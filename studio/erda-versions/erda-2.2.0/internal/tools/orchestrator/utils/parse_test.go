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

package utils

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
)

func TestParseOrder(t *testing.T) {
	uuid := "07f3023d-46bb-49de-b139-c775c6881636"
	assert.Equal(t, ParseOrderName(uuid), "07f302")
}

func TestParseDeploymentOrderStatus(t *testing.T) {
	type args struct {
		DeploymentStatus apistructs.DeploymentOrderStatusMap
	}
	tests := []struct {
		name string
		args args
		want apistructs.DeploymentOrderStatus
	}{
		{
			name: "apps-1",
			args: args{
				DeploymentStatus: apistructs.DeploymentOrderStatusMap{
					"app-1": apistructs.DeploymentOrderStatusItem{
						DeploymentStatus: apistructs.DeploymentStatusWaiting,
					},
					"app-2": apistructs.DeploymentOrderStatusItem{
						DeploymentStatus: apistructs.DeploymentStatusOK,
					},
				},
			},
			want: apistructs.DeploymentOrderStatus(apistructs.DeploymentStatusDeploying),
		},
		{
			name: "apps-2",
			args: args{
				DeploymentStatus: apistructs.DeploymentOrderStatusMap{
					"app-1": apistructs.DeploymentOrderStatusItem{
						DeploymentStatus: apistructs.DeploymentStatusFailed,
					},
					"app-2": apistructs.DeploymentOrderStatusItem{
						DeploymentStatus: apistructs.DeploymentStatusOK,
					},
				},
			},
			want: apistructs.DeploymentOrderStatus(apistructs.DeploymentStatusFailed),
		},
		{
			name: "apps-3",
			args: args{
				DeploymentStatus: nil,
			},
			want: apistructs.DeploymentOrderStatus(apistructs.DeployStatusWaitDeploy),
		},
		{
			name: "apps-4",
			args: args{
				DeploymentStatus: apistructs.DeploymentOrderStatusMap{
					"app-1": apistructs.DeploymentOrderStatusItem{
						DeploymentStatus: apistructs.DeploymentStatusCanceling,
					},
					"app-2": apistructs.DeploymentOrderStatusItem{
						DeploymentStatus: apistructs.DeploymentStatusOK,
					},
				},
			},
			want: apistructs.DeploymentOrderStatus(apistructs.DeploymentStatusCanceled),
		},
		{
			name: "apps-5",
			args: args{
				DeploymentStatus: apistructs.DeploymentOrderStatusMap{
					"app-1": apistructs.DeploymentOrderStatusItem{
						DeploymentStatus: apistructs.DeploymentStatusOK,
					},
				},
			},
			want: apistructs.DeploymentOrderStatus(apistructs.DeploymentStatusOK),
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := ParseDeploymentOrderStatus(tt.args.DeploymentStatus)

			if tt.want != got {
				t.Errorf("parseDeploymentOrderStatus got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestParseDeploymentStatus(t *testing.T) {
	type args struct {
		DeploymentStatus apistructs.DeploymentStatus
	}
	tests := []struct {
		name string
		args args
		want apistructs.DeploymentStatus
	}{
		{
			name: "apps-1",
			args: args{
				DeploymentStatus: apistructs.DeploymentStatusWaiting,
			},
			want: apistructs.DeploymentStatusDeploying,
		},
		{
			name: "apps-2",
			args: args{
				DeploymentStatus: apistructs.DeployStatusWaitDeploy,
			},
			want: apistructs.DeployStatusWaitDeploy,
		},
		{
			name: "apps-3",
			args: args{
				DeploymentStatus: apistructs.DeploymentStatusCanceling,
			},
			want: apistructs.DeploymentStatusCanceled,
		},
		{
			name: "apps-4",
			args: args{
				DeploymentStatus: apistructs.DeploymentStatusCanceled,
			},
			want: apistructs.DeploymentStatusCanceled,
		},
		{
			name: "apps-5",
			args: args{
				DeploymentStatus: apistructs.DeploymentStatusFailed,
			},
			want: apistructs.DeploymentStatusFailed,
		},
		{
			name: "apps-6",
			args: args{
				DeploymentStatus: apistructs.DeploymentStatusOK,
			},
			want: apistructs.DeploymentStatusOK,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := ParseDeploymentStatus(tt.args.DeploymentStatus)

			if tt.want != got {
				t.Errorf("parseDeploymentOrderStatus got = %v, want %v", got, tt.want)
			}
		})
	}
}
