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

package mysql

import (
	"context"
	"reflect"
	"testing"
	"time"

	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-proto-go/orchestrator/addon/mysql/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/pkg/kms/kmstypes"
)

type MockKMS struct {
}

func (k *MockKMS) CreateKey() (*kmstypes.CreateKeyResponse, error) {
	return &kmstypes.CreateKeyResponse{KeyMetadata: kmstypes.KeyMetadata{
		KeyID: "123",
	}}, nil
}

func (k *MockKMS) Encrypt(plaintext, keyID string) (*kmstypes.EncryptResponse, error) {
	return &kmstypes.EncryptResponse{
		KeyID:            "123",
		CiphertextBase64: "***",
	}, nil
}

func (k *MockKMS) Decrypt(ciphertext, keyID string) (*kmstypes.DecryptResponse, error) {
	return &kmstypes.DecryptResponse{
		PlaintextBase64: "MjIy",
	}, nil
}

func Test_mysqlService_ToDTO(t *testing.T) {
	type fields struct {
		logger logs.Logger
		kms    KMSWrapper
		db     *dbclient.DBClient
	}
	type args struct {
		acc     *dbclient.MySQLAccount
		decrypt bool
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   *pb.MySQLAccount
	}{
		{
			name: "t1",
			fields: fields{
				kms: &MockKMS{},
			},
			args: args{
				acc: &dbclient.MySQLAccount{
					ID:                "1",
					CreatedAt:         time.Time{},
					UpdatedAt:         time.Time{},
					Username:          "mysql-123",
					Password:          "MjIy",
					KMSKey:            "123",
					InstanceID:        "inst1",
					RoutingInstanceID: "r_inst2",
					Creator:           "321",
					IsDeleted:         false,
				},
				decrypt: true,
			},
			want: &pb.MySQLAccount{
				Id:         "1",
				InstanceId: "r_inst2",
				Creator:    "321",
				CreateAt:   timestamppb.New(time.Time{}),
				Username:   "mysql-123",
				Password:   "222",
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &mysqlService{
				logger: tt.fields.logger,
				kms:    tt.fields.kms,
				db:     tt.fields.db,
			}
			if got := s.ToDTO(tt.args.acc, tt.args.decrypt); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("ToDTO() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_mysqlService_getConnConfig(t *testing.T) {
	type fields struct {
		logger logs.Logger
		kms    KMSWrapper
		db     *dbclient.DBClient
	}
	type args struct {
		ins *dbclient.AddonInstance
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    *connConfig
		wantErr bool
	}{
		{
			name: "t1",
			fields: fields{
				kms: &MockKMS{},
			},
			args: args{
				ins: &dbclient.AddonInstance{
					Config: `{"MYSQL_HOST":"hhh","MYSQL_PORT":"ppp","MYSQL_USERNAME":"uuu","MYSQL_PASSWORD":"MjIy"}`,
				},
			},
			want: &connConfig{
				Host: "hhh",
				Port: "ppp",
				User: "uuu",
				Pass: "222",
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &mysqlService{
				logger: tt.fields.logger,
				kms:    tt.fields.kms,
				db:     tt.fields.db,
			}
			got, err := s.getConnConfig(tt.args.ins)
			if (err != nil) != tt.wantErr {
				t.Errorf("getConnConfig() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getConnConfig() got = %v, want %v", got, tt.want)
			}
		})
	}
}

type MockPerm struct {
}

func (m *MockPerm) CheckPermission(req *apistructs.PermissionCheckRequest) (*apistructs.PermissionCheckResponseData, error) {
	if req.ScopeID == 24 {
		return &apistructs.PermissionCheckResponseData{Access: true}, nil
	}
	return &apistructs.PermissionCheckResponseData{Access: false}, nil
}

func (m *MockPerm) CreateAuditEvent(audits *apistructs.AuditCreateRequest) error {
	return nil
}

func (m *MockPerm) GetProject(id uint64) (*apistructs.ProjectDTO, error) {
	return &apistructs.ProjectDTO{
		ID:   id,
		Name: "test-project",
	}, nil
}

func (m *MockPerm) GetApp(id uint64) (*apistructs.ApplicationDTO, error) {
	return &apistructs.ApplicationDTO{
		ID:   id,
		Name: "test-app",
	}, nil
}

func Test_mysqlService_checkPerm(t *testing.T) {
	type fields struct {
		logger logs.Logger
		kms    KMSWrapper
		perm   PermissionWrapper
		db     *dbclient.DBClient
	}
	type args struct {
		userID   string
		routing  *dbclient.AddonInstanceRouting
		resource string
		action   string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			name: "t1",
			fields: fields{
				perm: &MockPerm{},
			},
			args: args{
				userID: "111",
				routing: &dbclient.AddonInstanceRouting{
					ID:        "222",
					ProjectID: "24",
				},
				resource: "addon",
				action:   "UPDATE",
			},
			wantErr: false,
		},
		{
			name: "t2",
			fields: fields{
				perm: &MockPerm{},
			},
			args: args{
				userID: "111",
				routing: &dbclient.AddonInstanceRouting{
					ID:        "222",
					ProjectID: "42",
				},
				resource: "addon",
				action:   "UPDATE",
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &mysqlService{
				logger: tt.fields.logger,
				kms:    tt.fields.kms,
				perm:   tt.fields.perm,
				db:     tt.fields.db,
			}
			if err := s.mustHavePerm(tt.args.userID, tt.args.routing, tt.args.resource, tt.args.action); (err != nil) != tt.wantErr {
				t.Errorf("checkPerm() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func Test_mysqlService_audit(t *testing.T) {
	type fields struct {
		logger logs.Logger
		kms    KMSWrapper
		perm   PermissionWrapper
		db     *dbclient.DBClient
	}
	type args struct {
		ctx      context.Context
		userID   string
		orgID    string
		routing  *dbclient.AddonInstanceRouting
		att      *dbclient.AddonAttachment
		tmplName apistructs.TemplateName
		tmplCtx  map[string]interface{}
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			name: "create",
			fields: fields{
				perm: &MockPerm{},
			},
			args: args{
				ctx:    context.Background(),
				userID: "111",
				orgID:  "333",
				routing: &dbclient.AddonInstanceRouting{
					ID:        "222",
					ProjectID: "24",
				},
				att:      nil,
				tmplName: "create",
				tmplCtx: map[string]interface{}{
					"mysqlUsername": "mysql",
				},
			},
			wantErr: false,
		},
		{
			name: "update",
			fields: fields{
				perm: &MockPerm{},
			},
			args: args{
				ctx:    context.Background(),
				userID: "111",
				orgID:  "333",
				routing: &dbclient.AddonInstanceRouting{
					ID:        "222",
					ProjectID: "24",
				},
				att:      nil,
				tmplName: "update",
				tmplCtx: map[string]interface{}{
					"mysqlUsername": "mysql",
				},
			},
			wantErr: false,
		},
		{
			name: "change attachment",
			fields: fields{
				perm: &MockPerm{},
			},
			args: args{
				ctx:    context.Background(),
				userID: "111",
				orgID:  "333",
				routing: &dbclient.AddonInstanceRouting{
					ID:        "222",
					ProjectID: "24",
				},
				att: &dbclient.AddonAttachment{
					RuntimeID:     "123",
					RuntimeName:   "321",
					ApplicationID: "444",
				},
				tmplName: "update",
				tmplCtx: map[string]interface{}{
					"mysqlUsername": "mysql",
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &mysqlService{
				logger: tt.fields.logger,
				kms:    tt.fields.kms,
				perm:   tt.fields.perm,
				db:     tt.fields.db,
			}
			if err := s.audit(tt.args.ctx, tt.args.userID, tt.args.orgID, tt.args.routing, tt.args.att, tt.args.tmplName, tt.args.tmplCtx); (err != nil) != tt.wantErr {
				t.Errorf("audit() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
