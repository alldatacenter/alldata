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

package orykratos

import (
	"testing"
)

func Test_redirectUrl(t *testing.T) {
	type args struct {
		referer string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			args: args{
				referer: "https://erda.cloud/erda",
			},
			want: "/uc/login?redirectUrl=https%3A%2F%2Ferda.cloud%2Ferda",
		},
		{
			want: "/uc/login",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := redirectUrl(tt.args.referer); got != tt.want {
				t.Errorf("redirectUrl() = %v, want %v", got, tt.want)
			}
		})
	}
}
