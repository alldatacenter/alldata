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

// ExpandRequestBody 如果 request body 中引用了 Components, 则将 request body 的结构展开, 直到不再包含任何引用的结构
func ExpandRequestBody(ref *openapi3.RequestBodyRef, oas3 *openapi3.Swagger) error {
	if ref == nil {
		return errors.New("RequestBodyRef is nil")
	}
	if oas3 == nil {
		return errors.New("Swagger is nil")
	}

	if ref.Value == nil {
		return nil
	}

	if len(ref.Value.Content) == 0 {
		return nil
	}

	for mediaTypeName, mediaType := range ref.Value.Content {
		if mediaType.Schema == nil {
			continue
		}
		if err := ExpandSchemaRef(mediaType.Schema, oas3); err != nil {
			return errors.Wrapf(err, "failed to ExpandSchemaRef, mediaType: %s", mediaTypeName)
		}
	}

	return nil
}

func dedupeStringSlice(ss []string) []string {
	var (
		m      = make(map[string]bool, 0)
		result []string
	)
	for _, s := range ss {
		if _, ok := m[s]; !ok {
			result = append(result, s)
			m[s] = true
		}
	}
	return result
}
