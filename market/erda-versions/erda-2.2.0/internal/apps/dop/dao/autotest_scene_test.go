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

package dao

import (
	"fmt"
	"testing"

	"bou.ke/monkey"
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/pkg/database/dbengine"
)

func TestDBClient_MoveAutoTestScene(t *testing.T) {
	type args struct {
		id       uint64
		newPreID uint64
		newSetID uint64
		tx       *gorm.DB
	}

	// a < b < c < d < e
	// 1 < 2 < 3 < 4 < 5
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "change a b",
			args: args{
				id:       4,
				newPreID: 1,
				newSetID: 0,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			db := &DBClient{}
			patch := monkey.Patch(getScene, func(tx *gorm.DB, id uint64) (AutoTestScene, error) {
				if tt.name == "change a b" {
					if id == tt.args.id {
						return AutoTestScene{
							BaseModel: dbengine.BaseModel{
								ID: tt.args.id,
							},
							PreID: 3,
						}, nil
					}
					if id == tt.args.newPreID {
						return AutoTestScene{
							BaseModel: dbengine.BaseModel{
								ID: tt.args.newPreID,
							},
						}, nil
					}
				}
				return AutoTestScene{}, fmt.Errorf("not find")
			})
			defer patch.Unpatch()

			patch1 := monkey.Patch(getSceneByPreID, func(tx *gorm.DB, preID uint64, setID uint64) (AutoTestScene, error) {
				if tt.name == "change a b" {
					if preID == tt.args.id {
						return AutoTestScene{
							BaseModel: dbengine.BaseModel{
								ID: 5,
							},
							PreID: tt.args.id,
						}, nil
					}
					if preID == tt.args.newPreID {
						return AutoTestScene{
							BaseModel: dbengine.BaseModel{
								ID: 2,
							},
							PreID: tt.args.newPreID,
						}, nil
					}
				}
				return AutoTestScene{}, fmt.Errorf("not find")
			})
			defer patch1.Unpatch()

			patch2 := monkey.Patch(updateScenePreID, func(tx *gorm.DB, id uint64, preID uint64, newPreID uint64, newSetID uint64) error {
				return nil
			})
			defer patch2.Unpatch()

			patch3 := monkey.Patch(checkSceneSetNotHaveSamePreID, func(tx *gorm.DB, setID uint64) error {
				return nil
			})
			defer patch3.Unpatch()

			if err := db.MoveAutoTestScene(tt.args.id, tt.args.newPreID, tt.args.newSetID, tt.args.tx); (err != nil) != tt.wantErr {
				t.Errorf("MoveAutoTestScene() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
