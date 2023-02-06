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

// Cluster 集群配置
type Cluster struct {
	Name            string   `json:"name"`
	Type            string   `json:"type"` // dcos, k8s
	Nameservers     []string `json:"nameservers"`
	ContainerSubnet string   `json:"containerSubnet"`
	VirtualSubnet   string   `json:"virtualSubnet"`
	MasterVIP       string   `json:"masterVIP,omitempty"`
	Offline         bool     `json:"offline"`
}

// SSH 远程登录配置
type SSH struct {
	Port       int    `json:"port"`
	User       string `json:"user"`
	Password   string `json:"password,omitempty"`
	Account    string `json:"account"`
	PrivateKey string `json:"privateKey,omitempty"`
	PublicKey  string `json:"publicKey,omitempty"`
}

const (
	RootUser = "root"
)

// FPS 文件代理服务器配置
type FPS struct {
	Host  string `json:"host"`
	Port  int    `json:"port"`
	Proxy bool   `json:"proxy"`
}

// Gluster GlusterFS 配置
type Gluster struct {
	Version string   `json:"version"`
	Hosts   []string `json:"hosts"`
	Server  bool     `json:"server"`
	Replica int      `json:"replica"`
	Brick   string   `json:"brick"`
}

// Storage 共享存储配置
type Storage struct {
	MountPoint     string  `json:"mountPoint"`
	NAS            string  `json:"nas"`
	Gluster        Gluster `json:"gluster"`
	GittarDataPath string  `json:"gittarDataPath"`
}

// Docker Docker 配置
type Docker struct {
	DataRoot  string `json:"dataRoot"`
	ExecRoot  string `json:"execRoot"`
	BIP       string `json:"bip"`
	FixedCIDR string `json:"fixedCIDR"`
}

// Node 节点配置
type Node struct {
	IP   string `json:"ip"`
	Type string `json:"type"` // master, lb, app
	Tag  string `json:"tag"`
}

// Nodes 节点列表
type Nodes []Node

// MySQL 平台数据库配置
type MySQL struct {
	Host      string `json:"host,omitempty"`
	Port      int    `json:"port,omitempty"`
	Username  string `json:"username,omitempty"`
	Password  string `json:"password,omitempty"`
	DiceDB    string `json:"diceDB,omitempty"`
	PandoraDB string `json:"pandoraDB,omitempty"`
	SonarDB   string `json:"sonarDB,omitempty"`
}

// OpenVPN 平台 VPN 配置
type OpenVPN struct {
	PeerSubnet string   `json:"peerSubnet,omitempty"`
	Subnets    []string `json:"subnets,omitempty"`
	ConfigOPVN string   `json:"configOPVN,omitempty"`
}

// Platform 平台配置
type Platform struct {
	Environment    string            `json:"environment,omitempty"`
	WildcardDomain string            `json:"wildcardDomain"`
	AssignDomains  map[string]string `json:"assignDomains"`
	AssignNodes    map[string]string `json:"assignNodes,omitempty"`
	MySQL          MySQL             `json:"mysql,omitempty"`
	AcceptMaster   bool              `json:"acceptMaster,omitempty"`
	AcceptLB       bool              `json:"acceptLB,omitempty"`
	DataDiskDevice string            `json:"dataDiskDevice,omitempty"`
	DataRoot       string            `json:"dataRoot,omitempty"`
	Scheme         string            `json:"scheme"`
	Port           int               `json:"port"`
	RegistryHost   string            `json:"registryHost,omitempty"`
	OpenVPN        OpenVPN           `json:"openvpn,omitempty"`
}

// Sysconf dice installer 配置
type Sysconf struct {
	Cluster      Cluster           `json:"cluster"`
	SSH          SSH               `json:"ssh"`
	FPS          FPS               `json:"fps"`
	Storage      Storage           `json:"storage"`
	Docker       Docker            `json:"docker"`
	Nodes        Nodes             `json:"nodes"`
	NewNodes     Nodes             `json:"-"` // TODO
	Platform     Platform          `json:"platform"`
	MainPlatform *Platform         `json:"mainPlatform,omitempty"`
	Envs         map[string]string `json:"envs,omitempty"`
	OrgID        int               `json:"orgID,omitempty"`
}
