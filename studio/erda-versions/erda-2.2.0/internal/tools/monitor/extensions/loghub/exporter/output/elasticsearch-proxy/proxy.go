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

// Author: recallsong
// Email: ruiguo.srg@alibaba-inc.com

package elasticsearchr

import (
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"
	"sync/atomic"

	log "github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
)

type config struct {
	Addr    string `file:"addr"`
	Targets string `file:"targets"`
}

type provider struct {
	C       *config
	L       logs.Logger
	server  *http.Server
	targets []string
	index   int32
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.targets = strings.Split(p.C.Targets, ",")
	p.server = &http.Server{Addr: p.C.Addr, Handler: http.HandlerFunc(p.Handler)}
	return nil
}

func (p *provider) Start() error {
	p.L.Infof("start http proxy server at %s", p.C.Addr)
	return p.server.ListenAndServe()
}

func (p *provider) Close() error {
	return p.server.Close()
}

func (p *provider) getTargetURL() string {
	idx := atomic.AddInt32(&p.index, 1)
	return p.targets[int(idx)%len(p.targets)]
}

func (p *provider) Handler(w http.ResponseWriter, req *http.Request) {
	// target url
	path, err := url.PathUnescape(req.URL.Path)
	if err == nil {
		if strings.HasPrefix(path, "/<") && strings.HasSuffix(path, ">") {
			path = "/" + url.QueryEscape(path[1:])
		}
	} else {
		path = req.URL.Path
	}
	urlstr := p.getTargetURL() + path + "?" + req.URL.RawQuery
	u, err := url.Parse(urlstr)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		log.Errorf("invalid proxy url: %s", urlstr)
		return
	}
	request := &http.Request{
		Method: req.Method,
		URL:    u,
		Header: req.Header,
		Body:   req.Body,
	}
	resp, err := http.DefaultClient.Do(request)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte(fmt.Sprintf("%s", err)))
		log.Errorf("fail to proxy url: %s", err)
		return
	}
	for key, vals := range resp.Header {
		for _, val := range vals {
			w.Header().Add(key, val)
		}
	}
	w.WriteHeader(resp.StatusCode)
	io.Copy(w, resp.Body)
}

func init() {
	servicehub.Register("logs-elasticsearch-proxy", &servicehub.Spec{
		Services:    []string{"logs-elasticsearch-proxy"},
		Description: "elasticsearch proxy",
		ConfigFunc:  func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
