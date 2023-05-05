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

package hmac

import (
	"net/http"
	"strings"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/pkg/secret"
)

func TestSignature(t *testing.T) {
	s := New(mockKeyPair)
	assert.Equal(t, "f91708d826817ce3b6b28add8a5914a3f1c7419f", s.Signature("abc"))
}

func Test_getSignString(t *testing.T) {
	s := New(mockKeyPair)
	r := mockRequest()

	assert.Equal(t, "GET\n\n/users\npage=1&pageNum=10\nX-Erda-Sdk=true&X-Erda-Version=0.1.0", s.GetSignString(r))

	r.Header.Add("x-erda-aaa", "b")
	r.Header.Add("x-erda-aaa", "a")
	r.URL.RawQuery = "page=1&pageNum=10&bbb=c&bbb=b"
	assert.Equal(t, "GET\n\n/users\nbbb=b&bbb=c&page=1&pageNum=10\nX-Erda-Aaa=a&X-Erda-Aaa=b&X-Erda-Sdk=true&X-Erda-Version=0.1.0", s.GetSignString(r))
}

func TestWithTimestampNow(t *testing.T) {
	tt, err := time.Parse("2006-01-02 15:04:05", "2021-05-16 00:00:00")
	assert.Nil(t, err)
	s := New(mockKeyPair, WithTimestamp(tt))
	r := mockRequest()
	s.SignCanonicalRequest(r)
	assert.Equal(t, "X-Erda-Ak=IQ9E2Buhd8z2h7njPaxeGxq8&X-Erda-Signature=8b6e479c84d975023d6a554e1f252e7cb1efe1b2&X-Erda-Sign-Algorithm=hmac-sha1&X-Erda-Sign-Timestamp=1621123200", r.Header.Get("Authorization"))
}

func TestSigner_SignCanonicalRequest(t *testing.T) {
	type fields struct {
		timestampEnable   bool
		authInQueryString bool
		nowTimestamp      string
		keyPair           secret.AkSkPair
	}
	type args struct {
		r *http.Request
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   map[string]string
	}{
		{
			"",
			fields{
				keyPair: mockKeyPair,
			},
			args{r: mockRequest()},
			map[string]string{
				ErdaAccessKeyID:   "IQ9E2Buhd8z2h7njPaxeGxq8",
				ErdaSignAlgorithm: "hmac-sha1",
				ErdaSignature:     "cb63fc58c286c72827bee6e781f4c3f4c8792347",
			},
		},
		{
			"with timestamp",
			fields{
				keyPair:         mockKeyPair,
				timestampEnable: true,
				nowTimestamp:    "1620724985",
			},
			args{r: mockRequest()},
			map[string]string{
				ErdaAccessKeyID:   "IQ9E2Buhd8z2h7njPaxeGxq8",
				ErdaSignAlgorithm: "hmac-sha1",
				ErdaSignature:     "54967da400d8b23586a0b6922b6a933e67eeaa4d",
				ErdaSignTimestamp: "1620724985",
			},
		},
		{
			"auth in query string",
			fields{
				keyPair:           mockKeyPair,
				authInQueryString: true,
			},
			args{r: mockRequest()},
			map[string]string{
				ErdaAccessKeyID:   "IQ9E2Buhd8z2h7njPaxeGxq8",
				ErdaSignAlgorithm: "hmac-sha1",
				ErdaSignature:     "cb63fc58c286c72827bee6e781f4c3f4c8792347",
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &Signer{
				keyPair:           tt.fields.keyPair,
				authInQueryString: tt.fields.authInQueryString,
				timestampEnable:   tt.fields.timestampEnable,
				nowTimestamp:      tt.fields.nowTimestamp,
			}
			s.SignCanonicalRequest(tt.args.r)
			if s.authInQueryString {
				assert.Equal(t, tt.want, parseAuth(tt.args.r.URL.RawQuery))
			} else {
				assert.Equal(t, tt.want, parseAuth(tt.args.r.Header.Get("Authorization")))
			}
		})
	}
}

var mockKeyPair = secret.AkSkPair{
	AccessKeyID: "IQ9E2Buhd8z2h7njPaxeGxq8",
	SecretKey:   "0O2Hn0TrTrRwrds1q0un0p9AvX4JB8V6",
}

func mockRequest() *http.Request {
	r, _ := http.NewRequest(http.MethodGet, "https://example.com/users?page=1&pageNum=10", nil)
	r.Header.Set("x-erda-sdk", "true")
	r.Header.Set("x-erda-version", "0.1.0")
	r.Header.Set("content-type", "application/json")
	return r
}

func parseAuth(s string) map[string]string {
	res := make(map[string]string)
	for _, item := range strings.Split(s, "&") {
		kv := strings.Split(item, "=")
		if len(kv) == 2 && strings.HasPrefix(kv[0], ErdaHeaderPrefix) {
			res[kv[0]] = kv[1]
		}
	}
	return res
}
