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

package service

import (
	"testing"

	"github.com/xormplus/xorm"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
)

func TestGatewayKongInfoServiceImpl_acquireKongAddr(t *testing.T) {
	type fields struct {
		engine        *orm.OrmEngine
		SessionHelper *SessionHelper
		executor      xorm.Interface
	}
	type args struct {
		netportalUrl string
		selfAz       string
		info         *orm.GatewayKongInfo
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    string
		wantErr bool
	}{
		{
			"case1",
			fields{
				nil,
				nil,
				nil,
			},
			args{
				"inet://abc?ssl=on",
				"xxx",
				&orm.GatewayKongInfo{
					Az:       "xxx",
					KongAddr: "kong.local",
				},
			},
			"kong.local",
			false,
		},
		{
			"case2",
			fields{
				nil,
				nil,
				nil,
			},
			args{
				"inet://abc?ssl=on",
				"xxx",
				&orm.GatewayKongInfo{
					Az:       "yyy",
					KongAddr: "kong.local",
				},
			},
			"inet://abc?ssl=on/kong.local",
			false,
		},
		{
			"case3",
			fields{
				nil,
				nil,
				nil,
			},
			args{
				"inet://abc?ssl=on",
				"xxx",
				&orm.GatewayKongInfo{
					Az:       "yyy",
					KongAddr: "inet://cde/kong.local",
				},
			},
			"inet://abc?ssl=on/kong.local",
			false,
		},
		{
			"case4",
			fields{
				nil,
				nil,
				nil,
			},
			args{
				"inet://abc?ssl=on",
				"xxx",
				&orm.GatewayKongInfo{
					Az:       "xxx",
					KongAddr: "inet://ads/kong.local",
				},
			},
			"kong.local",
			false,
		},
		{
			"case5",
			fields{
				nil,
				nil,
				nil,
			},
			args{
				"inet://abc?ssl=on",
				"xxx",
				&orm.GatewayKongInfo{
					Az:       "yyy",
					KongAddr: "http://kong.local",
				},
			},
			"inet://abc?ssl=on/kong.local",
			false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			impl := &GatewayKongInfoServiceImpl{
				engine:        tt.fields.engine,
				SessionHelper: tt.fields.SessionHelper,
				executor:      tt.fields.executor,
			}
			got, err := impl.acquireKongAddr(tt.args.netportalUrl, tt.args.selfAz, tt.args.info)
			if (err != nil) != tt.wantErr {
				t.Errorf("GatewayKongInfoServiceImpl.acquireKongAddr() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("GatewayKongInfoServiceImpl.acquireKongAddr() = %v, want %v", got, tt.want)
			}
		})
	}
}
