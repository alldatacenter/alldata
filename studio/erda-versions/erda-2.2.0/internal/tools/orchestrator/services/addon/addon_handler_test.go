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
	"fmt"
	"math"
	"reflect"
	"strconv"
	"strings"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/components/addon/mysql"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/servicegroup"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/log"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/resource"
	"github.com/erda-project/erda/internal/tools/orchestrator/utils"
	"github.com/erda-project/erda/pkg/crypto/encryption"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

func TestUnixTimeFormat(t *testing.T) {
	fmt.Println(time.Now().Format("2006-01-02")) //2018-7-15 15:23:00
}

func TestOne(t *testing.T) {
	a := 2.0000
	b := 1.60000
	c := 0.30000
	fmt.Println(Smaller(a-b, c))
}

func Smaller(a, b float64) bool {
	return math.Max(a, b) == b && math.Abs(a-b) > 0.00001
}

func TestSplitN(t *testing.T) {
	image := "addon-registry.default.svc.cluster.local:5000/terminus-customer-engagement/acl-addon-demo:migration-1584515323233379060"
	ss := strings.SplitN(image, "/", 2)
	if len(ss) == 2 {
		repo := strings.Split(ss[1], ":")[0]
		var repoTag string
		if strings.Contains(ss[1], ":") {
			repoTag = strings.Split(ss[1], ":")[1]
		} else {
			repoTag = "latest"
		}
		fmt.Printf("repo: %s, repoTag: %s", repo, repoTag)
	}

}

func TestSubTime(t *testing.T) {
	t1 := "2019-12-01 18:00:30"
	timeTemplate1 := "2006-01-02 15:04:05" //常规类型
	stamp, _ := time.ParseInLocation(timeTemplate1, t1, time.Local)

	now := time.Now()
	subM := now.Sub(stamp)
	fmt.Println(subM.Minutes(), "分钟")
}

func TestTransPlan(t *testing.T) {
	var addons = make([]apistructs.AddonCreateItem, 2)
	addons[0] = apistructs.AddonCreateItem{
		Name: "1111",
		Type: "mysql",
		Plan: "small",
	}
	addons[1] = apistructs.AddonCreateItem{
		Name: "222",
		Type: "redis",
		Plan: "small",
	}
	fmt.Printf("-------%+v", addons)
	fmt.Println("")
	addonsss := transPlan(&addons)
	fmt.Printf("=======%+v", addonsss)
}

func transPlan(addons *[]apistructs.AddonCreateItem) *[]apistructs.AddonCreateItem {
	if len(*addons) == 0 {
		return nil
	}
	var addon_result = make([]apistructs.AddonCreateItem, 0, len(*addons))
	for _, v := range *addons {
		addon_item := apistructs.AddonCreateItem{
			Name: v.Name,
			Type: v.Type,
			Plan: v.Plan,
		}
		switch v.Plan {
		case "large", apistructs.AddonUltimate:
			addon_item.Plan = apistructs.AddonUltimate
		case "medium", apistructs.AddonProfessional:
			addon_item.Plan = apistructs.AddonProfessional
		case "small", apistructs.AddonBasic:
			addon_item.Plan = apistructs.AddonBasic
		default:
			addon_item.Plan = apistructs.AddonBasic
		}
		addon_result = append(addon_result, addon_item)
	}
	return &addon_result
}

func TestOptionsMap(t *testing.T) {
	fmt.Println(float64(895))
	fmt.Println(strconv.ParseFloat(fmt.Sprintf("%.2f", float64(0.3)), 64))
}

func TestConfigMap(t *testing.T) {
	var configMap = map[string]interface{}{}
	configMap["MYSQL_HOST"] = "123456"
	configMap["MYSQL_PORT"] = 3306.00
	fmt.Println(reflect.TypeOf(configMap["MYSQL_PORT"]).String())
}

func TestBuild(t *testing.T) {
	//fmt.Print(fmt.Sprintf("%.f", 2048*0.7))
	aa := StructToMap(apistructs.AddonCreateOptions{
		ClusterName: "terminus-dev",
		OrgName:     "terminus",
	}, 0, "json")

	fmt.Printf("sssssss----%+v", &aa)

}

func StructToMap(data interface{}, depth int, tag ...string) map[string]interface{} {
	m := make(map[string]interface{})
	values := reflect.ValueOf(data)
	types := reflect.TypeOf(data)
	for types.Kind() == reflect.Ptr {
		values = values.Elem()
		types = types.Elem()
	}
	num := types.NumField()
	depth = depth - 1
	if len(tag) <= 0 || tag[0] == "" {
		if depth == -1 {
			for i := 0; i < num; i++ {
				t := types.Field(i)
				v := values.Field(i)
				if v.CanInterface() {
					m[t.Name] = v.Interface()
				}
			}
		} else {
			for i := 0; i < num; i++ {
				t := types.Field(i)
				v := values.Field(i)
				v_struct := v
				v_struct_ptr := v
				for v_struct.Kind() == reflect.Ptr {
					v_struct_ptr = v_struct
					v_struct = v_struct.Elem()
				}
				if v_struct.Kind() == reflect.Struct && v_struct_ptr.CanInterface() {
					m[t.Name] = StructToMap(v_struct_ptr.Interface(), depth, tag[0])
				} else {
					if v.CanInterface() {
						m[t.Name] = v.Interface()
					}
				}
			}
		}
	} else {
		tagName := tag[0]
		if depth == -1 {
			for i := 0; i < num; i++ {
				t := types.Field(i)
				v := values.Field(i)
				tagVal := t.Tag.Get(tagName)
				if v.CanInterface() && tagVal != "" && tagVal != "-" {
					m[tagVal] = v.Interface()
				}
			}
		} else {
			for i := 0; i < num; i++ {
				t := types.Field(i)
				v := values.Field(i)
				tagVal := t.Tag.Get(tagName)
				if tagVal != "" && tagVal != "-" {
					v_struct := v
					v_struct_ptr := v
					for v_struct.Kind() == reflect.Ptr {
						v_struct_ptr = v_struct
						v_struct = v_struct.Elem()
					}
					if v_struct.Kind() == reflect.Struct && v_struct_ptr.CanInterface() {
						m[tagVal] = StructToMap(v_struct_ptr.Interface(), depth, tag[0])
						continue
					}
					if v.CanInterface() {
						m[tagVal] = v.Interface()
					}
				}
			}
		}
	}
	return m
}

func TestPreCheck(t *testing.T) {
	tt := []struct {
		Params *apistructs.AddonHandlerCreateItem
		Want   bool
	}{
		{&apistructs.AddonHandlerCreateItem{
			Plan:      "professional",
			Workspace: "PROD",
		}, true},
		{&apistructs.AddonHandlerCreateItem{
			Plan:      "basic",
			Workspace: "DEV",
		}, true},
		{&apistructs.AddonHandlerCreateItem{
			Plan:      "basic",
			Workspace: "PROD",
		}, false},
	}
	var a Addon
	for _, v := range tt {
		assert.Equal(t, v.Want, a.preCheck(v.Params) == nil)
	}
}

func TestAddon_basicAddonDeploy(t *testing.T) {
	type fields struct {
		db               *dbclient.DBClient
		bdl              *bundle.Bundle
		hc               *httpclient.HTTPClient
		encrypt          *encryption.EnvEncrypt
		resource         *resource.Resource
		kms              mysql.KMSWrapper
		Logger           *log.DeployLogHelper
		serviceGroupImpl servicegroup.ServiceGroup
	}
	type args struct {
		addonIns        *dbclient.AddonInstance
		addonInsRouting *dbclient.AddonInstanceRouting
		params          *apistructs.AddonHandlerCreateItem
		addonSpec       *apistructs.AddonExtension
		addonDice       *diceyml.Object
		vendor          string
	}

	testfileds := fields{
		db:               &dbclient.DBClient{},
		bdl:              &bundle.Bundle{},
		hc:               &httpclient.HTTPClient{},
		encrypt:          &encryption.EnvEncrypt{},
		resource:         &resource.Resource{},
		Logger:           &log.DeployLogHelper{},
		serviceGroupImpl: &servicegroup.ServiceGroupImpl{},
	}

	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		// TODO: Add test cases.
		{
			name:   "Test_01",
			fields: testfileds,
			args: args{
				addonIns: &dbclient.AddonInstance{
					ProjectID: "1",
					OrgID:     "1",
					Workspace: "DEV",
				},
				addonInsRouting: &dbclient.AddonInstanceRouting{},
				params: &apistructs.AddonHandlerCreateItem{
					InstanceName: "mysql",
					OperatorID:   "2",
					Plan:         "professional",
				},
				addonSpec: &apistructs.AddonExtension{},
				addonDice: &diceyml.Object{},
				vendor:    apistructs.ECIVendorAlibaba,
			},
			wantErr: false,
		},
		{
			name:   "Test_02",
			fields: testfileds,
			args: args{
				addonIns: &dbclient.AddonInstance{
					ProjectID: "1",
					OrgID:     "1",
					Workspace: "DEV",
				},
				addonInsRouting: &dbclient.AddonInstanceRouting{},
				params: &apistructs.AddonHandlerCreateItem{
					InstanceName: "mysql",
					OperatorID:   "2",
					Plan:         "professional",
				},
				addonSpec: &apistructs.AddonExtension{},
				addonDice: &diceyml.Object{},
				vendor:    apistructs.ECIVendorHuawei,
			},
			wantErr: false,
		},
		{
			name:   "Test_03",
			fields: testfileds,
			args: args{
				addonIns: &dbclient.AddonInstance{
					ProjectID: "1",
					OrgID:     "1",
					Workspace: "DEV",
				},
				addonInsRouting: &dbclient.AddonInstanceRouting{},
				params: &apistructs.AddonHandlerCreateItem{
					InstanceName: "mysql",
					OperatorID:   "2",
					Plan:         "professional",
				},
				addonSpec: &apistructs.AddonExtension{},
				addonDice: &diceyml.Object{},
				vendor:    apistructs.ECIVendorTecent,
			},
			wantErr: false,
		},
		{
			name:   "Test_04",
			fields: testfileds,
			args: args{
				addonIns: &dbclient.AddonInstance{
					ProjectID: "1",
					OrgID:     "1",
					Workspace: "DEV",
				},
				addonInsRouting: &dbclient.AddonInstanceRouting{},
				params: &apistructs.AddonHandlerCreateItem{
					InstanceName: "mysql",
					OperatorID:   "2",
					Plan:         "professional",
				},
				addonSpec: &apistructs.AddonExtension{},
				addonDice: &diceyml.Object{},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			a := &Addon{
				db:               tt.fields.db,
				bdl:              tt.fields.bdl,
				hc:               tt.fields.hc,
				encrypt:          tt.fields.encrypt,
				resource:         tt.fields.resource,
				kms:              tt.fields.kms,
				serviceGroupImpl: tt.fields.serviceGroupImpl,
			}
			patch1 := monkey.Patch(utils.IsProjectECIEnable, func(bdl *bundle.Bundle, projectID uint64, workspace string, orgID uint64, userID string) bool {

				return true
			})
			patch2 := monkey.PatchInstanceMethod(reflect.TypeOf(a), "BuildAddonRequestGroup", func(a *Addon, params *apistructs.AddonHandlerCreateItem, addonIns *dbclient.AddonInstance, addonSpec *apistructs.AddonExtension, addonDice *diceyml.Object) (*apistructs.ServiceGroupCreateV2Request, error) {

				return &apistructs.ServiceGroupCreateV2Request{
					ClusterName: "test",
				}, nil
			})
			patch3 := monkey.PatchInstanceMethod(reflect.TypeOf(a.serviceGroupImpl), "Create", func(_ *servicegroup.ServiceGroupImpl, sg apistructs.ServiceGroupCreateV2Request) (apistructs.ServiceGroup, error) {
				return apistructs.ServiceGroup{}, nil
			})
			patch4 := monkey.PatchInstanceMethod(reflect.TypeOf(a), "GetAddonResourceStatus", func(a *Addon, addonIns *dbclient.AddonInstance,
				addonInsRouting *dbclient.AddonInstanceRouting,
				addonDice *diceyml.Object, addonSpec *apistructs.AddonExtension) error {

				return nil
			})
			patch5 := monkey.PatchInstanceMethod(reflect.TypeOf(a), "InitMySQLAccount", func(a *Addon, addonIns *dbclient.AddonInstance, addonInsRouting *dbclient.AddonInstanceRouting, operator string) error {

				return nil
			})
			defer patch5.Unpatch()
			defer patch4.Unpatch()
			defer patch3.Unpatch()
			defer patch2.Unpatch()
			defer patch1.Unpatch()

			if err := a.basicAddonDeploy(tt.args.addonIns, tt.args.addonInsRouting, tt.args.params, tt.args.addonSpec, tt.args.addonDice, tt.args.vendor); (err != nil) != tt.wantErr {
				t.Errorf("basicAddonDeploy() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func TestAddonInstanceRoutingList_GetByName(t *testing.T) {
	var name = "dspo-mysql"
	var list = []dbclient.AddonInstanceRouting{
		{Name: name, Category: "database"},
		{Name: name, Category: apistructs.CUSTOM_TYPE_CUSTOM},
		{Name: name, Category: apistructs.CUSTOM_TYPE_CLOUD},
	}
	l := addonInstanceRoutingList(list)
	item, ok := l.GetByName(name)
	if !ok {
		t.Errorf("not ok, name: %s", name)
	}
	if item.Name != name {
		t.Errorf("name error, expected: %s, actual: %s", name, item.Name)
	}
	if item.Category != apistructs.CUSTOM_TYPE_CUSTOM {
		t.Errorf("category error, expected: %s, actual: %s", apistructs.CUSTOM_TYPE_CUSTOM, item.Category)
	}
}

func TestAddonInstanceRoutingList_GetByTag(t *testing.T) {
	var (
		name = "dspo-mysql"
		tag  = "basic"
	)
	var list = []dbclient.AddonInstanceRouting{
		{Name: name, Category: "database", Tag: tag},
		{Name: name, Category: apistructs.CUSTOM_TYPE_CUSTOM, Tag: tag},
	}
	l := addonInstanceRoutingList(list)
	if _, ok := l.GetByTag(""); ok {
		t.Errorf("it should not be ture")
	}
	item, ok := l.GetByTag(tag)
	if !ok {
		t.Errorf("not ok, name: %s", tag)
	}
	if item.Tag != tag {
		t.Errorf("name error, expected: %s, actual: %s", tag, item.Tag)
	}
	if item.Category != apistructs.CUSTOM_TYPE_CUSTOM {
		t.Errorf("category error, expected: %s, actual: %s", apistructs.CUSTOM_TYPE_CUSTOM, item.Category)
	}
}
