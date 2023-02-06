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

package taskpolicy

import (
	"context"
	"fmt"

	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

type Interface interface {
	AdaptPolicy(ctx context.Context, task *spec.PipelineTask) error
}

func (p *provider) AdaptPolicy(ctx context.Context, task *spec.PipelineTask) error {
	if task == nil {
		return fmt.Errorf("task is empty")
	}
	if task.Extra.Action.Policy == nil {
		return nil
	}

	policy := p.supportedPolicies[task.Extra.Action.Policy.Type]
	if policy == nil {
		return nil
	}

	return policy.AdaptPolicy(ctx, task)
}
