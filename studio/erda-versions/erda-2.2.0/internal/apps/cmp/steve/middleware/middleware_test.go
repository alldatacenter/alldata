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

package middleware

import (
	"bytes"
	"context"
	"fmt"
	"math/rand"
	"net"
	"net/http"
	"testing"
	"time"

	"github.com/gorilla/mux"
	"github.com/sirupsen/logrus"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	v1 "k8s.io/client-go/kubernetes/typed/core/v1"

	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

var (
	opts option
	log  *logrus.Logger
)

type option struct {
	port int
}

type Server struct {
	//auth   *Authenticator
	rw          http.ResponseWriter
	r           *http.Response
	chains      Chain
	audit       http.Handler
	router      *mux.Router
	httpHandler http.Handler
}

func handler() http.HandlerFunc {
	return func(http.ResponseWriter, *http.Request) {}
}

func (s *Server) ServeHTTP(rw http.ResponseWriter, r *http.Request) {
	res := s.chains.Handler(s.httpHandler)
	res.ServeHTTP(rw, r)
	frame := []byte("123213213")
	_, err := rw.Write(frame)
	if err != nil {
		logrus.Error(err)
		return
	}
}
func Init() {
	log = logrus.New()
	p := rand.Intn(32767) + 1024
	opts = option{port: p}
}

type fakeClusterServiceServer struct {
	clusterpb.ClusterServiceServer
}

func (f *fakeClusterServiceServer) ListCluster(context.Context, *clusterpb.ListClusterRequest) (*clusterpb.ListClusterResponse, error) {
	return &clusterpb.ListClusterResponse{Data: []*clusterpb.ClusterInfo{
		{
			Name: "testCluster",
		},
	}}, nil
}

func TestChain(t *testing.T) {
	Init()
	bundleOpts := []bundle.Option{
		bundle.WithHTTPClient(
			httpclient.New(
				httpclient.WithTimeout(time.Second, time.Second*60),
			)),
		bundle.WithPipeline(),
		bundle.WithScheduler(),
		bundle.WithMonitor(),
		bundle.WithCoreServices(),
		bundle.WithOrchestrator(),
		bundle.WithDiceHub(),
		bundle.WithClusterManager(),
	}
	bdl := bundle.New(bundleOpts...)

	server := opts
	l, err := net.Listen("tcp", fmt.Sprintf("127.0.0.1:%d", server.port))
	if err != nil {
		t.Error(err)
		return
	}
	auth := NewAuthenticator(bdl, &fakeClusterServiceServer{})
	audit := NewAuditor(bdl)
	shell := NewShellHandler(context.Background())
	chain := Chain{
		audit.AuditMiddleWare,
		shell.HandleShell,
		auth.AuthMiddleware,
	}
	s := Server{
		router:      mux.NewRouter(),
		chains:      chain,
		httpHandler: handler(),
	}

	//s.auth = auth
	go func() {
		err := http.Serve(l, &s)
		if err != nil {
			t.Error(err)
		}
	}()

	hc := httpclient.New(httpclient.WithTimeout(time.Second, time.Second*60))
	buf := bytes.NewBuffer(make([]byte, 1024))
	hc.Post(fmt.Sprintf("127.0.0.1:%d/api/k8s/clusters/local/v1/nodes", server.port)).Header("Org-ID", "1").Header("User-ID", "2").Do().Body(buf)
	hc.Post(fmt.Sprintf("127.0.0.1:%d/api/k8s/clusters/local/kubectl-shell", server.port)).Header("Org-ID", "1").Header("User-ID", "2").Do().Body(buf)
}

func TestAudit(t *testing.T) {
	Init()
	bundleOpts := []bundle.Option{
		bundle.WithHTTPClient(
			httpclient.New(
				httpclient.WithTimeout(time.Second, time.Second*60),
			)),
		bundle.WithPipeline(),
		bundle.WithScheduler(),
		bundle.WithMonitor(),
		bundle.WithCoreServices(),
		bundle.WithOrchestrator(),
		bundle.WithDiceHub(),
		bundle.WithClusterManager(),
	}
	bdl := bundle.New(bundleOpts...)

	server := opts
	l, err := net.Listen("tcp", fmt.Sprintf("127.0.0.1:%d", server.port))
	if err != nil {
		t.Error(err)
		return
	}
	audit := NewAuditor(bdl)
	chain := Chain{
		audit.AuditMiddleWare,
	}
	s := Server{
		router:      mux.NewRouter(),
		chains:      chain,
		httpHandler: handler(),
	}

	//s.auth = auth
	go func() {
		err := http.Serve(l, &s)
		if err != nil {
			t.Error(err)
		}
	}()

	hc := httpclient.New(httpclient.WithTimeout(time.Second, time.Second*60))
	buf := bytes.NewBuffer(make([]byte, 1024))
	hc.Post(fmt.Sprintf("127.0.0.1:%d/api/k8s/clusters/local/kubectl-shell", server.port)).Header("Org-ID", "1").Header("User-ID", "2").Do().Body(buf)
}

type MockPodInterface struct {
	v1.PodInterface
}

func (m *MockPodInterface) List(ctx context.Context, opts metav1.ListOptions) (*corev1.PodList, error) {
	pods := map[string][]corev1.Pod{
		"app=cluster-agent": {
			{
				ObjectMeta: metav1.ObjectMeta{
					Name: "appPod1",
				},
			},
			{
				ObjectMeta: metav1.ObjectMeta{
					Name: "appPod2",
				},
			},
			{
				ObjectMeta: metav1.ObjectMeta{
					Name: "appPod3",
				},
			},
		},
		"dice/component=cluster-agent": {
			{
				ObjectMeta: metav1.ObjectMeta{
					Name: "dicePod1",
				},
			},
			{
				ObjectMeta: metav1.ObjectMeta{
					Name: "dicePod2",
				},
			},
			{
				ObjectMeta: metav1.ObjectMeta{
					Name: "dicePod3",
				},
			},
		},
	}
	return &corev1.PodList{
		Items: pods[opts.LabelSelector],
	}, nil
}

func TestShellHandler_GetAgentPod(t *testing.T) {
	s := &ShellHandler{ctx: context.Background()}
	pods := s.getAgentPods(&MockPodInterface{})
	if len(pods) != 6 {
		t.Errorf("test failed, expect length of pods is %d, actual %d", 6, len(pods))
	}
}
