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

package server

import (
	"context"
	"fmt"
	"io"
	"io/ioutil"
	"net/http"
	"os"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/coreos/etcd/clientv3"
	"github.com/coreos/etcd/mvcc/mvccpb"
	"github.com/gorilla/mux"
	"github.com/sirupsen/logrus"

	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/cluster-manager/dialer/auth"
	"github.com/erda-project/erda/internal/core/cluster-manager/dialer/config"
	clientconfig "github.com/erda-project/erda/internal/tools/cluster-agent/config"
	clusteragent "github.com/erda-project/erda/internal/tools/cluster-agent/pkg/client"
	"github.com/erda-project/erda/pkg/clusterdialer"
	"github.com/erda-project/erda/pkg/discover"
)

const (
	dialerListenAddr = "127.0.0.1"
	dialerListenPort = "18751"
	helloListenAddr  = "127.0.0.1:18752"
)

func startServer(etcd *clientv3.Client) (context.Context, context.CancelFunc) {
	ctx, cancel := context.WithCancel(context.Background())
	go Start(ctx, &fakeClusterSvc{}, nil, &config.Config{
		Listen:          fmt.Sprintf("%s:%s", dialerListenAddr, dialerListenPort),
		NeedClusterInfo: false,
	}, etcd, &bundle.Bundle{})
	return ctx, cancel
}

type fakeKV struct {
	clientv3.KV
}

func (f *fakeKV) Get(ctx context.Context, key string, opts ...clientv3.OpOption) (*clientv3.GetResponse, error) {
	return &clientv3.GetResponse{
		Kvs: []*mvccpb.KeyValue{{
			Value: []byte(dialerListenAddr),
		}},
	}, nil
}

func (f *fakeKV) Put(ctx context.Context, key, val string, opts ...clientv3.OpOption) (*clientv3.PutResponse, error) {
	return nil, nil
}

type fakeClusterSvc struct {
	clusterpb.ClusterServiceServer
}

func (f *fakeClusterSvc) GetCluster(context.Context, *clusterpb.GetClusterRequest) (*clusterpb.GetClusterResponse, error) {
	return &clusterpb.GetClusterResponse{Data: &clusterpb.ClusterInfo{Name: fakeClusterKey}}, nil
}

func (f *fakeClusterSvc) PatchCluster(context.Context, *clusterpb.PatchClusterRequest) (*clusterpb.PatchClusterResponse, error) {
	return &clusterpb.PatchClusterResponse{}, nil
}

func Test_DialerContext(t *testing.T) {
	defer monkey.UnpatchAll()

	logrus.SetLevel(logrus.DebugLevel)
	authorizer := auth.New(auth.WithCredentialClient(nil))
	monkey.Patch(authorizer.Authorizer, func(req *http.Request) (string, bool, error) {
		return fakeClusterKey, true, nil
	})

	client := clusteragent.New(clusteragent.WithConfig(&clientconfig.Config{
		ClusterManagerEndpoint: fmt.Sprintf("ws://%s/clusteragent/connect", fmt.Sprintf("%s:%s", dialerListenAddr, dialerListenPort)),
		ClusterKey:             fakeClusterKey,
		CollectClusterInfo:     false,
		ClusterAccessKey:       fakeClusterAccessKey,
	}))

	ctx, cancel := startServer(&clientv3.Client{KV: &fakeKV{}})
	helloHandler := func(w http.ResponseWriter, req *http.Request) {
		io.WriteString(w, "Hello, world!")
	}
	mx := mux.NewRouter()
	mx.HandleFunc("/hello", helloHandler)
	go http.ListenAndServe(helloListenAddr, mx)

	go client.Start(ctx)
	for {
		if client.IsConnected() {
			logrus.Info("client connected at Test_DialerContext")
			break
		}
		time.Sleep(1 * time.Second)
	}

	os.Setenv(discover.EnvClusterDialer, fmt.Sprintf("%s:%s", dialerListenAddr, dialerListenPort))
	hc := http.Client{
		Transport: &http.Transport{
			DialContext: clusterdialer.DialContext(fakeClusterKey),
		},
		Timeout: 10 * time.Second,
	}
	req, _ := http.NewRequest("GET", fmt.Sprintf("http://%s/hello", helloListenAddr), nil)
	resp, err := hc.Do(req)
	if err != nil {
		t.Errorf("dialer failed, err:%+v", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != 200 {
		t.Errorf("status:%d expect:200", resp.StatusCode)
		return
	}
	respBody, _ := ioutil.ReadAll(resp.Body)
	if string(respBody) != "Hello, world!" {
		t.Errorf("respBody:%s, expect:Hello, world!", respBody)
		return
	}
	cancel()
	select {
	case <-ctx.Done():
	}
}
