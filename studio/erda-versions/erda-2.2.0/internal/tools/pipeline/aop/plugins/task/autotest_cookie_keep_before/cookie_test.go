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

package autotest_cookie_keep_before

import (
	"testing"
)

func Test_appendOrReplaceSetCookiesToCookie(t *testing.T) {
	type args struct {
		setCookies   []string
		originCookie string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "all empty",
			args: args{
				setCookies:   nil,
				originCookie: "",
			},
			want: "",
		},
		{
			name: "set-cookie of one existed cookie",
			args: args{
				setCookies:   []string{`cookie_a=aa; Path=/; Domain=xxx; Expires=Fri, 03 Sep 2021 15:12:15 GMT`},
				originCookie: "cookie_a=a; cookie_b=b",
			},
			want: "cookie_a=aa; cookie_b=b",
		},
		{
			name: "set-cookie of tow existed cookie(a,c) and append one(d), and originCookie is splitted by `;`, not `; `",
			args: args{
				setCookies:   []string{`cookie_a=aa; Path=/; Domain=xxx; Expires=Fri, 03 Sep 2021 15:12:15 GMT`, `cookie_c=C`, `cookie_d=dd`},
				originCookie: "cookie_a=a;cookie_b=b;cookie_c=c",
			},
			want: "cookie_a=aa; cookie_b=b; cookie_c=C; cookie_d=dd",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := appendOrReplaceSetCookiesToCookie(tt.args.setCookies, tt.args.originCookie); got != tt.want {
				t.Errorf("appendOrReplaceSetCookiesToCookie() = %v, want %v", got, tt.want)
			}
		})
	}
}
