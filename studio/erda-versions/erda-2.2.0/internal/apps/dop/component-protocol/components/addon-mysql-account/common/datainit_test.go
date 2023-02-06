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

package common

import (
	"context"
	"reflect"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	addonmysqlpb "github.com/erda-project/erda-proto-go/orchestrator/addon/mysql/pb"
	"github.com/erda-project/erda/apistructs"
)

func TestAccountData_GetAccountName(t *testing.T) {
	type fields struct {
		ShowPerm        bool
		EditPerm        bool
		Attachments     []*addonmysqlpb.Attachment
		AttachmentMap   map[uint64]*addonmysqlpb.Attachment
		Accounts        []*addonmysqlpb.MySQLAccount
		AccountMap      map[string]*addonmysqlpb.MySQLAccount
		AccountRefCount map[string]int
		Apps            []apistructs.ApplicationDTO
		AppMap          map[string]*apistructs.ApplicationDTO
	}
	type args struct {
		accountID string
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   string
	}{
		{
			name: "good",
			fields: fields{
				AccountMap: map[string]*addonmysqlpb.MySQLAccount{
					"123": {
						Id:       "123",
						Username: "mysql-123",
					},
				},
			},
			args: args{
				accountID: "123",
			},
			want: "mysql-123",
		},
		{
			name: "bad",
			fields: fields{
				AccountMap: map[string]*addonmysqlpb.MySQLAccount{
					"123": {
						Id:       "123",
						Username: "mysql-123",
					},
				},
			},
			args: args{
				accountID: "321",
			},
			want: "321",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			d := &AccountData{
				ShowPerm:        tt.fields.ShowPerm,
				EditPerm:        tt.fields.EditPerm,
				Attachments:     tt.fields.Attachments,
				AttachmentMap:   tt.fields.AttachmentMap,
				Accounts:        tt.fields.Accounts,
				AccountMap:      tt.fields.AccountMap,
				AccountRefCount: tt.fields.AccountRefCount,
				Apps:            tt.fields.Apps,
				AppMap:          tt.fields.AppMap,
			}
			if got := d.GetAccountName(tt.args.accountID); got != tt.want {
				t.Errorf("GetAccountName() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestAccountData_GetAppName(t *testing.T) {
	type fields struct {
		ShowPerm        bool
		EditPerm        bool
		Attachments     []*addonmysqlpb.Attachment
		AttachmentMap   map[uint64]*addonmysqlpb.Attachment
		Accounts        []*addonmysqlpb.MySQLAccount
		AccountMap      map[string]*addonmysqlpb.MySQLAccount
		AccountRefCount map[string]int
		Apps            []apistructs.ApplicationDTO
		AppMap          map[string]*apistructs.ApplicationDTO
	}
	type args struct {
		appID string
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   string
	}{
		{
			name: "have",
			fields: fields{
				AppMap: map[string]*apistructs.ApplicationDTO{
					"123": {
						ID:   123,
						Name: "app-123",
					},
				},
			},
			args: args{
				appID: "123",
			},
			want: "app-123",
		},
		{
			name: "not",
			fields: fields{
				AppMap: map[string]*apistructs.ApplicationDTO{
					"123": {
						ID:   123,
						Name: "app-123",
					},
				},
			},
			args: args{
				appID: "321",
			},
			want: "",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			d := &AccountData{
				ShowPerm:        tt.fields.ShowPerm,
				EditPerm:        tt.fields.EditPerm,
				Attachments:     tt.fields.Attachments,
				AttachmentMap:   tt.fields.AttachmentMap,
				Accounts:        tt.fields.Accounts,
				AccountMap:      tt.fields.AccountMap,
				AccountRefCount: tt.fields.AccountRefCount,
				Apps:            tt.fields.Apps,
				AppMap:          tt.fields.AppMap,
			}
			if got := d.GetAppName(tt.args.appID); got != tt.want {
				t.Errorf("GetAppName() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_countAccountUsage(t *testing.T) {
	type args struct {
		attachments []*addonmysqlpb.Attachment
	}
	tests := []struct {
		name string
		args args
		want map[string]int
	}{
		{
			name: "t1",
			args: args{
				attachments: []*addonmysqlpb.Attachment{
					{
						AccountId:    "123",
						PreAccountId: "321",
						AccountState: "PRE",
					},
				},
			},
			want: map[string]int{
				"123": 1,
				"321": 1,
			},
		},
		{
			name: "t2",
			args: args{
				attachments: []*addonmysqlpb.Attachment{
					{
						AccountId:    "123",
						PreAccountId: "321",
						AccountState: "CUR",
					},
				},
			},
			want: map[string]int{
				"123": 1,
			},
		},
		{
			name: "t3",
			args: args{
				attachments: []*addonmysqlpb.Attachment{
					{
						AccountId:    "123",
						PreAccountId: "321",
						AccountState: "PRE",
					},
					{
						AccountId:    "123",
						PreAccountId: "321",
						AccountState: "CUR",
					},
				},
			},
			want: map[string]int{
				"123": 2,
				"321": 1,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := countAccountUsage(tt.args.attachments); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("countAccountUsage() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestSetAccountData(t *testing.T) {
	ctx := context.WithValue(context.Background(), cptype.GlobalInnerKeyStateTemp, map[string]interface{}{})
	SetAccountData(ctx, &AccountData{ShowPerm: true})
	loaded, err := LoadAccountData(ctx)
	assert.NoError(t, err)
	assert.Equal(t, &AccountData{ShowPerm: true}, loaded)
}
