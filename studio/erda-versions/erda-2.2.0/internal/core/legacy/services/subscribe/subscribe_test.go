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

package subscribe

import (
	"fmt"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/conf"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/legacy/model"
)

func TestSubscribe_Subscribe(t *testing.T) {
	tests := []struct {
		name      string
		want      int
		wantErr   bool
		countErr  bool
		dupErr    bool
		createErr bool
	}{
		{
			name:      "test1_count_error",
			want:      0,
			wantErr:   false,
			countErr:  false,
			dupErr:    false,
			createErr: false,
		},
		{
			name:      "test2_duplication_error",
			want:      0,
			wantErr:   true,
			countErr:  false,
			dupErr:    true,
			createErr: false,
		},
		{
			name:      "test3_create_error",
			want:      0,
			wantErr:   true,
			countErr:  false,
			dupErr:    false,
			createErr: true,
		},
		{
			name:      "test4_create_success",
			want:      1,
			wantErr:   false,
			countErr:  false,
			dupErr:    false,
			createErr: false,
		},
	}
	conf.LoadForTest()
	t.Logf("limit: %v", conf.SubscribeLimitNum())

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var c *dao.DBClient
			createReq := apistructs.CreateSubscribeReq{
				Type:   apistructs.AppSubscribe,
				TypeID: 111,
				Name:   "subscribe_app_name",
				UserID: "2",
				OrgID:  1,
			}

			monkey.PatchInstanceMethod(reflect.TypeOf(c), "GetSubscribeCount", func(c *dao.DBClient, tp string, userID string, orgID uint64) (int, error) {
				if tt.countErr {
					return 3 + 1, nil
				}
				return 0, nil
			})
			defer monkey.UnpatchAll()

			monkey.PatchInstanceMethod(reflect.TypeOf(c), "GetSubscribe", func(c *dao.DBClient, tp string, tpID uint64, userID string, orgID uint64) (*model.Subscribe, error) {
				if tt.dupErr {
					return &model.Subscribe{
						TypeID: createReq.TypeID,
						Type:   createReq.Type.String(),
						UserID: createReq.UserID,
						Name:   createReq.Name,
					}, nil
				}
				return nil, nil
			})

			monkey.PatchInstanceMethod(reflect.TypeOf(c), "CreateSubscribe", func(c *dao.DBClient, subscribe *model.Subscribe) error {
				if tt.createErr {
					return fmt.Errorf("error")
				}
				subscribe.ID = "666"
				return nil
			})

			s := New(WithDBClient(&dao.DBClient{}))
			got, err := s.Subscribe(createReq)
			if (err != nil) != tt.wantErr {
				t.Errorf("Subscribe() error = %v, wantErr %v", err, tt.wantErr)
				return
			}

			if tt.wantErr && got != "" {
				t.Errorf("Subscribe() want error, but return non empty id: %v", got)
			}

			if !tt.wantErr && got == "" {
				t.Errorf("Subscribe() want success, but return empty id: %v", got)
			}

		})
	}
}

func TestSubscribe_UnSubscribe(t *testing.T) {
	tests := []struct {
		name         string
		wantErr      bool
		invalidReq   bool
		invalidType  bool
		deleteErr    bool
		deleteSuc    bool
		dByUserIDErr bool
		dByUserIDSuc bool
		dByIDErr     bool
		dByIDSuc     bool
	}{
		{
			name:       "test1_invalid_request_error",
			wantErr:    true,
			invalidReq: true,
		},
		{
			name:        "test2_invalid_type_error",
			wantErr:     true,
			invalidType: true,
		},
		{
			name:      "test3_delete_error",
			wantErr:   true,
			deleteErr: true,
		},
		{
			name:         "test4_delete_by_user_org_id_error",
			wantErr:      true,
			dByUserIDErr: true,
		},
		{
			name:     "test5_delete_by_id_error",
			wantErr:  true,
			dByIDErr: true,
		},
		{
			name:      "test6_delete_success",
			wantErr:   false,
			deleteSuc: true,
		},
		{
			name:         "test7_delete_by_user_id_success",
			wantErr:      false,
			dByUserIDSuc: true,
		},
		{
			name:     "test8_delete_by_id_success",
			wantErr:  false,
			dByIDSuc: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var c *dao.DBClient
			var req apistructs.UnSubscribeReq
			if tt.invalidReq {
				req = apistructs.UnSubscribeReq{}
			} else if tt.invalidType {
				req = apistructs.UnSubscribeReq{
					Type: apistructs.AppSubscribe,
				}
			} else if tt.deleteErr || tt.deleteSuc {
				req = apistructs.UnSubscribeReq{
					Type:   apistructs.AppSubscribe,
					TypeID: 666,
					UserID: "999",
					OrgID:  2,
				}
			} else if tt.dByUserIDErr || tt.dByUserIDSuc {
				req = apistructs.UnSubscribeReq{
					UserID: "999",
					OrgID:  2,
				}
			} else if tt.dByIDErr || tt.dByIDSuc {
				req = apistructs.UnSubscribeReq{
					ID:     "idxxxx",
					UserID: "999",
					OrgID:  2,
				}
			}

			monkey.PatchInstanceMethod(reflect.TypeOf(c), "DeleteSubscribe", func(c *dao.DBClient, tp string, tpID uint64, userID string, orgID uint64) error {
				if tt.deleteErr {
					return errors.Errorf("error")
				}
				return nil
			})
			defer monkey.UnpatchAll()

			monkey.PatchInstanceMethod(reflect.TypeOf(c), "DeleteSubscribeByUserOrgID", func(c *dao.DBClient, userID string, orgID uint64) error {
				if tt.dByUserIDErr {
					return errors.Errorf("error")
				}
				return nil
			})

			monkey.PatchInstanceMethod(reflect.TypeOf(c), "DeleteBySubscribeID", func(c *dao.DBClient, id string) error {
				if tt.dByIDErr {
					return fmt.Errorf("error")
				}
				return nil
			})

			s := New(WithDBClient(&dao.DBClient{}))
			err := s.UnSubscribe(req)
			if (err != nil) != tt.wantErr {
				t.Errorf("Subscribe() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}
