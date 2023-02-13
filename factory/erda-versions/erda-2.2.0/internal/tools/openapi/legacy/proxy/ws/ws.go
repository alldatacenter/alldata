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

package ws

import (
	"fmt"
	"io"
	"net"
	"net/http"
	"strings"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/openapi/legacy/api"
)

type ReverseProxy struct {
	Director func(*http.Request)
}

func NewReverseProxy(director func(*http.Request)) http.Handler {
	return &ReverseProxy{
		Director: director,
	}
}

func (p *ReverseProxy) ServeHTTP(rw http.ResponseWriter, req *http.Request) {
	p.Director(req)
	host := req.Host
	if !strings.Contains(host, ":") {
		host = host + ":80"
	}
	logrus.Infof("tcp dial: %v", host)
	conn, err := net.Dial("tcp", host)
	if err != nil {
		errStr := fmt.Sprintf("dial with host[%s] failed", host)
		logrus.Error(errStr)
		http.Error(rw, errStr, 500)
		return
	}
	defer conn.Close()
	clientConn, _, err := rw.(http.Hijacker).Hijack()
	if err != nil {
		logrus.Error("hijack failed")
		http.Error(rw, "hijack failed", 500)
		return
	}
	defer clientConn.Close()
	if err := req.Write(conn); err != nil {
		errStr := fmt.Sprintf("write request to backend conn failed: %v", err)
		logrus.Error(errStr)
		http.Error(rw, errStr, 500)
		return
	}
	done := make(chan struct{}, 1)
	copy := func(dst io.Writer, src io.Reader) {
		io.Copy(dst, src)
		done <- struct{}{}

	}
	go copy(conn, clientConn)
	go copy(clientConn, conn)
	<-done
}

type ReverseProxyWithCustom struct {
	reverseProxy http.Handler
}

func NewReverseProxyWithCustom(director func(*http.Request)) http.Handler {
	r := NewReverseProxy(director)
	return &ReverseProxyWithCustom{reverseProxy: r}
}

func (r *ReverseProxyWithCustom) ServeHTTP(rw http.ResponseWriter, req *http.Request) {
	defer func() {
		if err := recover(); err != nil {
			logrus.Errorf("[alert] openapi ws proxy recover from panic: %v", err)
		}
	}()
	spec := api.API.Find(req)
	if spec != nil && spec.Custom != nil {
		spec.Custom(rw, req)
		return
	}
	r.reverseProxy.ServeHTTP(rw, req)
}
