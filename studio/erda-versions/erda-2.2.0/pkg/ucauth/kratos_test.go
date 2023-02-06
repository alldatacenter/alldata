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
)

func Test_getUserByKey(t *testing.T) {
	type args struct {
		kratosPrivateAddr string
		key               string
	}
	tests := []struct {
		name    string
		args    args
		want    []User
		wantErr bool
	}{
		{
			name: "test",
			args: args{
				key: "a",
			},
			want: []User{
				{
					ID:    "1",
					Name:  "a1",
					State: UserActive,
				},
				{
					ID:    "2",
					Nick:  "a2",
					State: UserActive,
				},
			},
			wantErr: false,
		},
	}

	monkey.Patch(getUserPage,
		func(kratosPrivateAddr string, page, perPage int) ([]User, error) {
			if page == 2 {
				return nil, nil
			}
			return []User{
				{
					ID:    "1",
					Name:  "a1",
					State: UserActive,
				},
				{
					ID:    "2",
					Nick:  "a2",
					State: UserActive,
				},
				{
					ID:    "3",
					Email: "abc@gmail.com",
					State: UserInActive,
				},
			}, nil
		})
	defer monkey.UnpatchAll()

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := getUserByKey(tt.args.kratosPrivateAddr, tt.args.key)
			if (err != nil) != tt.wantErr {
				t.Errorf("getUserByKey() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getUserByKey() = %v, want %v", got, tt.want)
			}
		})
	}
}
