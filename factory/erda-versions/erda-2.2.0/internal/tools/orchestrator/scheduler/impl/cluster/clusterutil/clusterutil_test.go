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

package clusterutil

/*
import (
	"testing"

	"github.com/erda-project/erda/apistructs"
)

func TestSetJobExecutorByCluster(t *testing.T) {
	type args struct {
		job *apistructs.Job
	}

	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "Test_01",
			args: args{
				job: &apistructs.Job{
					JobFromUser: apistructs.JobFromUser{
						Executor:    "",
						ClusterName: "",
						Kind:        JobKindK8S,
					},
				},
			},
			wantErr: true,
		},
		{
			name: "Test_02",
			args: args{
				job: &apistructs.Job{
					JobFromUser: apistructs.JobFromUser{
						Executor:    "",
						ClusterName: "Kubernetes",
						Kind:        JobKindK8S,
					},
				},
			},
			wantErr: false,
		},
		{
			name: "Test_03",
			args: args{
				job: &apistructs.Job{
					JobFromUser: apistructs.JobFromUser{
						Executor:    "",
						ClusterName: "Kubernetes",
					},
				},
			},
			wantErr: false,
		},
		{
			name: "Test_04",
			args: args{
				job: &apistructs.Job{
					JobFromUser: apistructs.JobFromUser{
						Executor:    "",
						ClusterName: "xhsd-t2",
					},
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := SetJobExecutorByCluster(tt.args.job); (err != nil) != tt.wantErr {
				t.Errorf("SetJobExecutorByCluster() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func TestSetJobVolumeExecutorByCluster(t *testing.T) {
	type args struct {
		jobvolume *apistructs.JobVolume
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "Test_01",
			args: args{
				jobvolume: &apistructs.JobVolume{
					Executor:    "",
					ClusterName: "Kubernetes",
					Kind:        JobKindK8S,
				},
			},
			wantErr: false,
		},
		{
			name: "Test_02",
			args: args{
				jobvolume: &apistructs.JobVolume{
					Executor:    "",
					ClusterName: "",
					Kind:        JobKindK8S,
				},
			},
			wantErr: true,
		},
		{
			name: "Test_03",
			args: args{
				jobvolume: &apistructs.JobVolume{
					Executor:    "",
					ClusterName: "Kubernetes",
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := SetJobVolumeExecutorByCluster(tt.args.jobvolume); (err != nil) != tt.wantErr {
				t.Errorf("SetJobVolumeExecutorByCluster() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
*/
