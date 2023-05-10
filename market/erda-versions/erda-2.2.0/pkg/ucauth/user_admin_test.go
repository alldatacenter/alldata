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

package ucauth

import (
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/pkg/http/httpclient"
)

func Test_filterUserIDs(t *testing.T) {
	type args struct {
		ids   []string
		users map[string]string
	}
	tests := []struct {
		name string
		args args
		want []string
	}{
		{
			name: "test",
			args: args{
				ids: []string{"2", "af4bdd0c-ea6a-4941-b12e-2cc434ffc395"},
				users: map[string]string{
					"2": "70cfad51-8740-4f0b-be84-6ea63987de88",
				},
			},
			want: []string{"70cfad51-8740-4f0b-be84-6ea63987de88", "af4bdd0c-ea6a-4941-b12e-2cc434ffc395"},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := filterUserIDs(tt.args.ids, tt.args.users); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("filterUserIDs() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestUCClient_ConvertUserIDs(t *testing.T) {
	type fields struct {
		baseURL     string
		isOry       bool
		client      *httpclient.HTTPClient
		ucTokenAuth *UCTokenAuth
		db          *gorm.DB
	}
	type args struct {
		ids []string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    []string
		want1   map[string]string
		wantErr bool
	}{
		{
			name: "test",
			args: args{
				ids: []string{"2", "af4bdd0c-ea6a-4941-b12e-2cc434ffc395"},
			},
			want: []string{"70cfad51-8740-4f0b-be84-6ea63987de88", "af4bdd0c-ea6a-4941-b12e-2cc434ffc395"},
			want1: map[string]string{
				"70cfad51-8740-4f0b-be84-6ea63987de88": "2",
			},
			wantErr: false,
		},
	}

	client := &UCClient{}
	pm := monkey.PatchInstanceMethod(reflect.TypeOf(client), "GetUserIDMapping", func(c *UCClient, ids []string) ([]UserIDModel, error) {
		return []UserIDModel{
			{
				ID:     "2",
				UserID: "70cfad51-8740-4f0b-be84-6ea63987de88",
			},
		}, nil
	})
	defer pm.Unpatch()

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, got1, err := client.ConvertUserIDs(tt.args.ids)
			if (err != nil) != tt.wantErr {
				t.Errorf("UCClient.ConvertUserIDs() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("UCClient.ConvertUserIDs() got = %v, want %v", got, tt.want)
			}
			if !reflect.DeepEqual(got1, tt.want1) {
				t.Errorf("UCClient.ConvertUserIDs() got1 = %v, want %v", got1, tt.want1)
			}
		})
	}
}
