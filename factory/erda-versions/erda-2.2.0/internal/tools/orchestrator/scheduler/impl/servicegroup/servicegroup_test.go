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

package servicegroup

import (
	"fmt"
	"strings"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/clusterinfo"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

func Test_convertServiceGroup(t *testing.T) {
	services := make(map[string]*diceyml.Service)
	jobs := make(map[string]*diceyml.Job)
	services["mysql-1"] = &diceyml.Service{
		Image: "registry.erda.cloud/erda-addons-enterprise/addon-mysql:5.7.29-1.0.1-init",
		Ports: make([]diceyml.ServicePort, 0),
		Envs:  make(map[string]string),
		Resources: diceyml.Resources{
			CPU: 1,
			Mem: 4301,
		},
		Labels:      make(map[string]string),
		Annotations: make(map[string]string),
		Binds:       make([]string, 0),
		Deployments: diceyml.Deployments{
			Replicas: 1,
		},
	}

	services["mysql-1"].Ports = append(services["mysql-1"].Ports, diceyml.ServicePort{
		Port:       3306,
		Protocol:   "TCP",
		L4Protocol: "TCP",
		Expose:     false,
		Default:    false,
	})

	services["mysql-1"].Envs = diceyml.EnvMap{
		"ADDON_GROUPS":        "2",
		"ADDON_ID":            "z44f5f6543f004d54ac2a2538efd4e9ec",
		"ADDON_NODE_ID":       "f54fc4ff4197e4c4fa1cdc5b929ca5849",
		"ADDON_TYPE":          "mysql",
		"DICE_ADDON":          "z44f5f6543f004d54ac2a2538efd4e9ec",
		"DICE_ADDON_TYPE":     "mysql",
		"DICE_CLUSTER_NAME":   "test",
		"MYSQL_ROOT_PASSWORD": "cR7yf6zEBVFQ8WgE",
		"SERVER_ID":           "1",
		"SERVICE_TYPE":        "ADDONS",
	}

	services["mysql-1"].Labels = map[string]string{
		"ADDON_GROUP_ID": "mysql-master",
	}

	services["mysql-1"].Binds = []string{
		"/netdata/addon/mysql/backup/z44f5f6543f004d54ac2a2538efd4e9ec_1:/var/backup/mysql:rw",
		"z44f5f6543f004d54ac2a2538efd4e9ec_1:/var/lib/mysql:rw",
	}
	services["mysql-1"].HealthCheck = diceyml.HealthCheck{
		Exec: &diceyml.ExecCheck{Cmd: fmt.Sprintf("mysql -uroot -p%s  -e 'select 1'", "xxxxxx")},
	}
	jobs["job-1"] = &diceyml.Job{
		Image: "registry.erda.cloud/job:5.7.29-1.0.1-init",
		Envs:  make(map[string]string),
		Resources: diceyml.Resources{
			CPU: 1,
			Mem: 4301,
		},
		Labels: make(map[string]string),
		Binds:  make([]string, 0),
	}
	req := apistructs.ServiceGroupCreateV2Request{
		DiceYml: diceyml.Object{
			Version:  "2.0",
			Services: services,
			Jobs:     jobs,
		},
		ClusterName: "test",
		ID:          "z44f5f6543f004d54ac2a2538efd4e9ec",
		Type:        strings.Join([]string{"addon-", strings.Replace(strings.Replace("mysql", "terminus-", "", 1), "-operator", "", 1)}, ""),
		GroupLabels: make(map[string]string),
		Volumes:     make(map[string]apistructs.RequestVolumeInfo),
	}

	volumes := make(map[string]apistructs.RequestVolumeInfo)
	volumes["mysql-1"] = apistructs.RequestVolumeInfo{
		ID:            "101",
		Type:          "local",
		ContainerPath: "/opt/test",
	}
	req.Volumes = volumes

	type args struct {
		req         apistructs.ServiceGroupCreateV2Request
		clusterinfo clusterinfo.ClusterInfo
	}

	js, _ := jsonstore.New()
	clusterinfoImpl := clusterinfo.NewClusterInfoImpl(js)
	addr := convertHealthcheck(services["mysql-1"].HealthCheck)

	tests := []struct {
		name    string
		args    args
		want    apistructs.ServiceGroup
		wantErr bool
	}{
		{
			name: "Test_01",
			args: args{
				req:         req,
				clusterinfo: clusterinfoImpl,
			},
			want: apistructs.ServiceGroup{
				ClusterName:   "test",
				Force:         true,
				ScheduleInfo:  apistructs.ScheduleInfo{},
				ScheduleInfo2: apistructs.ScheduleInfo2{},
				Dice: apistructs.Dice{
					ID:     "z44f5f6543f004d54ac2a2538efd4e9ec",
					Type:   "addon-mysql",
					Labels: map[string]string{},
					Services: []apistructs.Service{
						{
							Name:          "mysql-1",
							Namespace:     "",
							Image:         "registry.erda.cloud/erda-addons-enterprise/addon-mysql:5.7.29-1.0.1-init",
							ImageUsername: "",
							ImagePassword: "",
							Cmd:           "",
							Ports:         []diceyml.ServicePort{{Port: 3306, Protocol: "TCP", L4Protocol: "TCP", Expose: false, Default: false}},
							Scale:         1,
							Resources:     apistructs.Resources{Cpu: 1, Mem: 4301, MaxCPU: 0, MaxMem: 0, Disk: 0},
							Env: map[string]string{
								"ADDON_GROUPS":        "2",
								"ADDON_ID":            "z44f5f6543f004d54ac2a2538efd4e9ec",
								"ADDON_NODE_ID":       "f54fc4ff4197e4c4fa1cdc5b929ca5849",
								"ADDON_TYPE":          "mysql",
								"DICE_ADDON":          "z44f5f6543f004d54ac2a2538efd4e9ec",
								"DICE_ADDON_TYPE":     "mysql",
								"DICE_CLUSTER_NAME":   "test",
								"MYSQL_ROOT_PASSWORD": "cR7yf6zEBVFQ8WgE",
								"SERVER_ID":           "1", "SERVICE_TYPE": "ADDONS",
							},
							Labels: map[string]string{"ADDON_GROUP_ID": "mysql-master"},
							Binds: []apistructs.ServiceBind{{
								Bind: apistructs.Bind{
									ContainerPath: "/var/backup/mysql",
									HostPath:      "/netdata/addon/mysql/backup/z44f5f6543f004d54ac2a2538efd4e9ec_1",
									ReadOnly:      false,
								},
							}, {
								Bind: apistructs.Bind{
									ContainerPath: "/var/lib/mysql",
									HostPath:      "z44f5f6543f004d54ac2a2538efd4e9ec_1",
									ReadOnly:      false,
								},
							},
							},
							Volumes: []apistructs.Volume{{
								ID:            "101",
								VolumePath:    "",
								VolumeType:    "local",
								Size:          10,
								ContainerPath: "/opt/test",
								Storage:       "",
							},
							},
							NewHealthCheck: addr,
						},
					},
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			setServiceGroupExecutorByClusterPatch := monkey.Patch(setServiceGroupExecutorByCluster, func(sg *apistructs.ServiceGroup, clusterinfo clusterinfo.ClusterInfo) error {
				return nil
			})
			convertHealthcheckPatch := monkey.Patch(convertHealthcheck, func(hc diceyml.HealthCheck) *apistructs.NewHealthCheck {
				return addr
			})

			setServiceVolumesPatch := monkey.Patch(setServiceVolumes, func(clusterName string, vs diceyml.Volumes, clusterinfo clusterinfo.ClusterInfo, enableECI bool) ([]apistructs.Volume, error) {
				return []apistructs.Volume{}, nil
			})

			defer setServiceVolumesPatch.Unpatch()
			defer setServiceGroupExecutorByClusterPatch.Unpatch()
			defer convertHealthcheckPatch.Unpatch()

			got, err := convertServiceGroup(tt.args.req, tt.args.clusterinfo)
			if (err != nil) != tt.wantErr {
				t.Errorf("convertServiceGroup() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			assert.Equal(t, got.Services[0].Name, tt.want.Services[0].Name)
		})
	}
}
