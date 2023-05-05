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

package sysconf

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/erda-project/erda/apistructs"
)

//go:generate bash copy_sysconf.sh

// Addr 返回服务的内部访问地址
func (c Cluster) Host(serviceName string) string {
	switch c.Type {
	case apistructs.DCOS:
		return serviceName + ".marathon.l4lb.thisdcos.directory"
	case apistructs.K8S:
		return serviceName + ".default.svc.cluster.local"
	default:
		panic("cluster type")
	}
}

// Addr 返回 FPS 完整地址
func (x FPS) Addr() string {
	return fmt.Sprintf("%s:%d", x.Host, x.Port)
}

// Master 返回第一个 master 节点
func (a Nodes) Master() Node {
	for _, n := range a {
		if n.Type == "master" {
			return n
		}
	}
	panic("no master")
}

// Filter 返回指定类型的所有节点
func (a Nodes) Filter(t string) (b Nodes) {
	for _, n := range a {
		if n.Type == t {
			b = append(b, n)
		}
	}
	return
}

func (a Nodes) Len() int {
	return len(a)
}

func (a Nodes) Less(i, j int) bool {
	if a[i].Type < a[j].Type {
		return true
	} else if a[i].Type > a[j].Type {
		return false
	}
	return a[i].IP < a[j].IP
}

func (a Nodes) Swap(i, j int) {
	a[i], a[j] = a[j], a[i]
}

// Domains 返回服务的全部域名
func (p *Platform) Domains(serviceName string) string {
	if v, ok := p.AssignDomains[serviceName]; ok {
		return v
	}
	switch serviceName {
	case "ui":
		return p.Domains("dice")
	case "cookie":
		return p.WildcardDomain
	}
	return serviceName + "." + p.WildcardDomain
}

// Domain 返回服务的第一个域名
func (p *Platform) Domain(serviceName string) string {
	return strings.Split(p.Domains(serviceName), ",")[0]
}

// PublicURL 返回服务的外部访问链接
func (p *Platform) PublicURL(serviceName string) string {
	u := p.Scheme + "://" + p.Domain(serviceName)
	switch p.Scheme {
	case "http":
		if p.Port != 80 {
			u += ":" + strconv.Itoa(p.Port)
		}
	case "https":
		if p.Port != 443 {
			u += ":" + strconv.Itoa(p.Port)
		}
	default:
		panic("platform scheme")
	}
	return u
}

func (s Storage) RemoteTarget() string {
	if i := strings.LastIndexByte(s.NAS, ':'); i != -1 {
		return s.NAS
	} else {
		return s.NAS + ":/"
	}
}
