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

package vk

import (
	"fmt"

	"github.com/erda-project/erda/apistructs"
)

func GetLabelsWithVendor(vendor string) (map[string]string, error) {
	labels := make(map[string]string, 0)

	// ECI and CSI provider must be the same cloud vendor now.
	switch vendor {
	case apistructs.ECIVendorAlibaba:
		labels[apistructs.AlibabaECILabel] = "true"
		return labels, nil
	case "":
		return labels, nil
	default:
		return labels, fmt.Errorf("failed to get label, vendor %s not support", vendor)
	}
}
