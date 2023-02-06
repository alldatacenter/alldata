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
	"encoding/json"
	"fmt"
	"io"
	"net"
	"net/http"
	"strings"
	"sync"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/cache"
	"github.com/erda-project/erda/pkg/discover"
)

var (
	sessions = sync.Map{}
	ipCache  = cache.New("clusterManagerEndpoint", time.Second*30, queryClusterManagerIP)
)

type DialContextFunc func(ctx context.Context, network, address string) (net.Conn, error)
type DialContextProtoFunc func(ctx context.Context, address string) (net.Conn, error)

func DialContext(clusterKey string) DialContextFunc {
	return func(ctx context.Context, network, addr string) (net.Conn, error) {
		session, err := getSession(clusterKey)
		if err != nil {
			return nil, errors.Errorf("failed to get session for clusterKey %s, %v", clusterKey, err)
		}
		logrus.Debugf("use cluster dialer, key:%s, addr:%s", clusterKey, addr)
		f := session.getClusterDialer(ctx, clusterKey)
		if f == nil {
			return nil, errors.New("get cluster dialer failed")
		}
		return f(ctx, network, addr)
	}
}

func DialContextProto(clusterKey, proto string) DialContextProtoFunc {
	return func(ctx context.Context, addr string) (net.Conn, error) {
		session, err := getSession(clusterKey)
		if err != nil {
			return nil, errors.Errorf("failed to get session for clusterKey %s, %v", clusterKey, err)
		}
		logrus.Debugf("use cluster dialer, key:%s, addr:%s", clusterKey, addr)
		f := session.getClusterDialer(ctx, clusterKey)
		if f == nil {
			return nil, errors.New("get cluster dialer failed")
		}
		return f(ctx, proto, addr)
	}
}

func DialContextTCP(clusterKey string) DialContextProtoFunc {
	return DialContextProto(clusterKey, "tcp")
}

func queryClusterManagerIP(clusterKey interface{}) (interface{}, bool) {
	log := logrus.WithField("func", "DialContext")
	log.Debugf("start querying clusterManager IP in dialContext...")

	splits := strings.Split(discover.ClusterDialer(), ":")
	if len(splits) != 2 {
		log.Errorf("invalid clusterManager addr: %s", discover.ClusterDialer())
		return "", false
	}
	addr := splits[0]
	port := splits[1]
	host := fmt.Sprintf("http://%s:%s", addr, port)
	resp, err := http.Get(host + fmt.Sprintf("/clusterdialer/ip?clusterKey=%s", clusterKey))
	if err != nil {
		log.Errorf("failed to request clusterManager in cache updating in dialContext, %v", err)
		return "", false
	}

	data, err := io.ReadAll(resp.Body)
	if err != nil {
		log.Errorf("failed to read from resp in cache updating, %v", err)
		return "", false
	}
	r := make(map[string]interface{})
	if err = json.Unmarshal(data, &r); err != nil {
		log.Errorf("failed to unmarshal resp in cache updating, %v", err)
		return "", false
	}

	succeeded, _ := r["succeeded"].(bool)
	if !succeeded {
		errStr, _ := r["error"].(string)
		log.Errorf("return error from clusterManager in cache updating, %s", errStr)
		return "", false
	}

	ip, _ := r["IP"].(string)
	return fmt.Sprintf("%s:%s", ip, port), true
}

func getSession(clusterKey string) (*TunnelSession, error) {
	clusterManagerEndpoint, ok := ipCache.LoadWithUpdateSync(clusterKey)
	if !ok {
		logrus.Errorf("failed to get clusterManager endpoint for clusterKey %s", clusterKey)
		return nil, errors.Errorf("failed to get clusterManager endpoint for clusterKey %s", clusterKey)
	}
	if clusterManagerEndpoint == "" {
		return nil, errors.Errorf("can not found clusterManager endpoint for clusterKey %s", clusterKey)
	}
	logrus.Debugf("[DEBUG] get clusterManager endpoint succeeded, IP: %s", clusterManagerEndpoint)

	var session *TunnelSession
	v, ok := sessions.Load(clusterManagerEndpoint)
	if !ok {
		ctx, cancel := context.WithCancel(context.Background())
		session = &TunnelSession{expired: ctx, cancel: cancel, clusterManagerEndpoint: clusterManagerEndpoint.(string)}
		go session.initialize(fmt.Sprintf("ws://%s%s", clusterManagerEndpoint, "/clusterdialer"))
		sessions.Store(clusterManagerEndpoint, session)
	} else {
		session, _ = v.(*TunnelSession)
	}
	return session, nil
}
