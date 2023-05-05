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

package legacy

import (
	"fmt"
	"net/http"
	"strings"
	"time"

	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/conf"
)

func NewServer(token tokenpb.TokenServiceServer) (*http.Server, error) {
	s, err := NewLoginServer(token)
	if err != nil {
		return nil, err
	}

	srv := &http.Server{
		Addr:              conf.ListenAddr(),
		Handler:           s,
		ReadHeaderTimeout: 60 * time.Second, // TODO: test whether will timeout option affect websocket
	}
	return srv, nil
}

func IsHTTPS(req *http.Request) (bool, error) {
	referrer := req.Header.Get("Referer")
	if referrer == "" {
		return false, fmt.Errorf("no Referer header")
	}
	return strings.HasPrefix(referrer, "https:"), nil
}

func replaceProto(isHTTPS bool, v string) string {
	if isHTTPS {
		v = strings.Replace(v, "https", "http", -1)
		v = strings.Replace(v, "http", "https", -1)
	} else {
		v = strings.Replace(v, "https", "http", -1)
	}
	return v
}
