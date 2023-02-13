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
	"path/filepath"
	"sort"
	"strings"

	"github.com/erda-project/erda/apistructs"
)

const (
	NodeTypeMaster  = "master"
	NodeTypeLB      = "lb"
	NodeTypeApp     = "app"
	NodeTypePublic  = "public"
	NodeTypePrivate = "private"
)

func (sc Sysconf) HostSubnets() []string {
	m := make(map[string]struct{})
	for _, n := range sc.Nodes {
		a := strings.Split(n.IP, ".")
		if len(a) != 4 { //
			panic(n.IP)
		}
		a[3] = "0"
		m[strings.Join(a, ".")+"/24"] = struct{}{}
	}
	if len(m) > 1 {
		for _, n := range sc.Nodes {
			a := strings.Split(n.IP, ".")
			a[2] = "0"
			a[3] = "0"
			m[strings.Join(a, ".")+"/16"] = struct{}{}
		}
	}
	if len(m) > 1 {
		for _, n := range sc.Nodes {
			a := strings.Split(n.IP, ".")
			a[1] = "0"
			a[2] = "0"
			a[3] = "0"
			m[strings.Join(a, ".")+"/8"] = struct{}{}
		}
	}
	a := make([]string, 0, len(m))
	for k := range m {
		a = append(a, k)
	}
	return a
}

func (sc *Sysconf) SetDefault() {
	if sc.Cluster.Type == "" {
		sc.Cluster.Type = apistructs.K8S
	}
	if sc.Cluster.ContainerSubnet == "" {
		sc.Cluster.ContainerSubnet = "9.0.0.0/8"
	}
	if sc.Cluster.VirtualSubnet == "" {
		if sc.Cluster.Type == apistructs.K8S {
			sc.Cluster.VirtualSubnet = "10.96.0.0/12"
		} else {
			sc.Cluster.VirtualSubnet = "11.0.0.0/8"
		}
	}

	if sc.SSH.Port == 0 {
		sc.SSH.Port = 22
	}
	if sc.SSH.User == "" {
		sc.SSH.User = "root"
	}

	if sc.FPS.Port == 0 {
		sc.FPS.Port = 17621
	}

	if sc.Storage.MountPoint == "" {
		sc.Storage.MountPoint = "/netdata"
	}
	if sc.Storage.Gluster.Version == "" {
		sc.Storage.Gluster.Version = "6"
	}
	if sc.Storage.Gluster.Brick == "" {
		sc.Storage.Gluster.Brick = "/brick"
	}

	if sc.Docker.DataRoot == "" {
		sc.Docker.DataRoot = "/var/lib/docker"
	}
	if sc.Docker.ExecRoot == "" {
		sc.Docker.ExecRoot = "/var/run/docker"
	}
	if sc.Docker.BIP == "" {
		sc.Docker.BIP = "172.17.0.1/16"
	}
	if sc.Docker.FixedCIDR == "" {
		sc.Docker.FixedCIDR = "172.17.0.0/16"
	}

	for _, n := range sc.Nodes {
		if n.Type == NodeTypePublic {
			n.Type = NodeTypeLB
		}
		if n.Type == NodeTypePrivate {
			n.Type = NodeTypeApp
		}
		n.Tag = CleanDiceTag(n.Tag, sc.Cluster.Type)
		for _, t := range strings.Split(n.Tag, ",") {
			switch t {
			case NodeTypeMaster, NodeTypeLB, NodeTypeApp:
				if n.Type == "" {
					n.Type = t
				}
			}
		}
		if n.Type == "" {
			n.Type = NodeTypeApp
		}
	}

	if sc.Platform.OpenVPN.PeerSubnet == "" {
		sc.Platform.OpenVPN.PeerSubnet = "10.99.99.0/24"
	}
	if len(sc.Platform.OpenVPN.Subnets) == 0 {
		sc.Platform.OpenVPN.Subnets = []string{
			sc.Cluster.ContainerSubnet,
			sc.Cluster.VirtualSubnet,
		}
		sc.Platform.OpenVPN.Subnets = append(sc.Platform.OpenVPN.Subnets, sc.HostSubnets()...)
		if sc.Cluster.Type == apistructs.DCOS {
			sc.Platform.OpenVPN.Subnets = append(sc.Platform.OpenVPN.Subnets, "198.51.100.0/24")
		}
		sort.Strings(sc.Platform.OpenVPN.Subnets)
	}

	if sc.Platform.RegistryHost == "" {
		sc.Platform.RegistryHost = sc.Cluster.Host("registry") + ":5000"
	}

	if sc.Platform.Scheme == "" {
		sc.Platform.Scheme = "http"
	}
	if sc.Platform.Port == 0 {
		if sc.Platform.Scheme == "https" {
			sc.Platform.Port = 443
		} else {
			sc.Platform.Port = 80
		}
	}

	if sc.Platform.DataRoot == "" {
		sc.Platform.DataRoot = "/data"
	}

	if sc.Storage.GittarDataPath == "" {
		sc.Storage.GittarDataPath = filepath.Join(sc.Storage.MountPoint, "dice", "gittar")
	}
}
