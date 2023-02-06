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

package restclient

import (
	"os"
	"strings"

	"github.com/pkg/errors"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/client-go/kubernetes"
	. "k8s.io/client-go/rest"
)

var (
	ErrAddrMiss    = Error{"customhttp: inetaddr miss"}
	ErrInvalidAddr = Error{"customhttp: invalid inetaddr"}
)

type Error struct {
	msg string
}

func (e Error) Error() string {
	return e.msg
}

var inetAddr string

// 用于获取netportal地址的环境变量名
const (
	netportalAddrEnvVarName = "NETPORTAL_ADDR"
	portalPassthroughHeader = "X-Portal-Passthrough"
	portalDirectHeader      = "X-Portal-Direct"
	portalSSLHeader         = "X-Portal-SSL"
	portalSSEHeader         = "X-Portal-SSE"
	portalWSHeader          = "X-Portal-Websocket"
	portalHostHeader        = "X-Portal-Host"
	portalDestHeader        = "X-Portal-Dest"
)

type InetRESTClient struct {
	*RESTClient
	inetHeaders map[string]string
}

func NewClientSet(addr string, config *Config) (*kubernetes.Clientset, error) {
	inetClient, err := NewInetRESTClient(addr, config)
	if err != nil {
		return nil, err
	}
	return kubernetes.New(inetClient), nil
}

// 用于覆盖根据环境变量取的值
func SetInetAddr(addr string) {
	inetAddr = addr
}

func init() {
	inetAddr = os.Getenv(netportalAddrEnvVarName)
}

func parseInetUrl(url string) (portalHost string, portalDest string, portalUrl string, portalArgs map[string]string, err error) {
	url = strings.TrimPrefix(url, "inet://")
	url = strings.Replace(url, "//", "/", -1)
	portalArgs = map[string]string{}
	parts := strings.SplitN(url, "/", 3)
	if len(parts) < 2 {
		err = errors.Wrapf(ErrInvalidAddr, "addr:%s", url)
		return
	}
	portalHost = parts[0]
	portalDest = parts[1]
	portalUrl = ""
	if len(parts) > 2 {
		portalUrl = parts[2]
	}
	portalArgsPos := strings.Index(portalHost, "?")
	if portalArgsPos == -1 {
		return
	}
	argStr := portalHost[portalArgsPos+1:]
	portalHost = portalHost[:portalArgsPos]
	argPairs := strings.Split(argStr, "&")
	for _, pair := range argPairs {
		argParts := strings.Split(pair, "=")
		if len(argParts) == 2 {
			portalArgs[argParts[0]] = argParts[1]
		}
	}
	return
}

func NewInetRESTClient(addr string, config *Config) (*InetRESTClient, error) {
	var restClient *RESTClient
	var err error
	if !strings.HasPrefix(addr, "inet://") {
		config.Host = addr
		if config.GroupVersion == nil {
			restClient, err = UnversionedRESTClientFor(config)
		} else {
			restClient, err = RESTClientFor(config)

		}
		if err != nil {
			return nil, errors.WithStack(err)
		}
		return &InetRESTClient{
			RESTClient: restClient,
		}, nil
	}
	if inetAddr == "" {
		return nil, errors.WithStack(ErrAddrMiss)
	}
	inetAddr = strings.TrimPrefix(inetAddr, "http://")
	portalHost, portalDest, _, portalArgs, err := parseInetUrl(addr)
	if err != nil {
		return nil, err
	}
	config.Host = inetAddr
	if config.GroupVersion == nil {
		restClient, err = UnversionedRESTClientFor(config)
	} else {
		restClient, err = RESTClientFor(config)

	}
	if err != nil {
		return nil, errors.WithStack(err)
	}
	inetClient := &InetRESTClient{
		RESTClient:  restClient,
		inetHeaders: map[string]string{},
	}
	inetClient.inetHeaders[portalHostHeader] = portalHost
	inetClient.inetHeaders[portalDestHeader] = portalDest
	if portalArgs["direct"] == "on" {
		inetClient.inetHeaders[portalDirectHeader] = "on"
	}
	if portalArgs["ssl"] == "on" {
		inetClient.inetHeaders[portalSSLHeader] = "on"
	}
	if portalArgs["passthrough"] == "on" {
		inetClient.inetHeaders[portalPassthroughHeader] = "on"
	}
	return inetClient, nil
}

func (c *InetRESTClient) Get() *Request {
	return c.Verb("GET")
}

func (c *InetRESTClient) Post() *Request {
	return c.Verb("POST")
}
func (c *InetRESTClient) Put() *Request {
	return c.Verb("PUT")
}

func (c *InetRESTClient) Patch(pt types.PatchType) *Request {
	return c.Verb("PATCH").SetHeader("Content-Type", string(pt))
}

func (c *InetRESTClient) Delete() *Request {
	return c.Verb("DELETE")
}

func (c *InetRESTClient) Verb(verb string) *Request {
	req := c.RESTClient.Verb(verb)
	for key, value := range c.inetHeaders {
		req.SetHeader(key, value)
	}
	return req
}
