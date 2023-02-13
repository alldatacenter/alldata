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

package auth

import (
	"net/http"
	"reflect"
	"testing"

	gomock "github.com/golang/mock/gomock"

	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func TestVerifyAccessKey(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	tokenService := NewMockTokenServiceServer(ctrl)
	tokenService.EXPECT().QueryTokens(gomock.Any(), gomock.Any()).AnyTimes().Return(&tokenpb.QueryTokensResponse{
		Data: []*tokenpb.Token{
			{
				Id:      "1",
				ScopeId: "2",
			},
		},
		Total: 1,
	}, nil)
	r := http.Request{
		Header: make(http.Header),
	}
	r.Header.Add(HeaderAuthorization, "Bearer abcdef")
	tests := []struct {
		name    string
		want    TokenClient
		wantErr bool
	}{
		{
			name: "valid",
			want: TokenClient{
				"2", "2",
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := VerifyAccessKey(tokenService, &r)
			if (err != nil) != tt.wantErr {
				t.Errorf("VerifyAccessKey() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("VerifyAccessKey() = %v, want %v", got, tt.want)
			}
			if !reflect.DeepEqual(r.Header.Get(httputil.InternalHeader), "2") {
				t.Errorf("internal client header = %v, want %v", got, "2")
			}
		})
	}
}
