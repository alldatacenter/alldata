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

// Package conf 定义了 orchestrator 所需要的配置选项，这些配置选项都是通过环境变量加载.
package conf

import (
	"sync"
	"time"

	"github.com/erda-project/erda/pkg/envconf"
)

const (
	CLUSTERS_CONFIG_PATH = "/dice/scheduler/configs/cluster/"
)

// Conf 定义配置对象.
type Conf struct {
	Debug                      bool   `env:"DEBUG" default:"false"`
	ListenAddr                 string `env:"LISTEN_ADDR" default:"0.0.0.0:8081"`
	PoolSize                   int    `env:"POOL_SIZE" default:"100"`
	RedisMasterName            string `env:"REDIS_MASTER_NAME" default:"my-master"`
	RedisSentinels             string `env:"REDIS_SENTINELS_ADDR" default:""`
	RedisAddr                  string `env:"REDIS_ADDR" default:"127.0.0.1:6379"`
	RedisPassword              string `env:"REDIS_PASSWORD" default:""`
	InstancesPerService        int    `env:"INSTANCES_PER_SERVICE" default:"1000"`
	MainClusterName            string `env:"DICE_CLUSTER_NAME" default:""`
	TenantGroupKey             string `env:"TENANT_GROUP_KEY" default:""`
	SoldierAddr                string `env:"SOLDIER_ADDR" default:""`
	SchedulerAddr              string `env:"SCHEDULER_ADDR" default:""`
	DeployZookeeper            string `env:"DEPLOY_ZOOKEEPER" default:""`
	RuntimeUpMaxWaitTime       int64  `env:"MAX_WAIT_TIME" default:"15"`
	PublicKey                  string `env:"ENV_PUBLIC_KEY" default:"LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUlHZk1BMEdDU3FHU0liM0RRRUJBUVVBQTRHTkFEQ0JpUUtCZ1FEZUlYV1dBN09xMDdIRWlLb2NKWFp1U0dJbAoyVmZHRjZSQktxd0g0ZHVjTjBTQzk1NUNkU3hzNzlBOHFxVXNhMUZLaG5UZzV1Y1RjTkNXSjdSQW1OL1VEdmtNCitYSzFiWXJnRnBZUFk3QmV6OWJmYU9KTWVWdEdnNDdjL25xNm44Z1dlQkFhRE1VK0JPcTRTSjNCVnpsaVp6RmsKTmppRDFrQUM2QSs1Ym1NUkxRSURBUUFCCi0tLS0tRU5EIFBVQkxJQyBLRVktLS0tLQo="`
	PrivateKey                 string `env:"ENV_PRIVATE_KEY" default:"LS0tLS1CRUdJTiBSU0EgUFJJVkFURSBLRVktLS0tLQpNSUlDWEFJQkFBS0JnUURlSVhXV0E3T3EwN0hFaUtvY0pYWnVTR0lsMlZmR0Y2UkJLcXdINGR1Y04wU0M5NTVDCmRTeHM3OUE4cXFVc2ExRktoblRnNXVjVGNOQ1dKN1JBbU4vVUR2a00rWEsxYllyZ0ZwWVBZN0JlejliZmFPSk0KZVZ0R2c0N2MvbnE2bjhnV2VCQWFETVUrQk9xNFNKM0JWemxpWnpGa05qaUQxa0FDNkErNWJtTVJMUUlEQVFBQgpBb0dBWlB2Tkd5Ly9wQyt0WjIzQitCM0g0NGNncDVoUllRc3FiejNaQzVSUVpJcHpxUjZ0WWdVbTl6ZG04YzJhClhjRkVLWjlLejF2cHZWclNXUkVmenlZd3lyeEUzTGl5YmFHZldwNmNZRnlmWUpWZHU5UU05aDlsUlF3TDRHNWMKSFdWSTIzUDFvMWdxMnJCS2lEZ08xZnF1Nkp5b2s2WE5EVmM3dXVxTjgxeEkxcmtDUVFEMURCQ29tTjROR215TQpjNWhvZktKVktQbnFYU09zeTgwcld5amJjRVRGKzgxdThWdmlUNGlVZkVJWUFjVWZRek1heVJHNFNmOWwxKzQ4Ck9waExTTTREQWtFQTZBOHNzY2VmYkVuSW8yd0J2U2FITHVSbFhmLytiNC92SnJ5TzZiRGc5OHpjL1JUWnR1enkKSUNiRFN0dkIySzZtcXJsVUFEeXVPdERKSC81U1l1aFZEd0pBY09tTVM0T1UzY2pOTjdLVUNhRlVVNVU4QXdmRAp4bjFxSG80MHQxaDErQnhjdnNBc0xJMmxTM1l1SmsyNmZQdEQ4eFd2T3BHdVEwbEtGeXFRdmkvZjdRSkFhSW1sCmRiVGFvWHFma3RidDlacXNuVGd3WGVjYlpJQnZtSUNxMUtWa3d0eWIxTHFXMVN2cWF3ZHJSSWE0elhib0I5S1MKLzhSV0xKS3ZkK1Vta2YzZGl3SkJBTUdWa09UMENXRGV5b0xvcmlBRkYyZklob21FK1pZWXFiQzNnd0wraUlLRApaeXJuOGo2OVpVOGNNam54Mlp2cndpemNJaEhZTlNVa0ZZL2o1bXlzUzQ0PQotLS0tLUVORCBSU0EgUFJJVkFURSBLRVktLS0tLQo="`
	InitContainerImage         string `env:"INIT_CONTAINER_IMAGE" default:"registry.cn-hangzhou.aliyuncs.com/dice-third-party/curl:stable"`
	TokenClientID              string `env:"TOKEN_CLIENT_ID" default:"orchestrator"`
	TokenClientSecret          string `env:"TOKEN_CLIENT_SECRET" default:"devops/orchestrator"`
	InspectServiceGroupTimeout int    `env:"INSPECT_SERVICEGROUP_TIMEOUT" default:"60"`

	// Conf for scheduler
	DefaultRuntimeExecutor string `env:"DEFAULT_RUNTIME_EXECUTOR" default:"MARATHON"`
	// TraceLogEnv shows the key of environment variable defined for tracing log
	TraceLogEnv           string `env:"TRACELOGENV" default:"TERMINUS_DEFINE_TAG"`
	WsDiceRootDomain      string `env:"WS_DICE_ROOT_DOMAIN" default:"app.terminus.io,erda.cloud"`
	TerminalSecurity      bool   `env:"TERMINAL_SECURITY" default:"false"`
	ExecutorClientTimeout int    `env:"EXECUTOR_CLIENT_TIMEOUT" default:"10"`
}

var cfg Conf

// Load 从环境变量加载配置选项.
func Load() {
	envconf.MustLoad(&cfg)
}

// Debug 返回 Debug 选项.
func Debug() bool {
	return cfg.Debug
}

// ListenAddr 返回 ListenAddr 选项.
func ListenAddr() string {
	return cfg.ListenAddr
}

// PoolSize 返回 PoolSize 选项.
func PoolSize() int {
	return cfg.PoolSize
}

// RedisMasterName 返回 RedisMasterName 选项.
func RedisMasterName() string {
	return cfg.RedisMasterName
}

// RedisSentinels 返回 RedisSentinels 选项.
func RedisSentinels() string {
	return cfg.RedisSentinels
}

// RedisAddr 返回 RedisAddr 选项.
func RedisAddr() string {
	return cfg.RedisAddr
}

// RedisPassword 返回 RedisPassword 选项.
func RedisPassword() string {
	return cfg.RedisPassword
}

// InstancesPerService 返回 InstancesPerService 选项.
func InstancesPerService() int {
	return cfg.InstancesPerService
}

// MainClusterName 返回 MainClusterName 选项.
func MainClusterName() string {
	return cfg.MainClusterName
}

// TenantGroupKey 返回 TenantGroupKey 选项.
func TenantGroupKey() string {
	return cfg.TenantGroupKey
}

// SoldierAddr 返回 SoldierAddr 选项.
func SoldierAddr() string {
	return cfg.SoldierAddr
}

// SchedulerAddr 返回 SchedulerAddr 选项.
func SchedulerAddr() string {
	return cfg.SchedulerAddr
}

// PublicKey 返回 PublicKey 选项.
func PublicKey() string {
	return cfg.PublicKey
}

// PrivateKey 返回 PrivateKey 选项.
func PrivateKey() string {
	return cfg.PrivateKey
}

// DeployZookeeper 返回 DeployZookeeper 选项.
func DeployZookeeper() string {
	return cfg.DeployZookeeper
}

// RuntimeUpMaxWaitTime 发布超时时间
func RuntimeUpMaxWaitTime() int64 {
	return cfg.RuntimeUpMaxWaitTime
}

// InitContainerImage 返回 InitContainerImage 选项.
func InitContainerImage() string {
	return cfg.InitContainerImage
}

// TokenClientID 返回 TokenClientID 选项.
func TokenClientID() string {
	return cfg.TokenClientID
}

// TokenClientSecret 返回 TokenClientSecret 选项.
func TokenClientSecret() string {
	return cfg.TokenClientSecret
}

// InspectServiceGroupTimeout time out of servicegroup
func InspectServiceGroupTimeout() int {
	return cfg.InspectServiceGroupTimeout
}

var confStore ConfStore

func GetConfStore() *ConfStore {
	return &confStore
}

type ConfStore struct {
	ExecutorStore sync.Map
}

func WsDiceRootDomain() string {
	return cfg.WsDiceRootDomain
}

// DefaultRuntimeExecutor return cfg.DefaultRuntimeExecutor
func DefaultRuntimeExecutor() string {
	return cfg.DefaultRuntimeExecutor
}

// TraceLogEnv return cfg.TraceLogEnv
func TraceLogEnv() string {
	return cfg.TraceLogEnv
}

// TerminalSecurity return cfg.TerminalSecurity
func TerminalSecurity() bool {
	return cfg.TerminalSecurity
}

func ExecutorClientTimeout() time.Duration {
	return time.Duration(cfg.ExecutorClientTimeout) * time.Second
}
