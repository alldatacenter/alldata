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

package pipelineymlv1

import "github.com/pkg/errors"

var (
	errDuplicateResTypes      = errors.New("duplicate resource_type found")
	errDuplicateRes           = errors.New("duplicate resource found")
	errInvalidTypeOfResType   = errors.New("type of resource_type invalid, only support [" + DockerImageResType + "]")
	errInvalidSourceOfResType = errors.New("source of resource_type invalid")

	errLackResName = errors.New("lack of resource name")
	errLackResType = errors.New("lack of resource type")

	errUnknownResTypes = errors.New("unknown resource_types found")
	errUnusedResTypes  = errors.New("unused resource_types found")

	errUnusedResources = errors.New("unused resources found")

	errInvalidVersion = errors.New("invalid version")

	errTempTaskConfigsSize = errors.New("temporary the size of tasks list limit to 1")

	errNoClusterNameSpecify = errors.New("no clusterName specified")

	errInvalidResource = errors.New("invalid resource")

	errNotAvailableInContext = errors.New("not available in context currently")

	errDuplicateOutput = errors.New("this output already exist")

	errDuplicateTaskName = errors.New("task name already used")

	errNilPipelineYmlObj = errors.New("PipelineYml.obj is nil pointer")

	errInvalidStepTaskConfig = errors.New("invalid step task config found, type should be one of: get, put, task")

	errDecodeGetStepTask  = errors.New("error decode get step task")
	errDecodePutStepTask  = errors.New("error decode put step task")
	errDecodeTaskStepTask = errors.New("error decode task step task")

	errMissingNFSRealPath = errors.New("missing nfs real path for context store")

	errTriggerScheduleCron      = errors.New("invalid trigger schedule cron syntax")
	errTriggerScheduleFilters   = errors.New("invalid trigger schedule filter syntax")
	errTooManyLegalTriggerFound = errors.New("more than one legal triggers found!")
)
