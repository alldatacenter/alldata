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

package pvolumes

import (
	"fmt"
	"strconv"

	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/metadata"
)

func GenerateTaskVolume(task spec.PipelineTask, namespace string, volumeID *string) metadata.MetadataField {
	volumeMountPath := MakeTaskContainerVolumeMountDir(namespace)
	vo := metadata.MetadataField{
		Name:  namespace,
		Value: volumeMountPath,
		Type:  string(spec.StoreTypeDiceVolumeNFS),
		Labels: map[string]string{
			VoLabelKeyContextPath:   volumeMountPath,
			VoLabelKeyContainerPath: MakeTaskContainerWorkdir(namespace),
			VoLabelKeyStageOrder:    fmt.Sprintf("%d", task.Extra.StageOrder),
		},
	}
	if volumeID != nil {
		vo.Labels[VoLabelKeyVolumeID] = *volumeID
	}
	return vo
}

func GenerateLocalVolume(namespace string, volumeID *string) metadata.MetadataField {
	vo := metadata.MetadataField{
		Name:  namespace,
		Value: ContainerContextDir,
		Type:  string(spec.StoreTypeDiceVolumeLocal),
		Labels: map[string]string{
			VoLabelKeyShareVolume: strconv.FormatBool(true),
		},
	}
	if volumeID != nil {
		vo.Labels[VoLabelKeyVolumeID] = *volumeID
	}
	return vo
}

func GenerateFakeVolume(namespace string, mountPath string, volumeID *string) metadata.MetadataField {
	vo := metadata.MetadataField{
		Name:  namespace,
		Value: mountPath,
		Type:  string(spec.StoreTypeDiceVolumeFake),
		Labels: map[string]string{
			VoLabelKeyContextPath:   mountPath,
			VoLabelKeyContainerPath: mountPath,
		},
	}
	if volumeID != nil {
		vo.Labels[VoLabelKeyVolumeID] = *volumeID
	}
	return vo
}
