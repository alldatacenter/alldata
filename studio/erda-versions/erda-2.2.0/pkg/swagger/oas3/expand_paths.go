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

package oas3

import (
	"github.com/getkin/kin-openapi/openapi3"
	"github.com/pkg/errors"
)

// ExpandPaths 展开所有路径中的所有请求体和响应体
func ExpandPaths(oas3 *openapi3.Swagger) error {
	if len(oas3.Paths) == 0 {
		return nil
	}
	for path_, pathItem := range oas3.Paths {
		for _, operation := range []*openapi3.Operation{
			pathItem.Connect, pathItem.Delete, pathItem.Get,
			pathItem.Head, pathItem.Options, pathItem.Patch,
			pathItem.Post, pathItem.Put, pathItem.Trace,
		} {
			if err := ExpandOperation(operation, oas3); err != nil {
				return errors.Wrapf(err, "failed to ExpandOperation, path: %s", path_)
			}
		}
	}
	return nil
}
