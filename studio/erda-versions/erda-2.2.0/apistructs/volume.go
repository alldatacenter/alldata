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

package apistructs

import (
	"fmt"
	"time"
)

// VolumeCreateRequest 创建 volume
// POST: <scheduler>/api/volumes
type VolumeCreateRequest struct {
	// 单位 G, 暂时没有用到

	// 单位 G
	Size int    `json:"size"`
	Type string `json:"type"`
}

// VolumeCreateResponse 创建 volume
// POST: <scheduler>/api/volumes
type VolumeCreateResponse struct {
	Header
	Data VolumeInfo `json:"data"`
}

// VolumeInfoRequest 查询 volume
// GET: <scheduler>/api/volumes/<id>
type VolumeInfoRequest struct {
	ID string `path:"id"`
}

// VolumeInfoResponse 查询 volume
// GET: <scheduler>/api/volumes/<id>
type VolumeInfoResponse struct {
	Header
	Data VolumeInfo `json:"data"`
}

// VolumeDeleteRequest 删除 volume
// DELETE: <scheduler>/api/volumes/<id>
type VolumeDeleteRequest struct {
	ID string `path:"id"`
}

// VolumeDeleteResponse 删除 volume
// DELETE: <scheduler>/api/volumes/<id>
type VolumeDeleteResponse struct {
	Header
}

// VolumeInfo volume 信息
type VolumeInfo struct {
	// volume ID, 可能是 uuid 也可能是 unique name
	ID         string            `json:"id"`
	References []VolumeReference `json:"references"`
	Size       int               `json:"size"`
	Type       VolumeType        `json:"type"`

	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
	DeletedAt time.Time `json:"deleted_at"`
}

// VolumeType volume 类型
type VolumeType string

const (
	// LocalVolume 本地盘
	LocalVolume VolumeType = "local"
	// NasVolume nas网盘
	NasVolume VolumeType = "nas"
)

const (
	// LocalVolumeStr LocalVolume 的 string 表示
	LocalVolumeStr = "localvolume"
	// NasVolumeStr NasVolume 的 string 表示
	NasVolumeStr = "nasvolume"
	// LocalVolumeHex 本地盘的hex编码表示，用于作为 VolumeID 的 prefix
	// LocalVolumeHex = hex.EncodeToString([]byte("local"))
	LocalVolumeHex = "6c6f63616c"
	// NasVolumeHex nas盘的hex编码表示，用于作为 VolumeID 的 prefix
	// NasVolumeHex = hex.EncodeToString([]byte("nas"))
	NasVolumeHex = "6e6173"
)

// VolumeTypeFromString 从 string 转化成 VolumeType
func VolumeTypeFromString(s string) (VolumeType, error) {
	switch s {
	case "local":
		fallthrough
	case LocalVolumeStr:
		return LocalVolume, nil
	case "nas":
		fallthrough
	case NasVolumeStr:
		return NasVolume, nil
	}
	return LocalVolume, fmt.Errorf("illegal volume type: %s", s)
}

// VolumeReference Volume 被使用的信息
type VolumeReference struct {
	Info AttachDest
}

// AttachDest Volume Attach 目的地的信息
type AttachDest struct {
	//runtime.Service[x].Namespace
	Namespace string

	Service string
	// 容器中的路径
	Path string
}
