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

package metadata

import (
	"strings"
)

type MetadataLevel string

var (
	MetadataLevelError MetadataLevel = "ERROR"
	MetadataLevelWarn  MetadataLevel = "WARN"
	MetadataLevelInfo  MetadataLevel = "INFO"
)

type (
	MetadataField struct {
		Name     string            `json:"name"`
		Value    string            `json:"value"`
		Type     string            `json:"type,omitempty"`
		Optional bool              `json:"optional,omitempty"`
		Labels   map[string]string `json:"labels,omitempty"`
		Level    MetadataLevel     `json:"level,omitempty"`
	}

	Metadata []MetadataField

	MetadataFieldType string
)

func (field MetadataField) GetLevel() MetadataLevel {
	if field.Level != "" {
		return field.Level
	}
	// judge by prefix
	idx := strings.Index(field.Name, ".")
	prefix := ""
	if idx != -1 {
		prefix = field.Name[:idx]
	} else {
		prefix = field.Name
	}
	switch MetadataLevel(strings.ToUpper(prefix)) {
	case MetadataLevelError:
		return MetadataLevelError
	case MetadataLevelWarn:
		return MetadataLevelWarn
	case MetadataLevelInfo:
		return MetadataLevelInfo
	}

	// fallback
	return MetadataLevelInfo
}

func (metadata Metadata) DedupByName() Metadata {
	tmp := make(map[string]struct{})
	dedup := make(Metadata, 0)
	for _, each := range metadata {
		if _, ok := tmp[each.Name]; ok {
			continue
		}
		tmp[each.Name] = struct{}{}
		dedup = append(dedup, each)
	}
	return dedup
}

// FilterNoErrorLevel filter by field level, return collection of NotErrorLevel and ErrorLevel.
func (metadata Metadata) FilterNoErrorLevel() (notErrorLevel, errorLevel Metadata) {
	for _, field := range metadata {
		if field.GetLevel() == MetadataLevelError {
			errorLevel = append(errorLevel, field)
			continue
		}
		notErrorLevel = append(notErrorLevel, field)
	}
	return
}
