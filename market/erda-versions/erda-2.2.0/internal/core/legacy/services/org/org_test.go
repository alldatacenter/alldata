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

package org

import (
	"fmt"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/legacy/model"
)

// func TestShouldGetOrgByName(t *testing.T) {
// 	db, mock, err := sqlmock.New()
// 	require.NoError(t, err)
// 	connection, err := gorm.Open("mysql", db)
// 	require.NoError(t, err)
// 	client := &dao.DBClient{
// 		connection,
// 	}

// 	const sql = `SELECT * FROM "dice_org" WHERE (name = ?)`
// 	const sql1 = ` ORDER BY "dice_org"."id" ASC LIMIT 1`
// 	str := regexp.QuoteMeta(sql + sql1)
// 	mock.ExpectQuery(str).
// 		WithArgs("org1").
// 		WillReturnRows(sqlmock.NewRows([]string{"id", "name"}).
// 			AddRow(1, "org1"))

// 	org, err := client.GetOrgByName("org1")
// 	require.NoError(t, err)
// 	assert.Equal(t, org.ID, 1)

// 	require.NoError(t, mock.ExpectationsWereMet())
// }

func TestGetOrgByDomainAndOrgName(t *testing.T) {
	o := &Org{}
	org := &model.Org{Name: "org0"}
	orgByDomain := monkey.PatchInstanceMethod(reflect.TypeOf(o), "GetOrgByDomain", func(_ *Org, domain string) (*model.Org, error) {
		if domain == "org0" {
			return org, nil
		} else {
			return nil, nil
		}
	})
	defer orgByDomain.Unpatch()
	db := &dao.DBClient{}
	orgByName := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetOrgByName", func(_ *dao.DBClient, orgName string) (*model.Org, error) {
		if orgName == "org0" {
			return org, nil
		} else {
			return nil, dao.ErrNotFoundOrg
		}
	})
	defer orgByName.Unpatch()

	res, err := o.GetOrgByDomainAndOrgName("org0", "")
	require.NoError(t, err)
	assert.Equal(t, org, res)
	res, err = o.GetOrgByDomainAndOrgName("org0", "org1")
	require.NoError(t, err)
	assert.Equal(t, org, res)
	res, err = o.GetOrgByDomainAndOrgName("org2", "org1")
	require.NoError(t, err)
	assert.Equal(t, (*model.Org)(nil), res)
}

func TestOrgNameRetriever(t *testing.T) {
	var domains = []string{"erda-org.erda.cloud", "buzz-org.app.terminus.io", "fuzz.com"}
	var domainRoots = []string{"erda.cloud", "app.terminus.io"}
	assert.Equal(t, "erda", orgNameRetriever(domains[0], domainRoots[0]))
	assert.Equal(t, "buzz", orgNameRetriever(domains[1], domainRoots[1]))
	assert.Equal(t, "", orgNameRetriever(domains[2], domainRoots[0]))
}

func TestWithI18n(t *testing.T) {
	var trans i18n.Translator
	New(WithI18n(trans))
}

func TestOrg_ListOrgs(t *testing.T) {
	type args struct {
		orgIDs []int64
		req    *apistructs.OrgSearchRequest
		all    bool
	}
	tests := []struct {
		name    string
		args    args
		want    int
		want1   []model.Org
		wantErr bool
	}{
		{
			name: "test_return_error",
			args: args{
				orgIDs: []int64{1},
				req:    &apistructs.OrgSearchRequest{},
				all:    false,
			},
			want:    0,
			want1:   nil,
			wantErr: true,
		},
		{
			name: "test_all",
			args: args{
				orgIDs: []int64{1},
				req:    &apistructs.OrgSearchRequest{},
				all:    true,
			},
			want: 1,
			want1: []model.Org{
				{
					Name: "1",
				},
			},
			wantErr: false,
		},
		{
			name: "test_not_all",
			args: args{
				orgIDs: []int64{1},
				req:    &apistructs.OrgSearchRequest{},
				all:    false,
			},
			want: 2,
			want1: []model.Org{
				{
					Name: "2",
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			o := &Org{}

			patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(o), "SearchByName", func(o *Org, name string, pageNo, pageSize int) (int, []model.Org, error) {
				if tt.wantErr {
					return 0, nil, fmt.Errorf("error")
				}
				return tt.want, tt.want1, nil
			})
			defer patch1.Unpatch()

			patch2 := monkey.PatchInstanceMethod(reflect.TypeOf(o), "ListByIDsAndName", func(o *Org, orgIDs []int64, name string, pageNo, pageSize int) (int, []model.Org, error) {
				if tt.wantErr {
					return 0, nil, fmt.Errorf("error")
				}
				return tt.want, tt.want1, nil
			})
			defer patch2.Unpatch()

			got, got1, err := o.ListOrgs(tt.args.orgIDs, tt.args.req, tt.args.all)
			if (err != nil) != tt.wantErr {
				t.Errorf("ListOrgs() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("ListOrgs() got = %v, want %v", got, tt.want)
			}
			if !reflect.DeepEqual(got1, tt.want1) {
				t.Errorf("ListOrgs() got1 = %v, want %v", got1, tt.want1)
			}
		})
	}
}

func TestGetAuditMessage(t *testing.T) {
	tt := []struct {
		org  model.Org
		req  apistructs.OrgUpdateRequestBody
		want apistructs.AuditMessage
	}{
		{
			model.Org{DisplayName: "dice"},
			apistructs.OrgUpdateRequestBody{DisplayName: "erda"},
			apistructs.AuditMessage{
				MessageZH: "组织名称由 dice 改为 erda ",
				MessageEN: "org name updated from dice to erda ",
			},
		},
		{
			model.Org{Locale: "en-US"},
			apistructs.OrgUpdateRequestBody{Locale: "zh-CN"},
			apistructs.AuditMessage{
				MessageZH: "通知语言改为中文 ",
				MessageEN: "language updated to zh-CN ",
			},
		},
		{
			model.Org{IsPublic: true},
			apistructs.OrgUpdateRequestBody{IsPublic: false},
			apistructs.AuditMessage{
				MessageZH: "改为私有组织 ",
				MessageEN: "org updated to private ",
			},
		},
		{
			model.Org{Logo: ""},
			apistructs.OrgUpdateRequestBody{Logo: "foo.png"},
			apistructs.AuditMessage{
				MessageZH: "组织Logo发生变更 ",
				MessageEN: "org Logo changed ",
			},
		},
		{
			model.Org{Desc: ""},
			apistructs.OrgUpdateRequestBody{Desc: "bar"},
			apistructs.AuditMessage{
				MessageZH: "组织描述信息发生变更 ",
				MessageEN: "org desc changed ",
			},
		},
		{
			model.Org{BlockoutConfig: model.BlockoutConfig{
				BlockDEV:   false,
				BlockTEST:  false,
				BlockStage: true,
				BlockProd:  true,
			}},
			apistructs.OrgUpdateRequestBody{BlockoutConfig: &apistructs.BlockoutConfig{
				BlockDEV:   true,
				BlockTEST:  true,
				BlockStage: false,
				BlockProd:  false,
			}},
			apistructs.AuditMessage{
				MessageZH: "开发环境开启封网 测试环境开启封网 预发环境关闭封网 生产环境关闭封网 ",
				MessageEN: "block network opened in dev environment block network opened in test environment block network closed in staging environment block network closed in prod environment ",
			},
		},
		{
			model.Org{BlockoutConfig: model.BlockoutConfig{
				BlockDEV:   true,
				BlockTEST:  true,
				BlockStage: true,
				BlockProd:  true,
			},
				DisplayName: "dice", Locale: "en-US", IsPublic: true, Logo: "", Desc: ""},
			apistructs.OrgUpdateRequestBody{BlockoutConfig: &apistructs.BlockoutConfig{
				BlockDEV:   true,
				BlockTEST:  true,
				BlockStage: true,
				BlockProd:  true,
			},
				DisplayName: "dice", Locale: "en-US", IsPublic: true, Logo: "", Desc: ""},
			apistructs.AuditMessage{
				MessageZH: "无信息变更",
				MessageEN: "no message changed",
			},
		},
	}
	for _, v := range tt {
		message := getAuditMessage(v.org, v.req)
		if v.want.MessageEN != message.MessageEN {
			t.Error("fail")
		}
		if v.want.MessageZH != message.MessageZH {
			t.Error("fail")
		}

	}
}
