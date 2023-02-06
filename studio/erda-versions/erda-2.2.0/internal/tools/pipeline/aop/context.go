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

package aop

import (
	"context"

	"github.com/erda-project/erda/internal/tools/pipeline/aop/aoptypes"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

// NewContextForPipeline 用于快速构造流水线 AOP 上下文
func NewContextForPipeline(p spec.Pipeline, trigger aoptypes.TuneTrigger, customKVs ...map[interface{}]interface{}) *aoptypes.TuneContext {
	ctx := aoptypes.TuneContext{
		Context: context.Background(),
		SDK:     globalSDK.Clone(),
	}
	ctx.SDK.TuneType = aoptypes.TuneTypePipeline
	ctx.SDK.TuneTrigger = trigger
	ctx.SDK.Pipeline = p
	// 用户自定义上下文
	for _, kvs := range customKVs {
		for k, v := range kvs {
			ctx.PutKV(k, v)
		}
	}
	return &ctx
}

// NewContextForTask 用于快速构任务 AOP 上下文
func NewContextForTask(task spec.PipelineTask, p spec.Pipeline, trigger aoptypes.TuneTrigger, customKVs ...map[interface{}]interface{}) *aoptypes.TuneContext {
	// 先构造 pipeline 上下文
	ctx := NewContextForPipeline(p, trigger, customKVs...)
	// 修改 tune type
	ctx.SDK.TuneType = aoptypes.TuneTypeTask
	// 注入特有 sdk 属性
	ctx.SDK.Task = task
	return ctx
}
