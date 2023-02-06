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
	"path/filepath"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/volume"
	"github.com/erda-project/erda/pkg/jsonstore"
)

var (
	BadVolumeTypeNotNasVolume = errors.New("Bad VolumeType, not nasvolume")
)

const (
	NasVolumeHostPathPrefix = "/netdata/volumes"
)

type NasVolumeDriver struct {
	js jsonstore.JsonStore
}

func NewNasVolumeDriver(js jsonstore.JsonStore) volume.Volume {
	return &NasVolumeDriver{js}
}

func (d NasVolumeDriver) Type() volume.VolumeType {
	return apistructs.NasVolume
}

func (d NasVolumeDriver) Create(config volume.VolumeCreateConfig) (volume.VolumeInfo, error) {
	if config.Type != apistructs.NasVolume {
		return volume.VolumeInfo{}, BadVolumeTypeNotNasVolume
	}
	info, err := defaultCreate(d.js, config)
	if err != nil {
		return volume.VolumeInfo{}, err
	}
	// TODO: create nasvolume on netdata
	return info, nil
}

func (d NasVolumeDriver) Info(ID volume.VolumeIdentity) (volume.VolumeInfo, error) {
	return defaultInfo(d.js, ID)
}

func (d NasVolumeDriver) Attach(ID volume.VolumeIdentity, dst volume.AttachDest) (volume.AttachCallback, error) {
	if err := dst.Validate(); err != nil {
		return nil, errors.Wrap(BadAttachDest, err.Error())
	}
	info, err := defaultInfo(d.js, ID)
	if err != nil {
		return nil, err
	}

	if info.Type != apistructs.NasVolume {
		return nil, BadVolumeTypeNotNasVolume
	}

	ref := volume.VolumeReference{apistructs.AttachDest(dst)}
	info.References = append(info.References, ref)

	_, err = defaultUpdate(d.js, ID, info)
	if err != nil {
		return nil, err
	}
	cb := func(runtime *apistructs.ServiceGroup) (volume.VolumeInfo, error) {
		for i, s := range runtime.Services {
			if s.Name == dst.Service {
				for idx := range s.Volumes {
					runtime.Services[i].Volumes[idx].VolumePath = mkNasVolumeHostPath(info.ID)
				}
			}
		}
		return info, nil
	}
	return cb, nil
}

func (d NasVolumeDriver) UnAttach(ID volume.VolumeIdentity, dst volume.AttachDest) (volume.VolumeInfo, error) {
	if err := dst.Validate(); err != nil {
		return volume.VolumeInfo{}, errors.Wrap(BadAttachDest, err.Error())
	}
	info, err := defaultInfo(d.js, ID)
	if err != nil {
		return volume.VolumeInfo{}, err
	}
	newReferences := []volume.VolumeReference{}
	for _, ref := range info.References {
		if !volume.AttachDest(ref.Info).Equal(dst) {
			newReferences = append(newReferences, ref)
		}
	}
	info.References = newReferences
	_, err = defaultUpdate(d.js, ID, info)
	if err != nil {
		return volume.VolumeInfo{}, err
	}
	return info, nil
}

func (d NasVolumeDriver) Delete(ID volume.VolumeIdentity, force bool) error {
	// TODO: Call soldier to delete
	_, err := defaultSoftDelete(d.js, ID) // soft delete
	return err
}

func mkNasVolumeHostPath(ID string) string {
	return filepath.Join(NasVolumeHostPathPrefix, ID)
}
