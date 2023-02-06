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
)

// ExpandOperation 展开所有 operation 中的请求体和响应体
func ExpandOperation(operation *openapi3.Operation, oas3 *openapi3.Swagger) error {
	if operation == nil {
		return nil
	}
	if len(operation.Parameters) > 0 {
		for _, parameterRef := range operation.Parameters {
			if parameterRef.Value == nil || parameterRef.Value.Schema == nil {
				continue
			}
			if err := ExpandSchemaRef(parameterRef.Value.Schema, oas3); err != nil {
				return err
			}
		}
	}
	if operation.RequestBody != nil {
		if err := ExpandRequestBody(operation.RequestBody, oas3); err != nil {
			return err
		}
	}
	if len(operation.Responses) != 0 {
		if err := ExpandResponses(operation.Responses, oas3); err != nil {
			return err
		}
	}
	return nil
}
