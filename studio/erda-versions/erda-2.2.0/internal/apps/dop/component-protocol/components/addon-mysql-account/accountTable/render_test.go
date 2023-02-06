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

package accountTable

import (
	"context"
	"reflect"
	"testing"
	"time"

	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	addonmysqlpb "github.com/erda-project/erda-proto-go/orchestrator/addon/mysql/pb"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/addon-mysql-account/accountTable/table"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/addon-mysql-account/common"
)

func Test_comp_getDatum(t *testing.T) {
	type fields struct {
		ac      *common.AccountData
		pg      *common.PageDataAccount
		userIDs []string
	}
	now := time.Now()
	type args struct {
		item *addonmysqlpb.MySQLAccount
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   map[string]table.ColumnData
	}{
		{
			name: "t1",
			fields: fields{
				ac: &common.AccountData{
					Attachments:     nil,
					AttachmentMap:   nil,
					Accounts:        nil,
					AccountMap:      nil,
					AccountRefCount: map[string]int{},
					Apps:            nil,
					AppMap:          nil,
				},
				pg: &common.PageDataAccount{
					ProjectID:             24,
					InstanceID:            "123",
					AccountID:             "555",
					ShowDeleteModal:       false,
					ShowViewPasswordModal: false,
					FilterValues:          nil,
				},
				userIDs: nil,
			},
			args: args{
				item: &addonmysqlpb.MySQLAccount{
					Id:         "111",
					InstanceId: "123",
					Creator:    "333",
					CreateAt:   timestamppb.New(now),
					Username:   "user",
					Password:   "pass",
				},
			},
			want: map[string]table.ColumnData{
				"attachments": {
					Value:      "i18n:not_used",
					RenderType: "text",
				},
				"createdAt": {
					Value:      timestamppb.New(now).AsTime().Format(time.RFC3339),
					RenderType: "datePicker",
				},
				"creator": {
					Value:      "333",
					RenderType: "userAvatar",
				},
				"username": {
					Value:      "user",
					RenderType: "text",
				},
				"operate": {
					Value:      "",
					RenderType: "tableOperation",
					Tags:       nil,
					Operations: map[string]*table.Operation{
						"viewPassword": {
							Key:    "viewPassword",
							Text:   "i18n:view_password",
							Reload: true,
							Meta: map[string]string{
								"id": "111",
							},
							Disabled:    true,
							DisabledTip: "i18n:view_password_no_perm_tip",
							ShowIndex:   1,
						},
						"delete": {
							Key:    "delete",
							Text:   "i18n:delete",
							Reload: true,
							Meta: map[string]string{
								"id": "111",
							},
							Disabled:    true,
							DisabledTip: "i18n:delete_no_perm_tip",
							ShowIndex:   2,
							Confirm:     "i18n:delete_confirm",
							SuccessMsg:  "i18n:delete_success_tip",
						},
					},
				},
			},
		},
	}

	sdk := cptype.SDK{
		Tran: &MockTran{},
	}

	// make ctx with sdk
	ctx := context.WithValue(context.Background(), cptype.GlobalInnerKeyCtxSDK, &sdk)

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			f := &comp{
				ac:      tt.fields.ac,
				pg:      tt.fields.pg,
				userIDs: tt.fields.userIDs,
			}
			if got := f.getDatum(ctx, tt.args.item); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getDatum() = %+v, want %+v", got, tt.want)
			}
		})
	}
}

type MockTran struct {
	i18n.Translator
}

func (m *MockTran) Text(lang i18n.LanguageCodes, key string) string {
	return "i18n:" + key
}

func (m *MockTran) Sprintf(lang i18n.LanguageCodes, key string, args ...interface{}) string {
	return "i18n:" + key
}
