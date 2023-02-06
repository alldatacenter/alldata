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

package apitestsv2

import (
	"reflect"
	"testing"

	"github.com/alecthomas/assert"

	"github.com/erda-project/erda/apistructs"
)

func Test_getK8sNamespace(t *testing.T) {
	type args struct {
		k8sHost string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			args: args{
				k8sHost: "a.default.svc.cluster.local",
			},
			want: "default",
		},
		{
			args: args{
				k8sHost: "web.n1.svc.cluster.local",
			},
			want: "n1",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getK8sNamespace(tt.args.k8sHost); got != tt.want {
				t.Errorf("getK8sNamespace() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_checkNetportal(t *testing.T) {
	type args struct {
		url          string
		netportalOpt *netportalOption
	}
	tests := []struct {
		name   string
		args   args
		hasErr bool
	}{
		{
			name: "no netportal info",
			args: args{
				netportalOpt: nil,
			},
			hasErr: true,
		},
		{
			name: "empty netportal url",
			args: args{
				netportalOpt: &netportalOption{url: ""},
			},
			hasErr: true,
		},
		{
			name: "a valid service host",
			args: args{
				url: "http://web.n1.svc.cluster.local:8080",
				netportalOpt: &netportalOption{
					url:                           "netportal:80",
					blacklistOfK8sNamespaceAccess: nil,
				},
			},
			hasErr: false,
		},
		{
			name: "cannot access ns in blacklist",
			args: args{
				url: "http://web.n1.svc.cluster.local:8080",
				netportalOpt: &netportalOption{
					url:                           "netportal:80",
					blacklistOfK8sNamespaceAccess: []string{"n2", "n1"},
				},
			},
			hasErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			err := checkNetportal(tt.args.url, tt.args.netportalOpt)
			if tt.hasErr {
				assert.Error(t, err)
			} else {
				assert.NoError(t, err)
			}
		})
	}
}

func Test_handleCustomNetportalRequest(t *testing.T) {
	//inetAddr := "web.n1.svc.cluster.local:80"
	netportalURL := "inet://xxx.yyy"
	type args struct {
		apiReq       *apistructs.APIRequestInfo
		netportalOpt *netportalOption
	}
	type want struct {
		schema string
		host   string
	}
	tests := []struct {
		name    string
		args    args
		want    want
		wantErr bool
	}{
		{
			name: "public network url, do not use netportal",
			args: args{
				apiReq:       &apistructs.APIRequestInfo{URL: "https://www.erda.cloud"},
				netportalOpt: nil,
			},
			want: want{
				schema: "https",
				host:   "www.erda.cloud",
			},
			wantErr: true,
		},
		{
			name: "internal service url, but in blacklist, do not use netportal",
			args: args{
				apiReq: &apistructs.APIRequestInfo{URL: "http://web.n1.svc.cluster.local:8080"},
				netportalOpt: &netportalOption{
					url:                           netportalURL,
					blacklistOfK8sNamespaceAccess: []string{"n2", "n1"},
				},
			},
			want: want{
				schema: "http",
				host:   "web.n1.svc.cluster.local:8080",
			},
			wantErr: true,
		},
		//{
		//	name: "internal service url, NOT in blacklist, use netportal",
		//	args: args{
		//		apiReq: &apistructs.APIRequestInfo{URL: "http://web.n1.svc.cluster.local:8080", Headers: make(http.Header)},
		//		netportalOpt: &netportalOption{
		//			url:                           netportalURL,
		//			blacklistOfK8sNamespaceAccess: []string{"n2"},
		//		},
		//	},
		//	want: want{
		//		schema: "http",
		//		host:   inetAddr,
		//	},
		//	wantErr: false,
		//},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := handleCustomNetportalRequest(tt.args.apiReq, tt.args.netportalOpt)
			if (err != nil) != tt.wantErr {
				t.Errorf("handleCustomNetportalRequest() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got == nil {
				return
			}
			_got := want{
				schema: got.URL.Scheme,
				host:   got.Host,
			}
			if !reflect.DeepEqual(_got, tt.want) {
				t.Errorf("handleCustomNetportalRequest() got = %v, want %v", _got, tt.want)
			}
		})
	}
}
