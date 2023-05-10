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

package addon

import (
	"reflect"
	"strings"
	"sync"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/servicegroup"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

const count = 20

func TestConcurrentReadWriteAppInfos(t *testing.T) {
	var keys = []string{"1", "2", "3", "4", "5"}

	var db *dbclient.DBClient
	monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetAttachmentsByInstanceID",
		func(*dbclient.DBClient, string) (*[]dbclient.AddonAttachment, error) {
			var addons []dbclient.AddonAttachment
			for _, v := range keys {
				addons = append(addons, dbclient.AddonAttachment{
					ProjectID:     v,
					ApplicationID: v,
				})
			}
			return &addons, nil
		},
	)
	defer monkey.UnpatchAll()

	var bdl *bundle.Bundle
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetAppsByProject",
		func(_ *bundle.Bundle, id uint64, _ uint64, _ string) (*apistructs.ApplicationListResponseData, error) {
			return &apistructs.ApplicationListResponseData{
				List: []apistructs.ApplicationDTO{
					{
						ID: id,
					},
				},
			}, nil
		},
	)

	var (
		wg         sync.WaitGroup
		orgID      uint64 = 1
		userID            = "1"
		instanceID        = "1"
	)
	wg.Add(count)
	for i := 0; i != count; i++ {
		go func() {
			a := Addon{}
			_, err := a.ListReferencesByInstanceID(orgID, userID, instanceID)
			if err != nil {
				t.Fatal(err)
			}
			wg.Done()
		}()
	}
	wg.Wait()
	for _, v := range keys {
		_, ok := AppInfos.Load(v)
		assert.Equal(t, true, ok)
	}
}

func TestDeleteAddonUsed(t *testing.T) {
	var db *dbclient.DBClient
	monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetInstanceRouting",
		func(*dbclient.DBClient, string) (*dbclient.AddonInstanceRouting, error) {
			return &dbclient.AddonInstanceRouting{}, nil
		},
	)

	addon := Addon{}
	monkey.PatchInstanceMethod(reflect.TypeOf(&addon), "DeleteTenant",
		func(*Addon, string, string) error {
			return nil
		},
	)

	monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetAttachmentCountByRoutingInstanceID",
		func(*dbclient.DBClient, string) (int64, error) {
			return 1, nil
		},
	)
	defer monkey.UnpatchAll()

	err := addon.Delete("", "")
	if err.Error() != "addon is being referenced, can't delete" {
		t.Fatal("the err is not equal with expected")
	}
}

func Test_GetAddonConfig(t *testing.T) {
	cfgStr := `{"ADDON_HAS_ENCRIPY":"YES","MYSQL_DATABASE":"fake","MYSQL_HOST":"fake","MYSQL_PASSWORD":"fake1","MYSQL_PORT":"fake","MYSQL_USERNAME":"fake"}`
	cfg, err := GetAddonConfig(cfgStr)
	assert.NoError(t, err)
	assert.Equal(t, "fake1", cfg["MYSQL_PASSWORD"].(string))
}

func TestSetAddonVolumes(t *testing.T) {
	type args struct {
		service    *diceyml.Service
		options    map[string]string
		hostPath   string
		targetPath string
		readOnly   bool
	}

	service := &diceyml.Service{}
	options := map[string]string{
		"app_kind":             "deployment",
		"alibabacloud.com/eci": "true",
	}

	tests := []struct {
		name string
		args args
	}{
		{
			name: "case_01",
			args: args{
				service:    service,
				options:    options,
				hostPath:   "/opt/data",
				targetPath: "/opt/data",
				readOnly:   false,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
		})
	}
}

func Test_addonCanScale(t *testing.T) {
	type args struct {
		addonName    string
		addonId      string
		action       string
		status       string
		addonPlan    string
		addonVersion string
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		// TODO: Add test cases.
		{
			name: "Test_01",
			args: args{
				addonName:    "mysql",
				addonId:      "z44f5f6543f004d54ac2a2538efd4e9ec",
				action:       apistructs.ScaleActionDown,
				status:       string(apistructs.AddonAttached),
				addonPlan:    "basic",
				addonVersion: "5.7.29",
			},
			wantErr: false,
		},
		{
			name: "Test_02",
			args: args{
				addonName:    "mysql",
				addonId:      "z44f5f6543f004d54ac2a2538efd4e9ec",
				action:       apistructs.ScaleActionUp,
				status:       string(apistructs.AddonAttached),
				addonPlan:    "basic",
				addonVersion: "5.7.29",
			},
			wantErr: true,
		},
		{
			name: "Test_03",
			args: args{
				addonName:    "mysql",
				addonId:      "z44f5f6543f004d54ac2a2538efd4e9ec",
				action:       apistructs.ScaleActionUp,
				status:       string(apistructs.AddonAttached),
				addonPlan:    "basic",
				addonVersion: "5.7.29",
			},
			wantErr: true,
		},
		{
			name: "Test_04",
			args: args{
				addonName:    "mysql",
				addonId:      "z44f5f6543f004d54ac2a2538efd4e9ec",
				action:       apistructs.ScaleActionUp,
				status:       string(apistructs.AddonOffline),
				addonPlan:    "basic",
				addonVersion: "5.7.29",
			},
			wantErr: false,
		},
		{
			name: "Test_05",
			args: args{
				addonName:    "mysql",
				addonId:      "z44f5f6543f004d54ac2a2538efd4e9ec",
				action:       apistructs.ScaleActionUp,
				status:       string(apistructs.AddonOffline),
				addonPlan:    "professional",
				addonVersion: "5.7.29",
			},
			wantErr: false,
		},
		{
			name: "Test_06",
			args: args{
				addonName:    "redis",
				addonId:      "z44f5f6543f004d54ac2a2538efd4e9ec",
				action:       apistructs.ScaleActionUp,
				status:       string(apistructs.AddonOffline),
				addonPlan:    "professional",
				addonVersion: "5.7.29",
			},
			wantErr: false,
		},
		{
			name: "Test_07",
			args: args{
				addonName:    "terminus-elasticsearch",
				addonId:      "z44f5f6543f004d54ac2a2538efd4e9ec",
				action:       apistructs.ScaleActionUp,
				status:       string(apistructs.AddonOffline),
				addonPlan:    "professional",
				addonVersion: "5.7.29",
			},
			wantErr: false,
		},
		{
			name: "Test_08",
			args: args{
				addonName:    "terminus-elasticsearch",
				addonId:      "z44f5f6543f004d54ac2a2538efd4e9ec",
				action:       apistructs.ScaleActionUp,
				status:       string(apistructs.AddonOffline),
				addonPlan:    "professional",
				addonVersion: "6.8.9",
			},
			wantErr: true,
		},
		{
			name: "Test_09",
			args: args{
				addonName:    "mysql",
				addonId:      "z44f5f6543f004d54ac2a2538efd4e9ec",
				action:       apistructs.ScaleActionDown,
				status:       string(apistructs.AddonOffline),
				addonPlan:    "professional",
				addonVersion: "5.7.29",
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := addonCanScale(tt.args.addonName, tt.args.addonId, tt.args.addonPlan, tt.args.addonVersion, tt.args.status, tt.args.action); (err != nil) != tt.wantErr {
				t.Errorf("addonCanScale() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func TestAddon_doAddonScale(t *testing.T) {
	var db *dbclient.DBClient
	var bdl *bundle.Bundle
	var serviceGroupImpl *servicegroup.ServiceGroupImpl

	a := New(WithDBClient(db), WithBundle(bdl), WithServiceGroup(serviceGroupImpl))
	addonInstance := &dbclient.AddonInstance{
		ID:                  "z44f5f6543f004d54ac2a2538efd4e9ec",
		Name:                "mysql",
		AddonName:           "mysql",
		Category:            "database",
		Namespace:           "addon-mysql",
		ScheduleName:        "z44f5f6543f004d54ac2a2538efd4e9ec",
		Plan:                "professional",
		Version:             "5.7.29",
		Options:             "{\"applicationId\":\"21\",\"clusterName\":\"test\",\"orgId\":\"1\",\"projectId\":\"1\",\"runtimeId\":\"192\",\"runtimeName\":\"feature/develop\",\"version\":\"5.7.29\",\"workspace\":\"DEV\"}",
		Status:              "ATTACHED",
		ShareScope:          "PROJECT",
		OrgID:               "1",
		Cluster:             "test",
		ProjectID:           "1",
		ApplicationID:       "21",
		AppID:               "1",
		Workspace:           "DEV",
		Deleted:             "N",
		PlatformServiceType: 0,
		KmsKey:              "f2dcc7b3761d4244898303cb0104a584",
		CpuRequest:          0.4,
		CpuLimit:            4,
		MemRequest:          17204,
		MemLimit:            17204,
	}

	addonInstanceRoutings := make([]dbclient.AddonInstanceRouting, 0)

	monkey.PatchInstanceMethod(reflect.TypeOf(a), "GetAddonExtention",
		func(a *Addon, params *apistructs.AddonHandlerCreateItem) (*apistructs.AddonExtension, *diceyml.Object, error) {
			return &apistructs.AddonExtension{}, &diceyml.Object{}, nil
		},
	)

	monkey.PatchInstanceMethod(reflect.TypeOf(a), "BuildAddonScaleRequestGroup",
		func(a *Addon, params *apistructs.AddonHandlerCreateItem, addonIns *dbclient.AddonInstance, scaleAction string, addonSpec *apistructs.AddonExtension, addonDice *diceyml.Object) (*apistructs.ServiceGroup, error) {
			services := make(map[string]*diceyml.Service)
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

			services["mysql-2"] = &diceyml.Service{
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

			services["mysql-2"].Ports = append(services["mysql-2"].Ports, diceyml.ServicePort{
				Port:       3306,
				Protocol:   "TCP",
				L4Protocol: "TCP",
				Expose:     false,
				Default:    false,
			})

			services["mysql-2"].Envs = diceyml.EnvMap{
				"ADDON_GROUPS":        "2",
				"ADDON_ID":            "z44f5f6543f004d54ac2a2538efd4e9ec",
				"ADDON_NODE_ID":       "m6475b57e54884af59e4147382964f7ab",
				"ADDON_TYPE":          "mysql",
				"DICE_ADDON":          "z44f5f6543f004d54ac2a2538efd4e9ec",
				"DICE_ADDON_TYPE":     "mysql",
				"DICE_CLUSTER_NAME":   "test",
				"MYSQL_ROOT_PASSWORD": "cR7yf6zEBVFQ8WgE",
				"SERVER_ID":           "2",
				"SERVICE_TYPE":        "ADDONS",
			}

			services["mysql-2"].Labels = map[string]string{
				"ADDON_GROUP_ID": "mysql-slave",
			}

			services["mysql-2"].Binds = []string{
				"/netdata/addon/mysql/backup/z44f5f6543f004d54ac2a2538efd4e9ec_2:/var/backup/mysql:rw",
				"z44f5f6543f004d54ac2a2538efd4e9ec_2:/var/lib/mysql:rw",
			}

			req := &apistructs.ServiceGroupCreateV2Request{
				DiceYml: diceyml.Object{
					Version:  "2.0",
					Services: services,
				},
				ClusterName: "test",
				ID:          addonIns.ID,
				Type:        strings.Join([]string{"addon-", strings.Replace(strings.Replace(addonIns.AddonName, "terminus-", "", 1), "-operator", "", 1)}, ""),
				GroupLabels: make(map[string]string),
				Volumes:     make(map[string]apistructs.RequestVolumeInfo),
			}

			ret := &apistructs.UpdateServiceGroupScaleRequest{
				Namespace:   addonIns.Namespace,
				Name:        addonIns.ScheduleName,
				ClusterName: params.ClusterName,
				Labels:      make(map[string]string),
				Services:    make([]apistructs.Service, 0),
			}
			ret.Labels = req.GroupLabels

			for svcName, svc := range req.DiceYml.Services {
				scale := 0
				if scaleAction == apistructs.ScaleActionDown {
					scale = 0
				}
				if scaleAction == apistructs.ScaleActionUp {
					// TODO: 从数据库表获取前一次 scaleUp 成功之后的 replicas
					scale = svc.Deployments.Replicas
				}

				ret.Services = append(ret.Services, apistructs.Service{
					Name:  svcName,
					Scale: scale,
					Resources: apistructs.Resources{
						Cpu:  svc.Resources.CPU,
						Mem:  float64(svc.Resources.Mem),
						Disk: float64(svc.Resources.Disk),
					},
				})
			}
			return &apistructs.ServiceGroup{}, nil
		},
	)

	monkey.PatchInstanceMethod(reflect.TypeOf(serviceGroupImpl), "Scale",
		func(_ *servicegroup.ServiceGroupImpl, sg *apistructs.ServiceGroup) (apistructs.ServiceGroup, error) {
			return apistructs.ServiceGroup{}, nil
		},
	)

	monkey.PatchInstanceMethod(reflect.TypeOf(a.db), "UpdateAddonInstanceStatus",
		func(_ *dbclient.DBClient, ID, status string) error {
			return nil
		},
	)

	monkey.PatchInstanceMethod(reflect.TypeOf(a.db), "UpdateInstanceRouting",
		func(db *dbclient.DBClient, routing *dbclient.AddonInstanceRouting) error {
			return nil
		},
	)

	err := a.doAddonScale(addonInstance, &addonInstanceRoutings, apistructs.ScaleActionDown)
	assert.Equal(t, err, nil)
}

func TestAddon_insideAddonCanNotScale(t *testing.T) {
	type fields struct {
		db *dbclient.DBClient
	}
	type args struct {
		routingIns *dbclient.AddonInstanceRouting
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			name: "test_01",
			fields: fields{
				db: &dbclient.DBClient{},
			},
			args: args{
				routingIns: &dbclient.AddonInstanceRouting{
					AddonName:    "terminus-zookeeper",
					RealInstance: "y6f6485f7d9974c32b47c3a1ecd244109",
				},
			},
			wantErr: true,
		},
		{
			name: "test_02",
			fields: fields{
				db: &dbclient.DBClient{},
			},
			args: args{
				routingIns: &dbclient.AddonInstanceRouting{
					AddonName:    "mysql",
					RealInstance: "y6f6485f7d9974c32b47c3a1ecd244109",
				},
			},
			wantErr: true,
		},
		{
			name: "test_03",
			fields: fields{
				db: &dbclient.DBClient{},
			},
			args: args{
				routingIns: &dbclient.AddonInstanceRouting{
					AddonName:    "mysql",
					RealInstance: "y6f6485f7d9974c32b47c3a1ecd244109",
				},
			},
			wantErr: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			a := &Addon{
				db: tt.fields.db,
			}

			monkey.PatchInstanceMethod(reflect.TypeOf(a.db), "GetByInSideInstanceID",
				func(db *dbclient.DBClient, instanceID string) (*dbclient.AddonInstanceRelation, error) {
					if tt.name == "test_02" {
						return nil, nil
					}
					return &dbclient.AddonInstanceRelation{
						OutsideInstanceID: "c32a40074138a4910af97cff325f8bcd5",
						InsideInstanceID:  "y6f6485f7d9974c32b47c3a1ecd244109",
					}, nil
				},
			)

			monkey.PatchInstanceMethod(reflect.TypeOf(a.db), "GetInstanceRoutingByRealInstance",
				func(db *dbclient.DBClient, realIns string) (*[]dbclient.AddonInstanceRouting, error) {
					if tt.name == "test_03" {
						return nil, nil
					}
					ars := make([]dbclient.AddonInstanceRouting, 0)
					ars = append(ars, dbclient.AddonInstanceRouting{
						Name: "kafka",
						ID:   "g3dd89da1f63245a3b2b174d9610661bd",
					})
					return &ars, nil
				},
			)

			if err := a.insideAddonCanNotScale(tt.args.routingIns); (err != nil) != tt.wantErr {
				t.Errorf("insideAddonCanNotScale() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
