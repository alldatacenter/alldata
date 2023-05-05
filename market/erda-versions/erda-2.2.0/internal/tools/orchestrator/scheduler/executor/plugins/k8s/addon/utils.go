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

package addon

import (
	"strings"

	"github.com/sirupsen/logrus"
	"k8s.io/apimachinery/pkg/util/validation"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

func SetAddonLabelsAndAnnotations(service apistructs.Service, labels, annotations map[string]string) {
	for lk, lv := range service.Labels {
		if strings.HasPrefix(lk, LabelKeyPrefix) {
			annotations[strings.TrimPrefix(lk, LabelKeyPrefix)] = lv
			continue
		} else {
			if errs := validation.IsValidLabelValue(lv); len(errs) > 0 {
				logrus.Warnf("Label key: %s with invalid value: %s will not convert to j8s label.", lk, lv)
				continue
			}
			labels[lk] = lv
			if lk == apistructs.AlibabaECILabel && lv == "true" {
				images := strings.Split(service.Image, "/")
				if len(images) >= 2 {
					annotations[diceyml.AddonImageRegistry] = images[0]
				}
			}
		}
	}

	for lk, lv := range service.DeploymentLabels {
		if strings.HasPrefix(lk, LabelKeyPrefix) {
			annotations[strings.TrimPrefix(lk, LabelKeyPrefix)] = lv
		} else {
			labels[lk] = lv
			if lk == apistructs.AlibabaECILabel && lv == "true" {
				images := strings.Split(service.Image, "/")
				if len(images) >= 2 {
					annotations[diceyml.AddonImageRegistry] = images[0]
				}
			}
		}
	}
}
