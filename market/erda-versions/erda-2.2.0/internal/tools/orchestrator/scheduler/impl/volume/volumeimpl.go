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

package volume

import (
	"github.com/erda-project/erda/apistructs"
)

type VolumeImpl struct {
	drivers map[apistructs.VolumeType]Volume
}

func NewVolumeImpl(drivers map[apistructs.VolumeType]Volume) Volume {
	return &VolumeImpl{drivers}
}

func (VolumeImpl) Type() VolumeType {
	panic("shouldn't call me")
}

func (i VolumeImpl) Create(config VolumeCreateConfig) (VolumeInfo, error) {
	return i.drivers[config.Type].Create(config)
}

func (i VolumeImpl) Info(ID VolumeIdentity) (VolumeInfo, error) {
	driver, err := GetDriver(i.drivers, ID)
	if err != nil {
		return VolumeInfo{}, err
	}
	return driver.Info(ID)
}

func (i VolumeImpl) Attach(ID VolumeIdentity, dst AttachDest) (AttachCallback, error) {
	driver, err := GetDriver(i.drivers, ID)
	if err != nil {
		return nil, err
	}
	return driver.Attach(ID, dst)
}
func (i VolumeImpl) UnAttach(ID VolumeIdentity, dst AttachDest) (VolumeInfo, error) {
	driver, err := GetDriver(i.drivers, ID)
	if err != nil {
		return VolumeInfo{}, err
	}
	return driver.UnAttach(ID, dst)
}

func (i VolumeImpl) Delete(ID VolumeIdentity, force bool) error {
	driver, err := GetDriver(i.drivers, ID)
	if err != nil {
		return err
	}
	return driver.Delete(ID, force)
}

func GetDriver(drivers map[VolumeType]Volume, ID VolumeIdentity) (Volume, error) {
	tp, err := DecodeVolumeType(string(ID))
	if err != nil {
		return nil, err
	}
	return drivers[tp], nil

}
