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
	"math/rand"
	"net/http"
	"sync"
	"time"

	"github.com/gorilla/websocket"
	"github.com/rancher/remotedialer"
	"github.com/sirupsen/logrus"
)

const HandshakeTimeOut = 10 * time.Second

type TunnelSession struct {
	session                *remotedialer.Session
	lock                   sync.Mutex
	expired                context.Context
	cancel                 context.CancelFunc
	clusterManagerEndpoint string
}

func (s *TunnelSession) initialize(endpoint string) {
	headers := http.Header{
		"X-API-Tunnel-Proxy": []string{"on"},
	}
	dialer := &websocket.Dialer{
		HandshakeTimeout: HandshakeTimeOut,
	}
	for {
		ws, _, err := dialer.Dial(endpoint, headers)
		if err != nil {
			logrus.Errorf("Failed to connect to proxy server %s, err: %v", endpoint, err)
			s.cancel()
			sessions.Delete(s.clusterManagerEndpoint)
			return
		}
		s.lock.Lock()
		s.session = remotedialer.NewClientSession(func(string, string) bool { return true }, ws)
		s.lock.Unlock()
		_, err = s.session.Serve(context.Background())
		if err != nil {
			logrus.Errorf("Failed to serve proxy connection err: %v", err)
		}
		s.session.Close()
		s.lock.Lock()
		s.session = nil
		s.lock.Unlock()
		ws.Close()
		// retry connect after sleep a random time
		time.Sleep(time.Duration(rand.Int()%10) * time.Second)
	}

}

func (s *TunnelSession) getClusterDialer(ctx context.Context, clusterKey string) remotedialer.Dialer {
	var session *remotedialer.Session
	start := time.Now()
	for {
		s.lock.Lock()
		session = s.session
		s.lock.Unlock()
		if session != nil {
			break
		}
		select {
		case <-s.expired.Done():
			logrus.Infof("clusterdial session for clusterKey %s canceled", clusterKey)
			return nil
		case <-ctx.Done():
			logrus.Errorf("get clusterdial session failed for clusterKey %s, cost %.3fs", clusterKey, time.Since(start).Seconds())
			return nil
		case <-time.After(1 * time.Second):
			logrus.Infof("waiting for clusterdial session ready for clusterKey %s... ", clusterKey)
		}
	}
	return remotedialer.ToDialer(session, clusterKey)
}
