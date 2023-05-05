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
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestMetadataField_GetLevel(t *testing.T) {
	field := MetadataField{Name: "Error.web"}
	assert.Equal(t, MetadataLevelError, field.GetLevel())

	field = MetadataField{Name: "ERROR.web"}
	assert.Equal(t, MetadataLevelError, field.GetLevel())

	field = MetadataField{Name: " ERROR.web"}
	assert.Equal(t, MetadataLevelInfo, field.GetLevel())

	field = MetadataField{Name: "ERROR"}
	assert.Equal(t, MetadataLevelError, field.GetLevel())

	field = MetadataField{Name: "WARN.."}
	assert.Equal(t, MetadataLevelWarn, field.GetLevel())

	field = MetadataField{Name: "warn"}
	assert.Equal(t, MetadataLevelWarn, field.GetLevel())

	field = MetadataField{Name: "INFO.x"}
	assert.Equal(t, MetadataLevelInfo, field.GetLevel())

	field = MetadataField{Name: "Error.x", Level: MetadataLevelInfo}
	assert.Equal(t, MetadataLevelInfo, field.GetLevel())
}
