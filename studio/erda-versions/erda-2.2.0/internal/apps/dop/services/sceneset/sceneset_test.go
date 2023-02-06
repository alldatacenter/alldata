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

package sceneset

import (
	"reflect"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

func TestService_sceneSetNameCheck(t *testing.T) {
	var db *dao.DBClient
	pm := monkey.PatchInstanceMethod(reflect.TypeOf(db), "FindSceneSetsByName",
		func(bdl *dao.DBClient, name string, spaceID uint64) ([]dao.SceneSet, error) {
			return []dao.SceneSet{
				{
					BaseModel: dbengine.BaseModel{
						ID: 1,
					},
					Name: "erda",
				},
				{
					BaseModel: dbengine.BaseModel{
						ID: 2,
					},
					Name: "dice",
				},
			}, nil
		})
	defer pm.Unpatch()

	type fields struct {
		db *dao.DBClient
	}
	type args struct {
		spaceID uint64
		name    string
		setID   uint64
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   bool
	}{
		{
			name:   "create test with not exit",
			fields: fields{db: db},
			args: args{
				spaceID: 1,
				name:    "erda2",
				setID:   0,
			},
			want: true,
		},
		{
			name:   "create test with exit",
			fields: fields{db: db},
			args: args{
				spaceID: 1,
				name:    "erda",
				setID:   0,
			},
			want: false,
		},
		{
			name:   "create test with sensitive",
			fields: fields{db: db},
			args: args{
				spaceID: 1,
				name:    "Erda",
				setID:   0,
			},
			want: true,
		},
		{
			name:   "update test with not exist",
			fields: fields{db: db},
			args: args{
				spaceID: 1,
				name:    "erda2",
				setID:   1,
			},
			want: true,
		},
		{
			name:   "update test with not exist2",
			fields: fields{db: db},
			args: args{
				spaceID: 1,
				name:    "erda",
				setID:   1,
			},
			want: true,
		},
		{
			name:   "update test with exist",
			fields: fields{db: db},
			args: args{
				spaceID: 1,
				name:    "dice",
				setID:   1,
			},
			want: false,
		},
		{
			name:   "update test with sensitive",
			fields: fields{db: db},
			args: args{
				spaceID: 1,
				name:    "Erda",
				setID:   1,
			},
			want: true,
		},
		{
			name:   "update test with sensitive2",
			fields: fields{db: db},
			args: args{
				spaceID: 1,
				name:    "Dice",
				setID:   1,
			},
			want: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			svc := &Service{
				db: tt.fields.db,
			}
			if got := svc.sceneSetCaseSensitiveNameCheck(tt.args.spaceID, tt.args.name, tt.args.setID); got != tt.want {
				t.Errorf("sceneSetNameCheck() = %v, want %v", got, tt.want)
			}
		})
	}
}
