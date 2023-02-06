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

package apis

import (
	"context"
	"net"
	"strings"

	"google.golang.org/grpc/peer"

	"github.com/erda-project/erda-infra/pkg/transport"
	transhttp "github.com/erda-project/erda-infra/pkg/transport/http"
)

// WithInternalClientContext .
func WithInternalClientContext(ctx context.Context, internalClient string) context.Context {
	header := transport.Header{}
	header.Set(headerInternalClient, internalClient)
	return transport.WithHeader(ctx, header)
}

func WithUserIDContext(ctx context.Context, userID string) context.Context {
	header := transport.Header{}
	header.Set(headerUserID, userID)
	return transport.WithHeader(ctx, header)
}

// GetClientIP
func GetClientIP(ctx context.Context) string {
	header := transport.ContextHeader(ctx)
	if header != nil {
		for _, key := range []string{"X-Forwarded-For", "X-Real-IP"} {
			for _, v := range header.Get(key) {
				if len(v) > 0 {
					return strings.Split(v, ",")[0]
				}
			}
		}
	}

	if req := transhttp.ContextRequest(ctx); req != nil {
		ip, _, err := net.SplitHostPort(req.RemoteAddr)
		if err != nil {
			return req.RemoteAddr
		}
		return ip
	} else if pr, ok := peer.FromContext(ctx); ok {
		if tcpAddr, ok := pr.Addr.(*net.TCPAddr); ok {
			return tcpAddr.IP.String()
		} else {
			addr := pr.Addr.String()
			ip, _, err := net.SplitHostPort(addr)
			if err != nil {
				return addr
			}
			return ip
		}
	}
	return ""
}
