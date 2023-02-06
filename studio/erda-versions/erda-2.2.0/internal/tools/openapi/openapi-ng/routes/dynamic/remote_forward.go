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

package dynamic

import (
	"net"
	"strings"

	"github.com/erda-project/erda-infra/base/servicehub"
	forward "github.com/erda-project/erda-infra/providers/remote-forward"
)

func (p *provider) initRemoteForward(ctx servicehub.Context) (err error) {
	host := p.getHost()
	p.ForwardServer.AddHandshaker(func(req *forward.RequestHeader, resp *forward.ResponseHeader) error {
		resp.Values["host"] = host
		return nil
	})
	return nil
}

func (p *provider) getHost() string {
	if len(p.Cfg.Host) > 0 {
		return p.Cfg.Host
	}
	addr, err := getMainNetInterfaceAddr(p.Cfg.NetInterfaceName)
	if err == nil && len(addr) > 0 {
		return addr
	}
	return "127.0.0.1"
}

func getMainNetInterfaceAddr(iname string) (string, error) {
	interfaces, err := net.Interfaces()
	if err != nil {
		return "", err
	}
	nameMatcher := func(name string) bool {
		return strings.HasPrefix(name, "e") && strings.HasSuffix(name, "0")
	}
	if len(iname) > 0 {
		nameMatcher = func(name string) bool { return name == iname }
	}
	for _, ifi := range interfaces {
		if ifi.Flags&net.FlagUp == 0 || ifi.Flags&net.FlagLoopback != 0 {
			continue
		}
		if !nameMatcher(ifi.Name) {
			continue
		}
		inter, err := net.InterfaceByName(ifi.Name)
		if err != nil {
			continue
		}
		addrs, err := inter.Addrs()
		if err != nil {
			continue
		}
		for _, addr := range addrs {
			if ip, ok := addr.(*net.IPNet); ok {
				if ipv4 := ip.IP.To4(); ipv4 != nil {
					return ipv4.String(), nil
				}
			}
		}
	}
	return "", nil
}
