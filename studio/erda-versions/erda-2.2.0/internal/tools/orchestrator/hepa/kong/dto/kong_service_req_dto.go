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

package dto

type KongServiceReqDto struct {
	// 服务名称,选填
	Name string `json:"name,omitempty"`
	// 具体路径，如果设置了这个可以不设下面4个
	Url string `json:"url,omitempty"`
	// 1、协议，默认http
	Protocol string `json:"protocol,omitempty"`
	// 2、主机，没设url则必传
	Host string `json:"host,omitempty"`
	// 3、端口，默认80
	Port int `json:"port,omitempty"`
	// 4、路径，默认null
	Path string `json:"path,omitempty"`
	// 选填，重试次数
	Retries *int `json:"retries,omitempty"`
	// 选填，连接超时时间
	ConnectTimeout int `json:"connect_timeout,omitempty"`
	// 选填，写超时时间
	WriteTimeout int `json:"write_timeout,omitempty"`
	// 选填，读超时时间
	ReadTimeout int `json:"read_timeout,omitempty"`
	// 真正的服务id，更新时使用
	ServiceId string `json:"-"`
}

// IsEmpty
func (dto KongServiceReqDto) IsEmpty() bool {
	return len(dto.Url) == 0 && len(dto.Host) == 0
}
