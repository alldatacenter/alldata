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

// Package conf 配置文件信息
package conf

import (
	"github.com/erda-project/erda/pkg/envconf"
)

var cfg Conf

// Conf Adaptor 环境变量配置项
type Conf struct {
	ListenAddr            string `env:"LISTEN_ADDR" default:":12580"`
	Debug                 bool   `env:"DEBUG" default:"false"`
	SelfAddr              string `env:"SELF_ADDR"`
	UCAddr                string `env:"UC_ADDR"`
	UCClientID            string `env:"UC_CLIENT_ID"`
	UCClientSecret        string `env:"UC_CLIENT_SECRET"`
	UCAuditorCron         string `env:"UC_AUDITOR_CRON" default:"0 */1 * * * ?"`        // UC审计拉取周期
	UCAuditorPullSize     uint64 `env:"UC_AUDITOR_PULL_SIZE" default:"30"`              // UC审计拉取大小
	CompensationExecCron  string `env:"COMPENSATION_EXEC_CRON" default:"0 */5 * * * ?"` // UC审计发送失败，补偿周期
	UCSyncRecordCleanCron string `env:"UC_SYNCRECORD_CLEAN_CRON" default:"0 0 3 * * ?"` // UC同步记录删除周期
}

// Load 加载环境变量配置.
func Load() {
	envconf.MustLoad(&cfg)
}

// ListenAddr 返回 ListenAddr 选项.
func ListenAddr() string {
	return cfg.ListenAddr
}

// Debug 是否处于调试模式
func Debug() bool {
	return cfg.Debug
}

// SelfAddr 返回 SELF_ADDR
func SelfAddr() string {
	return cfg.SelfAddr
}

// UCAddr 返回 UCAddr 选项.
func UCAddr() string {
	return cfg.UCAddr
}

// UCClientID 返回 UCClientID 选项.
func UCClientID() string {
	return cfg.UCClientID
}

// UCClientSecret 返回 UCClientSecret 选项.
func UCClientSecret() string {
	return cfg.UCClientSecret
}

// UCAuditorCron 返回 uc审计拉取周期
func UCAuditorCron() string {
	return cfg.UCAuditorCron
}

// UCAuditorPullSize 返回 uc审计拉取大小
func UCAuditorPullSize() uint64 {
	return cfg.UCAuditorPullSize
}

// CompensationExecCron 审计补偿周期
func CompensationExecCron() string {
	return cfg.CompensationExecCron
}

// UCSyncRecordCleanCron uc同步记录清理周期
func UCSyncRecordCleanCron() string {
	return cfg.UCSyncRecordCleanCron
}
