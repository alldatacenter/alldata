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

package stackhandlers

import (
	"context"

	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/components/filter"
)

type DefaultStackHandler struct {
	SingleStackName string
}

func NewDefaultStackHandler(single string) *DefaultStackHandler {
	return &DefaultStackHandler{SingleStackName: single}
}

func (h *DefaultStackHandler) GetStacks(ctx context.Context) []Stack {
	return []Stack{{
		Name:  h.SingleStackName,
		Value: "",
		Color: "red",
	}}
}

func (h *DefaultStackHandler) GetIndexer() func(issue interface{}) string {
	return func(issue interface{}) string {
		return ""
	}
}

func (h *DefaultStackHandler) GetFilterOptions(ctx context.Context) []filter.PropConditionOption {
	return getFilterOptions(h.GetStacks(ctx))
}
