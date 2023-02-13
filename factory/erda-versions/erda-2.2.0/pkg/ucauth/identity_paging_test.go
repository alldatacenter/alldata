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

	"github.com/erda-project/erda/apistructs"
)

var isLock = 0
var data = []*OryKratosIdentity{
	{
		ID: "1",
		Traits: OryKratosIdentityTraits{
			Name: "a1",
		},
		State: UserActive,
	},
	{
		ID: "2",
		Traits: OryKratosIdentityTraits{
			Name: "b2",
		},
		State: UserActive,
	},
	{
		ID: "3",
		Traits: OryKratosIdentityTraits{
			Name: "a3",
		},
		State: UserInActive,
	},
}

func Test_getUserList(t *testing.T) {
	type args struct {
		kratosPrivateAddr string
		req               *apistructs.UserPagingRequest
	}
	tests := []struct {
		name    string
		args    args
		want    []User
		want1   int
		wantErr bool
	}{
		{
			name: "search",
			args: args{
				req: &apistructs.UserPagingRequest{
					PageNo:   1,
					PageSize: 10,
					Name:     "a",
				},
			},
			want: []User{
				{
					ID:    "1",
					Name:  "a1",
					State: UserActive,
				},
				{
					ID:    "3",
					Name:  "a3",
					State: UserInActive,
				},
			},
			want1:   2,
			wantErr: false,
		},
		{
			name: "search active",
			args: args{
				req: &apistructs.UserPagingRequest{
					PageNo:   1,
					PageSize: 10,
					Name:     "a",
					Locked:   &isLock,
				},
			},
			want: []User{
				{
					ID:    "1",
					Name:  "a1",
					State: UserActive,
				},
			},
			want1:   1,
			wantErr: false,
		},
	}

	monkey.Patch(getIdentityPage,
		func(kratosPrivateAddr string, page, perPage int) ([]*OryKratosIdentity, error) {
			if page == 2 {
				return nil, nil
			}
			return data, nil
		})
	defer monkey.UnpatchAll()

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, got1, err := getUserList(tt.args.kratosPrivateAddr, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("getUserList() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getUserList() got = %v, want %v", got, tt.want)
			}
			if got1 != tt.want1 {
				t.Errorf("getUserList() got1 = %v, want %v", got1, tt.want1)
			}
		})
	}
}

func Test_paginate(t *testing.T) {
	type args struct {
		i        []*OryKratosIdentity
		pageNo   int
		pageSize int
	}
	tests := []struct {
		name string
		args args
		want []*OryKratosIdentity
	}{
		{
			name: "test",
			args: args{
				i:        data,
				pageNo:   1,
				pageSize: 15,
			},
			want: data,
		},
		{
			name: "empty page",
			args: args{
				i:        data,
				pageNo:   3,
				pageSize: 2,
			},
			want: nil,
		},
		{
			name: "first page",
			args: args{
				i:        data,
				pageNo:   1,
				pageSize: 2,
			},
			want: []*OryKratosIdentity{
				{
					ID: "1",
					Traits: OryKratosIdentityTraits{
						Name: "a1",
					},
					State: UserActive,
				},
				{
					ID: "2",
					Traits: OryKratosIdentityTraits{
						Name: "b2",
					},
					State: UserActive,
				},
			},
		},
		{
			name: "second page",
			args: args{
				i:        data,
				pageNo:   2,
				pageSize: 2,
			},
			want: []*OryKratosIdentity{
				{
					ID: "3",
					Traits: OryKratosIdentityTraits{
						Name: "a3",
					},
					State: UserInActive,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := paginate(tt.args.i, tt.args.pageNo, tt.args.pageSize); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("paginate() = %v, want %v", got, tt.want)
			}
		})
	}
}
