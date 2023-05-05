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

// Package event manipulates the k8s api of event object
package event

var (
	errPullImage               = "拉取镜像失败"
	errImageNeverPull          = "服务配置了永不拉取镜像，而调度的宿主机又没有对应镜像"
	errInvalidImageName        = "无效的镜像名"
	errNetworkNotReady         = "节点网络组件异常"
	errHostPortConflict        = "宿主机端口冲突"
	errNodeSelectorMismatching = "节点标签不匹配"
	errInsufficientFreeCPU     = "CPU 资源不足"
	errInsufficientFreeMemory  = "内存资源不足"
	errMountVolume             = "存储卷挂载失败"
	errAlreadyMountedVolume    = "存储卷已经被挂载过"
	errNodeRebooted            = "节点重启"
)
