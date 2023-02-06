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

package node

import (
	"fmt"

	v1 "k8s.io/api/core/v1"

	"github.com/erda-project/erda/pkg/http/httpclient"
)

type Node struct {
	addr   string
	client *httpclient.HTTPClient
}

func New(addr string, client *httpclient.HTTPClient) *Node {
	return &Node{
		addr:   addr,
		client: client,
	}
}

func (n *Node) List() (*v1.NodeList, error) {
	var body v1.NodeList
	path := "/api/v1/nodes"
	r, err := n.client.Get(n.addr, httpclient.RetryOption{}).
		Path(path).
		Do().
		JSON(&body)
	if err != nil {
		return nil, fmt.Errorf("failed to get nodelist, err: %v", err)
	}
	if !r.IsOK() {
		return nil, fmt.Errorf("failed to get nodelist, statuscode: %d, body: %s", r.StatusCode(), r.Body())
	}
	return &body, nil
}
