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

package utils

import (
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

// IsProjectECIEnable 检查项目对应的环境是否开启 ECI 功能
func IsProjectECIEnable(bdl *bundle.Bundle, projectID uint64, workspace string, orgID uint64, userID string) bool {
	ablilities, err := bdl.GetProjectWorkSpaceAbilities(projectID, workspace, orgID, userID)
	if err != nil {
		logrus.Errorf("get project workspace abilities failed for project %d wokrspace %s, error: %v", projectID, workspace, err)
		return false
	}

	logrus.Infof("get project workspace abilities for project %d wokrspace %s result is: %#v", projectID, workspace, ablilities)
	if ablilities != nil {
		if v, ok := ablilities["ECI"]; ok && v == "enable" {
			return true
		}
	}
	return false
}

func AddECIConfigToServiceGroupCreateV2Request(sg *apistructs.ServiceGroupCreateV2Request, vendor string) {
	for name := range sg.DiceYml.Services {
		// ECI Labels
		if sg.DiceYml.Services[name].Labels == nil {
			sg.DiceYml.Services[name].Labels = make(map[string]string)
		}
		switch vendor {
		case apistructs.ECIVendorAlibaba:
			sg.DiceYml.Services[name].Labels[apistructs.AlibabaECILabel] = "true"

		case apistructs.ECIVendorHuawei:
			sg.DiceYml.Services[name].Labels[apistructs.HuaweiCCILabel] = "force"
		case apistructs.ECIVendorTecent:
			sg.DiceYml.Services[name].Labels[apistructs.TecentEKSNodeSelectorKey] = apistructs.TecentEKSNodeSelectorValue
		default:
			sg.DiceYml.Services[name].Labels[apistructs.AlibabaECILabel] = "true"
		}

		// Services Volumes
		if len(sg.DiceYml.Services[name].Volumes) > 0 {
			for index, vol := range sg.DiceYml.Services[name].Volumes {
				sg.DiceYml.Services[name].Volumes[index] = ConvertVolume(vol)
			}
		}
	}
}

func ConvertVolume(volume diceyml.Volume) diceyml.Volume {
	convertedVolume := diceyml.Volume{}

	if volume.Path != "" {
		// old volumes define
		convertedVolume.TargetPath = volume.Path
		if volume.Storage == "nfs" || volume.Storage == "" {
			convertedVolume.Type = apistructs.VolumeTypeNAS
		} else {
			convertedVolume.Type = apistructs.VolumeTypeSSD
		}

	} else {
		// new volumes define
		convertedVolume.TargetPath = volume.TargetPath
		convertedVolume.Type = volume.Type
	}

	if volume.Capacity <= 0 {
		convertedVolume.Capacity = diceyml.ProjectECIVolumeDefaultSize
	} else {
		convertedVolume.Capacity = volume.Capacity
	}

	if volume.Snapshot != nil {
		convertedVolume.Snapshot = volume.Snapshot
	} else {
		convertedVolume.Snapshot = &diceyml.VolumeSnapshot{
			MaxHistory: 0,
		}
	}
	return convertedVolume
}
