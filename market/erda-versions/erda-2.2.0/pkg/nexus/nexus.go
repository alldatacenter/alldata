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

package nexus

import (
	"github.com/erda-project/erda/pkg/http/httpclient"
)

type Nexus struct {
	Server

	hc             *httpclient.HTTPClient
	blobNetdataDir string
}

type Server struct {
	Addr     string
	Username string
	Password string
}

type Option func(*Nexus)

func New(server Server, ops ...Option) *Nexus {
	n := Nexus{
		Server: server,
	}
	for _, op := range ops {
		op(&n)
	}
	if n.hc == nil {
		n.hc = httpclient.New(
		// 3 seconds is too short, temporarily change to the default 60 seconds
		// Decouple the connection between publisher and nexsus in the future
		// httpclient.WithTimeout(time.Second, time.Second*3),
		)
	}
	if n.blobNetdataDir == "" {
		n.blobNetdataDir = DefaultBlobNetdataDir
	}
	return &n
}

func WithHttpClient(hc *httpclient.HTTPClient) Option {
	return func(n *Nexus) {
		n.hc = hc
	}
}

func WithBlobNetdataDir(dir string) Option {
	return func(n *Nexus) {
		n.blobNetdataDir = dir
	}
}
