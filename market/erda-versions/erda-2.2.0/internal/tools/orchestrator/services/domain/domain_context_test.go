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

package domain

import (
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

func TestGroupDomains(t *testing.T) {
	ctx := getFakeCtx()

	// do invoke
	data := ctx.GroupDomains()

	d, _ := json.Marshal(data)
	var actual interface{}
	json.Unmarshal(d, &actual)

	expectJson := `{"showcase-front":[{"appName":"showcase-front","domainId":11,"domain":"showcase-front-dev-8.dev.terminus.io","domainType":"DEFAULT","customDomain":"showcase-front-dev-8","rootDomain":".dev.terminus.io","useHttps":false},{"appName":"showcase-front","domainId":13,"domain":"baidu.com","domainType":"CUSTOM","customDomain":"","rootDomain":"","useHttps":false},{"appName":"showcase-front","domainId":14,"domain":"fromt.dev.terminus.io","domainType":"CUSTOM","customDomain":"","rootDomain":"","useHttps":false}],"blog-web":[{"appName":"blog-web","domainId":12,"domain":"blog-web-dev-8.dev.terminus.io","domainType":"DEFAULT","customDomain":"blog-web-dev-8","rootDomain":".dev.terminus.io","useHttps":false},{"appName":"blog-web","domainId":15,"domain":"google.com","domainType":"CUSTOM","customDomain":"","rootDomain":"","useHttps":false}]}`
	var expect interface{}
	json.Unmarshal([]byte(expectJson), &expect)

	assert.Equal(t, expect, actual)
}

// this ut patched a inline func (db.SaveDomain), need go test --gcflags=-l

// func TestUpdateDomains(t *testing.T) {
// 	ctx := getFakeCtx()
//
// 	var db *dbclient.DBClient
// 	monkey.PatchInstanceMethod(reflect.TypeOf(db), "FindDomains",
// 		func(_ *dbclient.DBClient, domainValues []string) ([]dbclient.RuntimeDomain, error) {
// 			for _, d := range domainValues {
// 				if d == "occupied.dev.terminus.io" {
// 					return []dbclient.RuntimeDomain{
// 						{
// 							RuntimeId:    101,
// 							Domain:       "occupied.dev.terminus.io",
// 							EndpointName: "xxx",
// 						},
// 					}, nil
// 				}
// 			}
// 			return nil, nil
// 		},
// 	)
// 	var toSaves []*dbclient.RuntimeDomain
// 	monkey.PatchInstanceMethod(reflect.TypeOf(db), "SaveDomain",
// 		func(_ *dbclient.DBClient, domain *dbclient.RuntimeDomain) error {
// 			toSaves = append(toSaves, domain)
// 			return nil
// 		},
// 	)
// 	var toDeletes []string
// 	monkey.PatchInstanceMethod(reflect.TypeOf(db), "DeleteDomain",
// 		func(_ *dbclient.DBClient, domainValue string) error {
// 			toDeletes = append(toDeletes, domainValue)
// 			return nil
// 		},
// 	)
//
// 	// success, save baidu.com & google.com
// 	toSaves = make([]*dbclient.RuntimeDomain, 0)
// 	resp := ctx.UpdateDomains(buildUpdateReq(`{"showcase-front":[{"appName":"showcase-front","domainId":11,"domain":"showcase-front-dev-8.dev.terminus.io","domainType":"DEFAULT","customDomain":"showcase-front-dev-8-changed","rootDomain":".dev.terminus.io","useHttps":false},{"appName":"showcase-front","domainId":14,"domain":"fromt.dev.terminus.io","domainType":"CUSTOM","customDomain":"","rootDomain":"","useHttps":false,"serviceName":"showcase-front"},{"domainType":"CUSTOM","serviceName":"showcase-front","domain":"example.com"}],"blog-web":[{"appName":"blog-web","domainId":12,"domain":"blog-web-dev-8.dev.terminus.io","domainType":"DEFAULT","customDomain":"blog-web-dev-8-changed","rootDomain":".dev.terminus.io","useHttps":false},{"appName":"blog-web","domainId":15,"domain":"google.com","domainType":"CUSTOM","customDomain":"","rootDomain":"","useHttps":false,"serviceName":"blog-web"},{"domainType":"CUSTOM","serviceName":"blog-web","domain":"example2.com"}]}`))
// 	assert.Nil(t, resp)
// 	assert.Equal(t, 4, len(toSaves))
// 	assert.Equal(t, []*dbclient.RuntimeDomain{
// 		{
// 			EndpointName: "showcase-front",
// 			Domain:       "showcase-front-dev-8-changed.dev.terminus.io",
// 			DomainType:   "DEFAULT",
// 			RuntimeId:    8,
// 			BaseModel: dbengine.BaseModel{
// 				ID: 0,
// 			},
// 		},
// 		{
// 			EndpointName: "showcase-front",
// 			Domain:       "example.com",
// 			DomainType:   "CUSTOM",
// 			RuntimeId:    8,
// 			BaseModel: dbengine.BaseModel{
// 				ID: 0,
// 			},
// 		},
// 		{
// 			EndpointName: "blog-web",
// 			Domain:       "blog-web-dev-8-changed.dev.terminus.io",
// 			DomainType:   "DEFAULT",
// 			RuntimeId:    8,
// 			BaseModel: dbengine.BaseModel{
// 				ID: 0,
// 			},
// 		},
// 		{
// 			EndpointName: "blog-web",
// 			Domain:       "example2.com",
// 			DomainType:   "CUSTOM",
// 			RuntimeId:    8,
// 			BaseModel: dbengine.BaseModel{
// 				ID: 0,
// 			},
// 		},
// 	}, toSaves)
// 	assert.Equal(t, 3, len(toDeletes))
// 	assert.Equal(t, []string{"showcase-front-dev-8.dev.terminus.io", "blog-web-dev-8.dev.terminus.io", "baidu.com"}, toDeletes)
//
// 	// duplicated in group
// 	resp = ctx.UpdateDomains(buildUpdateReq(`{"showcase-front":[{"appName":"showcase-front","domainId":11,"domain":"showcase-front-dev-8.dev.terminus.io","domainType":"DEFAULT","customDomain":"showcase-front-dev-8","rootDomain":".dev.terminus.io","useHttps":false},{"appName":"showcase-front","domainId":13,"domain":"baidu.com","domainType":"CUSTOM","customDomain":"","rootDomain":"","useHttps":false,"serviceName":"showcase-front"},{"appName":"showcase-front","domainId":14,"domain":"fromt.dev.terminus.io","domainType":"CUSTOM","customDomain":"","rootDomain":"","useHttps":false,"serviceName":"showcase-front"},{"domainType":"CUSTOM","serviceName":"showcase-front","domain":"baidu.com"}],"blog-web":[{"appName":"blog-web","domainId":12,"domain":"blog-web-dev-8.dev.terminus.io","domainType":"DEFAULT","customDomain":"blog-web-dev-8","rootDomain":".dev.terminus.io","useHttps":false},{"appName":"blog-web","domainId":15,"domain":"google.com","domainType":"CUSTOM","customDomain":"","rootDomain":"","useHttps":false,"serviceName":"blog-web"}]}`))
// 	assert.Equal(t, "更新域名失败: 参数错误 域名 baidu.com 重复使用", resp.Error())
//
// 	// occupied
// 	resp = ctx.UpdateDomains(buildUpdateReq(`{"showcase-front":[{"appName":"showcase-front","domainId":11,"domain":"showcase-front-dev-8.dev.terminus.io","domainType":"DEFAULT","customDomain":"showcase-front-dev-8","rootDomain":".dev.terminus.io","useHttps":false},{"appName":"showcase-front","domainId":13,"domain":"baidu.com","domainType":"CUSTOM","customDomain":"","rootDomain":"","useHttps":false,"serviceName":"showcase-front"},{"appName":"showcase-front","domainId":14,"domain":"fromt.dev.terminus.io","domainType":"CUSTOM","customDomain":"","rootDomain":"","useHttps":false,"serviceName":"showcase-front"},{"domainType":"CUSTOM","serviceName":"showcase-front","domain":"occupied.dev.terminus.io"}],"blog-web":[{"appName":"blog-web","domainId":12,"domain":"blog-web-dev-8.dev.terminus.io","domainType":"DEFAULT","customDomain":"blog-web-dev-8","rootDomain":".dev.terminus.io","useHttps":false},{"appName":"blog-web","domainId":15,"domain":"google.com","domainType":"CUSTOM","customDomain":"","rootDomain":"","useHttps":false,"serviceName":"blog-web"}]}`))
// 	assert.Equal(t, "更新域名失败: 状态异常 域名 occupied.dev.terminus.io 已被 Runtime 101:xxx 使用", resp.Error())
// }

func buildUpdateReq(reqJson string) *apistructs.DomainGroup {
	var group apistructs.DomainGroup
	if err := json.Unmarshal([]byte(reqJson), &group); err != nil {
		panic(err)
	}
	return &group
}

func getFakeCtx() *context {
	domains := []dbclient.RuntimeDomain{
		{
			EndpointName: "showcase-front",
			Domain:       "showcase-front-dev-8.dev.terminus.io",
			DomainType:   "DEFAULT",
			RuntimeId:    8,
			BaseModel: dbengine.BaseModel{
				ID: 11,
			},
		},
		{
			EndpointName: "blog-web",
			Domain:       "blog-web-dev-8.dev.terminus.io",
			DomainType:   "DEFAULT",
			RuntimeId:    8,
			BaseModel: dbengine.BaseModel{
				ID: 12,
			},
		},
		{
			EndpointName: "showcase-front",
			Domain:       "baidu.com",
			DomainType:   "CUSTOM",
			RuntimeId:    8,
			BaseModel: dbengine.BaseModel{
				ID: 13,
			},
		},
		{
			EndpointName: "showcase-front",
			Domain:       "fromt.dev.terminus.io",
			DomainType:   "CUSTOM",
			RuntimeId:    8,
			BaseModel: dbengine.BaseModel{
				ID: 14,
			},
		},
		{
			EndpointName: "blog-web",
			Domain:       "google.com",
			DomainType:   "CUSTOM",
			RuntimeId:    8,
			BaseModel: dbengine.BaseModel{
				ID: 15,
			},
		},
	}

	ctx := context{
		Runtime: &dbclient.Runtime{
			BaseModel: dbengine.BaseModel{ID: 8},
		},
		Domains:    domains,
		RootDomain: ".dev.terminus.io",
	}
	return &ctx
}
