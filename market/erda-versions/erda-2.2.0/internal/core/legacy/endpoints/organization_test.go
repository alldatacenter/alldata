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

package endpoints

import (
	"net/http"
	"net/url"
	"reflect"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda/internal/core/legacy/services/member"
)

func TestEndpoints_getOrgPermissions(t *testing.T) {
	type args struct {
		r *http.Request
	}
	tests := []struct {
		name    string
		args    args
		want    bool
		want1   []int64
		wantErr bool
	}{
		{
			name: "",
			args: args{
				r: &http.Request{
					URL: &url.URL{},
				},
			},
			want1:   nil,
			want:    true,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			e := &Endpoints{}
			var me = &member.Member{}
			patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(me), "IsAdmin", func(mem *member.Member, userID string) bool {
				return true
			})
			defer patch1.Unpatch()
			e.member = me

			got, got1, err := e.getOrgPermissions(tt.args.r)
			if (err != nil) != tt.wantErr {
				t.Errorf("getOrgPermissions() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("getOrgPermissions() got = %v, want %v", got, tt.want)
			}
			if !reflect.DeepEqual(got1, tt.want1) {
				t.Errorf("getOrgPermissions() got1 = %v, want %v", got1, tt.want1)
			}
		})
	}
}
