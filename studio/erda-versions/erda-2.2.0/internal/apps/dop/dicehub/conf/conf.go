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

// Package conf Dicehub 环境配置
package conf

import (
	"github.com/erda-project/erda/pkg/envconf"
)

var cfg Conf

// Conf DiceHub 环境变量配置项
type Conf struct {
	ListenAddr      string              `env:"LISTEN_ADDR" default:":10000"`
	MonitorAddr     string              `env:"MONITOR_ADDR" default:"monitor.default.svc.cluster.local:7096"`
	Debug           bool                `env:"DEBUG" default:"false"`
	OpsAddr         string              `env:"OPS_ADDR"`
	EventBoxAddr    string              `env:"EVENTBOX_ADDR"`
	GCSwitch        bool                `env:"RELEASE_GC_SWITCH" default:"true"`
	MaxTimeReserved string              `env:"RELEASE_MAX_TIME_RESERVED" default:"72"` // default: 72h
	ExtensionMenu   map[string][]string `env:"EXTENSION_MENU" default:"{}"`
	SiteUrl         string              `env:"SITE_URL"`
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

// OpsAddr 返回ops地址
func OpsAddr() string {
	return cfg.OpsAddr
}

func SiteUrl() string {
	return cfg.SiteUrl
}

// EventBoxAddr 返回eventbox地址
func EventBoxAddr() string {
	return cfg.EventBoxAddr
}

// GCSwitch release自动回收开关
func GCSwitch() bool {
	return cfg.GCSwitch
}

// MaxTimeReserved 未使用的release保留最长时间
func MaxTimeReserved() string {
	return cfg.MaxTimeReserved
}

// ExtensionMenu 服务扩展菜单配置
func ExtensionMenu() map[string][]string {
	return cfg.ExtensionMenu
}

// MonitorAddr monitor
func MonitorAddr() string {
	return cfg.MonitorAddr
}
