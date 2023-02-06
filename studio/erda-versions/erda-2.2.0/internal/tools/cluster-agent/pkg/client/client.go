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

package client

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/url"
	"os"
	"strings"
	"sync"
	"time"

	"github.com/pkg/errors"
	"github.com/rancher/remotedialer"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/cluster-agent/config"
	"github.com/erda-project/erda/pkg/discover"
)

const (
	tokenFile  = "/var/run/secrets/kubernetes.io/serviceaccount/token"
	rootCAFile = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt"
)

type Option func(*Client)

type Client struct {
	sync.Mutex
	cfg        *config.Config
	accessKey  string
	connected  bool
	disconnect chan struct{}
}

func New(ops ...Option) *Client {
	c := Client{
		disconnect: make(chan struct{}),
	}
	for _, op := range ops {
		op(&c)
	}
	return &c
}

func WithConfig(cfg *config.Config) Option {
	return func(c *Client) {
		c.cfg = cfg
	}
}

func (c *Client) DisConnect() {
	if !c.IsConnected() {
		return
	}
	c.disconnect <- struct{}{}
}

func (c *Client) Start(ctx context.Context) error {
	headers := http.Header{
		"X-Erda-Cluster-Key": {c.cfg.ClusterKey},
	}

	if c.cfg.CollectClusterInfo {
		clusterInfo, err := getClusterInfo(c.cfg.K8SApiServerAddr)
		if err != nil {
			return err
		}
		bytes, err := json.Marshal(clusterInfo)
		if err != nil {
			return err
		}
		headers["X-Erda-Cluster-Info"] = []string{base64.StdEncoding.EncodeToString(bytes)}
	}

	ep, err := parseDialerEndpoint(c.cfg.ClusterManagerEndpoint)
	if err != nil {
		logrus.Errorf("failed to parse dial endpoint: %v", err)
		return err
	}

	// If specified cluster access key, preferred to use it.
	if c.cfg.ClusterAccessKey == "" {
		go func() {
			if err := c.watchClusterCredential(ctx); err != nil {
				logrus.Errorf("watch cluster info error: %v", err)
				return
			}
		}()
	} else {
		// Set access key values default
		c.setAccessKey(c.cfg.ClusterAccessKey)
		logrus.Infof("use specified cluster access key: %v", c.cfg.ClusterAccessKey)
	}

	for {
		if c.getAccessKey() == "" {
			continue
		}

		headers.Set("Authorization", c.getAccessKey())
		_ = remotedialer.ClientConnect(ctx, ep, headers, nil, func(proto, address string) bool {
			switch proto {
			case "tcp":
				return true
			case "unix":
				return address == "/var/run/docker.sock"
			case "npipe":
				return address == "//./pipe/docker_engine"
			}
			return false
		}, c.onConnect)

		select {
		case <-ctx.Done():
			return nil
		case <-time.After(time.Duration(c.cfg.ConRetryInterval) * time.Second):
		}
	}
}

// onConnect
func (c *Client) onConnect(ctx context.Context, _ *remotedialer.Session) error {
	defer func() {
		c.setConnected(false)
	}()

	c.setConnected(true)

	// Or passThrough cancel() function
	select {
	case <-c.disconnect:
		return fmt.Errorf("cluster credential reload")
	case <-ctx.Done():
		return nil
	}
}

func (c *Client) setConnected(b bool) {
	c.Lock()
	defer c.Unlock()
	c.connected = b
}

func (c *Client) IsConnected() bool {
	// data race
	c.Lock()
	defer c.Unlock()
	return c.connected
}

func getClusterInfo(apiServerAddr string) (map[string]interface{}, error) {
	caData, err := ioutil.ReadFile(rootCAFile)
	if err != nil {
		return nil, errors.Wrapf(err, "reading %s", rootCAFile)
	}

	token, err := ioutil.ReadFile(tokenFile)
	if err != nil {
		return nil, errors.Wrapf(err, "reading %s", tokenFile)
	}
	return map[string]interface{}{
		"address": apiServerAddr,
		"token":   strings.TrimSpace(string(token)),
		"caCert":  base64.StdEncoding.EncodeToString(caData),
	}, nil
}

func parseDialerEndpoint(endpoint string) (string, error) {
	u, err := url.Parse(endpoint)
	if err != nil {
		return "", err
	}

	//inCluster, visit dialer inner service first.
	if os.Getenv(string(apistructs.DICE_IS_EDGE)) == "false" && discover.ClusterDialer() != "" {
		return "ws://" + discover.ClusterDialer() + u.Path, nil
	}

	switch u.Scheme {
	case "https":
		u.Scheme = "wss"
	case "http":
		u.Scheme = "ws"
	}

	return u.String(), nil
}
