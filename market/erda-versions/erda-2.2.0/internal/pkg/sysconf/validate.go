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
	"net"
	"path/filepath"
	"regexp"
	"sort"
	"strconv"
	"strings"

	"github.com/erda-project/erda/apistructs"
)

func isLowerNumber(s string, maxLength int) bool {
	n := len(s)
	if n < 1 || n > maxLength {
		return false
	}
	for i := 0; i < n; i++ {
		if !(s[i] >= '0' && s[i] <= '9') && !(s[i] >= 'a' && s[i] <= 'z') {
			return false
		}
	}
	return true
}

func isClusterName(s string) bool {
	i := strings.IndexByte(s, '-')
	if i == -1 {
		return false
	}
	return isLowerNumber(s[:i], 20) && isLowerNumber(s[i+1:], 20)
}

func between(i, min, max int) bool {
	return i >= min && i <= max
}

func IsPort(i int) bool {
	return between(i, 1, 65535)
}

func isAbs(s string) bool {
	return filepath.Clean(s) == s && filepath.IsAbs(s) && s != "/"
}

func isIP(s string) bool {
	return net.ParseIP(s).To4() != nil
}

var dnsName = regexp.MustCompile(`^([a-zA-Z0-9_][a-zA-Z0-9_-]{0,62})(\.[a-zA-Z0-9_][a-zA-Z0-9_-]{0,62})*[._]?$`)

func IsDNSName(s string) bool {
	return between(len(s), 1, 255) && dnsName.MatchString(s)
}

func isNet(s string) bool {
	i := strings.IndexByte(s, '/')
	if i == -1 || !isIP(s[:i]) {
		return false
	}
	n, err := strconv.Atoi(s[i+1:])
	if err != nil || !between(n, 1, 32) {
		return false
	}
	return true
}

// Validate  检查配置
func (sc Sysconf) Validate() string {
	if !isClusterName(sc.Cluster.Name) {
		return "cluster name"
	}
	if !isNet(sc.Cluster.ContainerSubnet) {
		return "cluster container subnet"
	}
	if !isNet(sc.Cluster.VirtualSubnet) {
		return "cluster virtual subnet"
	}
	switch sc.Cluster.Type {
	case apistructs.DCOS, apistructs.K8S:
	default:
		return "cluster type"
	}
	if !IsPort(sc.SSH.Port) {
		return "ssh port"
	}
	if sc.SSH.User == "" {
		return "ssh user"
	}
	if sc.SSH.Account != "" {
		if sc.SSH.Account == RootUser {
			return "ssh root not allow"
		}
	} else {
		if sc.SSH.User == RootUser {
			return "ssh root not allow"
		}
	}
	if !IsPort(sc.FPS.Port) {
		return "fps port"
	}
	if !isIP(sc.FPS.Host) {
		return "fps host"
	}
	if !isAbs(sc.Storage.MountPoint) {
		return "storage mount point"
	}
	if sc.Storage.NAS != "" && len(sc.Storage.Gluster.Hosts) > 0 {
		return "storage either nas or gluster"
	} else if sc.Storage.NAS != "" {
		s := sc.Storage.NAS
		if i := strings.LastIndexByte(sc.Storage.NAS, ':'); i != -1 {
			s = sc.Storage.NAS[:i]
			if !isAbs(sc.Storage.NAS[i+1:]) {
				return "storage nas"
			}
		}
		if !IsDNSName(s) {
			return "storage nas"
		}
	} else if len(sc.Storage.Gluster.Hosts) > 0 {
		switch sc.Storage.Gluster.Version {
		case "3.12", "4.0", "4.1", "5", "6":
		default:
			return "storage gluster version"
		}
		for i, host := range sc.Storage.Gluster.Hosts {
			if !isIP(host) {
				return "storage gluster host: " + host
			}
			for j := i - 1; j >= 0; j-- {
				if sc.Storage.Gluster.Hosts[j] == host {
					return "storage gluster host: " + host
				}
			}
		}
		if sc.Storage.Gluster.Server {
			if sc.Storage.Gluster.Replica < 1 || len(sc.Storage.Gluster.Hosts)%sc.Storage.Gluster.Replica != 0 {
				return "storage gluster replica"
			}
			if !isAbs(sc.Storage.Gluster.Brick) {
				return "storage gluster brick"
			}
			for _, host := range sc.Storage.Gluster.Hosts {
				ok := false
				for _, n := range sc.Nodes {
					if host == n.IP {
						ok = true
						break
					}
				}
				if !ok {
					return "storage gluster host not in nodes: " + host
				}
			}
		}
	}
	var master, lb, app int
	for _, n := range sc.Nodes {
		if !isIP(n.IP) {
			return "node ip: " + n.IP
		}
		switch n.Type {
		case "master":
			master++
		case "lb":
			lb++
		case "app":
			app++
		default:
			return "node " + n.IP + " type: " + n.Type
		}
		if !IsDiceTags(n.Tag) {
			return "node " + n.IP + " tag: " + n.Tag
		}
	}
	if master == 0 {
		return "no master nodes"
	}
	if lb == 0 {
		return "no lb nodes"
	}
	if app == 0 {
		return "no app nodes"
	}
	if !isAbs(sc.Docker.DataRoot) {
		return "docker data root"
	}
	if !isAbs(sc.Docker.ExecRoot) {
		return "docker exec root"
	}
	if sc.Docker.DataRoot == sc.Docker.ExecRoot {
		return "docker data root same as exec root"
	}
	if !isNet(sc.Docker.BIP) {
		return "docker bip"
	}
	if !isNet(sc.Docker.FixedCIDR) {
		return "docker fixed cidr"
	}
	if sc.Docker.BIP == sc.Docker.FixedCIDR {
		return "docker bip same as fixed cidr"
	}
	if !isAbs(sc.Platform.DataRoot) {
		return "platform data root"
	}
	if sc.Platform.Scheme != "http" && sc.Platform.Scheme != "https" {
		return "platform scheme"
	}
	if !IsPort(sc.Platform.Port) {
		return "platform port"
	}
	if !IsDNSName(sc.Platform.WildcardDomain) {
		return "platform wildcard domain"
	}
	if sc.Platform.RegistryHost == "" {
		return "platform registry host"
	}
	return ""
}

var diceTagRegexp = regexp.MustCompile(`^[A-Za-z0-9\-_]{1,30}$`)

func IsDiceTags(s string) bool {
	if s == "" {
		return true
	}
	if a := strings.Split(s, ","); len(a) > 100 {
		return false
	} else {
		for i, s := range a {
			if !diceTagRegexp.MatchString(s) {
				return false
			}
			for i--; i >= 0; i-- {
				if s == a[i] {
					return false
				}
			}
		}
	}
	return true
}

func CleanDiceTag(s, t string) string {
	if s != "" {
		a := strings.Split(s, ",")
		if t == "dcos" {
			for k, v := range a {
				switch v {
				case "pack-job":
					a[k] = "pack"
				case "bigdata-job":
					a[k] = "bigdata"
				case "stateful-service":
					a[k] = "service-stateful"
				case "stateless-service":
					a[k] = "service-stateless"
				}
			}
		}
		m := make(map[string]struct{}, len(a))
		for _, t := range a {
			m[t] = struct{}{}
		}
		a = a[:0]
		for k := range m {
			a = append(a, k)
		}
		sort.Strings(a)
		s = strings.Join(a, ",")
	}
	return s
}
