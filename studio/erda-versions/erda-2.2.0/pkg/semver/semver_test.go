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

package semver

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestValid(t *testing.T) {
	validVersions := []string{
		"3.4.1",
		"v3.4.1",
		"3.5.0",
		"v3.5.0",
		"3.4.1-fix-your-bug",
		"v3.5.0-fix-123-bug-456",
	}
	for _, ver := range validVersions {
		assert.True(t, Valid(ver), ver)
	}

	invalidVersions := []string{
		"3",
		"v3",
		"3.4",
		"v3.4",
		"3.4.1.1",
		"v3.4.1.1",
		"v3.4.1@",
	}
	for _, ver := range invalidVersions {
		assert.False(t, Valid(ver), ver)
	}
}

func TestNew(t *testing.T) {
	v1 := New(3)
	assert.Equal(t, "3.0.0", v1)

	v2 := New(3, 5)
	assert.Equal(t, "3.5.0", v2)

	v3 := New(3, 5, 1)
	assert.Equal(t, "3.5.1", v3)
}
