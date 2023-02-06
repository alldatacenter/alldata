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

package clusterdialer

import (
	"context"
	"testing"
	"time"
)

func TestTunnelSession_getClusterDialer_timeout(t *testing.T) {
	sessionCtx, cancel := context.WithCancel(context.Background())
	session := TunnelSession{
		expired: sessionCtx,
		cancel:  cancel,
	}
	go session.initialize("ws://127.0.0.1")
	ctx, _ := context.WithTimeout(context.Background(), 5*time.Second)
	dialer := session.getClusterDialer(ctx, "test")
	if dialer != nil {
		t.Error("dialer is not nil")
	}
}
