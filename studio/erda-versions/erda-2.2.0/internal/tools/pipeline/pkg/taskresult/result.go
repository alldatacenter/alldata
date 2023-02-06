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

package taskresult

import (
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskerror"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskinspect"
	"github.com/erda-project/erda/pkg/metadata"
)

type Result struct {
	// Metadata stores meta from callback.
	Metadata metadata.Metadata `json:"metadata,omitempty"`

	// Errors stores from callback, not pipeline internal(like reconciler).
	// For internal errors, use taskinspect.Inspect.Errors.
	Errors taskerror.OrderedErrors `json:"errors,omitempty"`
}

type LegacyResult struct {
	Result

	// Below fields combined from task inspect now.
	// Exists just for exhibition.

	// Deprecated
	MachineStat *taskinspect.PipelineTaskMachineStat `json:"machineStat,omitempty"`
	// Deprecated
	Inspect string `json:"inspect,omitempty"`
	// Deprecated
	Events string `json:"events,omitempty"`
}
