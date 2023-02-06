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

package driver

import (
	"context"
	"path/filepath"
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/volume"
	"github.com/erda-project/erda/pkg/jsonstore"
)

var (
	ExistedVolumeID          = errors.New("Existed Volume ID")
	NotFoundVolume           = errors.New("Not found volume")
	VolumeNameReferNilVolume = errors.New("volume name refer to nil volume")
)

// etcd Volume information stored in etcd
// key: /volume/<id>
// val: VolumeInfo
type etcdVolumeInfo volume.VolumeInfo

// defaultCreate Store volume metadata in etcd
func defaultCreate(js jsonstore.JsonStore, config volume.VolumeCreateConfig) (volume.VolumeInfo, error) {
	id, err := volume.NewVolumeID(config)
	if err != nil {
		return volume.VolumeInfo{}, err
	}
	info := volume.VolumeInfo{
		ID:        id.String(),
		CreatedAt: time.Now(),
		UpdatedAt: time.Now(),
		Size:      config.Size,
		Type:      config.Type,
	}
	// TODO(zj): Here there is a concurrency problem with Get first and then Set
	// Later, STM support will be added to jsonstore to facilitate the use and avoid directly using the stm of the etcd library
	IDPath := mkIndexEtcdPath(volume.ETCDVolumeMetadataDir, id.String())
	infoToEtcd := etcdVolumeInfo(info)
	notfound, err := js.Notfound(context.Background(), IDPath)
	if err != nil {
		return volume.VolumeInfo{}, err
	}
	if !notfound {
		return volume.VolumeInfo{}, ExistedVolumeID
	}

	if err = js.Put(context.Background(), IDPath, infoToEtcd); err != nil {
		return volume.VolumeInfo{}, err
	}
	return info, nil
}

func defaultInfo(js jsonstore.JsonStore, ID volume.VolumeIdentity) (volume.VolumeInfo, error) {
	path := mkIndexEtcdPath(volume.ETCDVolumeMetadataDir, ID.String())
	var v etcdVolumeInfo
	if err := js.Get(context.Background(), path, &v); err != nil {
		if err == jsonstore.NotFoundErr {
			return volume.VolumeInfo{}, NotFoundVolume
		}
		return volume.VolumeInfo{}, err
	}
	return volume.VolumeInfo(v), nil
}

func defaultDelete(js jsonstore.JsonStore, ID volume.VolumeIdentity) (volume.VolumeInfo, error) {
	path := mkIndexEtcdPath(volume.ETCDVolumeMetadataDir, ID.String())
	var info etcdVolumeInfo
	if err := js.Get(context.Background(), path, &info); err != nil {
		if err == jsonstore.NotFoundErr {
			return volume.VolumeInfo{}, NotFoundVolume
		}
		return volume.VolumeInfo{}, err
	}
	var unused interface{}
	if err := js.Remove(context.Background(), path, &unused); err != nil {
		return volume.VolumeInfo{}, err
	}
	return volume.VolumeInfo(info), nil
}

// defaultUpdate Currently only the References value in VolumeInfo will be updated,
// Return volumeinfo before update
func defaultUpdate(js jsonstore.JsonStore, ID volume.VolumeIdentity, vlm volume.VolumeInfo) (volume.VolumeInfo, error) {
	path := mkIndexEtcdPath(volume.ETCDVolumeMetadataDir, ID.String())
	var info etcdVolumeInfo
	if err := js.Get(context.Background(), path, &info); err != nil {
		if err == jsonstore.NotFoundErr {
			return volume.VolumeInfo{}, NotFoundVolume
		}
		return volume.VolumeInfo{}, err
	}
	old := volume.VolumeInfo(info)
	info.UpdatedAt = time.Now()
	info.References = vlm.References
	var err error
	if err = js.Put(context.Background(), path, info); err != nil {
		return volume.VolumeInfo{}, err
	}
	return old, nil
}

func defaultSoftDelete(js jsonstore.JsonStore, ID volume.VolumeIdentity) (volume.VolumeInfo, error) {
	path := mkIndexEtcdPath(volume.ETCDVolumeMetadataDir, ID.String())
	var info etcdVolumeInfo
	if err := js.Get(context.Background(), path, &info); err != nil {
		if err == jsonstore.NotFoundErr {
			return volume.VolumeInfo{}, NotFoundVolume
		}
		return volume.VolumeInfo{}, err
	}
	info.DeletedAt = time.Now()
	if err := js.Put(context.Background(), path, info); err != nil {
		return volume.VolumeInfo{}, err
	}
	return volume.VolumeInfo(info), nil
}

func mkIndexEtcdPath(dir string, name string) string {
	return filepath.Join(dir, name)
}
