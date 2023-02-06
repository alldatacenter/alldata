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

package storage

import (
	"fmt"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
)

func VolumeTypeToSCName(diskType string, vendor string) (string, error) {
	// 卷类型以 'DICE-' 作为前缀的，表示使用 dice volume 插件，与 vender 无关
	switch diskType {
	case apistructs.VolumeTypeDiceNAS:
		// volume.type == "DICE-NAS" SC="dice-nfs-volume"
		return apistructs.DiceNFSVolumeSC, nil

	case apistructs.VolumeTypeDiceLOCAL:
		// volume.type == "DICE-LOCAL" SC="dice-local-volume"
		return apistructs.DiceLocalVolumeSC, nil

	// 卷类型不以 'DICE-' 作为前缀的，表示与 vender 有关，结合 vendor 选择 sc
	// 'SSD'
	case apistructs.VolumeTypeSSD:
		switch vendor {
		case apistructs.CSIVendorAlibaba:
			// vendor = "AliCloud"  volume.type="SSD"  SC="alicloud-disk-ssd-on-erda"
			return apistructs.AlibabaSSDSC, nil

		case apistructs.CSIVendorTencent:
			// vendor = "TencentCloud"  volume.type="SSD"  SC="tencent-disk-ssd-on-erda"
			return apistructs.TencentSSDSC, nil

		case apistructs.CSIVendorHuawei:
			// vendor = "HuaweiCloud"  volume.type="SSD"  SC="huawei-disk-ssd-on-erda"
			return apistructs.HuaweiSSDSC, nil
		}
	// 'NAS'
	case apistructs.VolumeTypeNAS:
		switch vendor {
		case apistructs.CSIVendorAlibaba:
			// vendor = "AliCloud"  volume.type="SSD"  SC="alicloud-disk-ssd-on-erda"
			return apistructs.AlibabaNASSC, nil

		case apistructs.CSIVendorTencent:
			// vendor = "TencentCloud"  volume.type="SSD"  SC="tencent-disk-ssd-on-erda"
			return apistructs.TencentNASSC, nil

		case apistructs.CSIVendorHuawei:
			// vendor = "HuaweiCloud"  volume.type="SSD"  SC="huawei-disk-ssd-on-erda"
			return apistructs.HuaweiNASSC, nil
		}
	/*
		// 'OSS'
		case apistructs.VolumeTypeOSS:
			switch vendor {
			case apistructs.CSIVendorAlibaba:
				// vendor = "AliCloud"  volume.type="SSD"  SC="alicloud-disk-ssd-on-erda"
				return apistructs.AlibabaOSSSC, nil

			case apistructs.CSIVendorTencent:
				// vendor = "TencentCloud"  volume.type="SSD"  SC="tencent-disk-ssd-on-erda"
				return apistructs.TencentOSSSC, nil

			case apistructs.CSIVendorHuawei:
				// vendor = "HuaweiCloud"  volume.type="SSD"  SC="huawei-disk-ssd-on-erda"
				return apistructs.HuaweiOSSSC, nil
			}
	*/
	case "":
		return apistructs.DiceLocalVolumeSC, nil
	//case apistructs.VolumeTypeOSS:
	default:
		return "", errors.New(fmt.Sprintf("unsupported disk type %s", diskType))
	}

	return "", errors.New(fmt.Sprintf("can not detect storageclass for disktype [%s] vendor [%s] ", diskType, vendor))
}
