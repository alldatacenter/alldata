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

package dto_test

import (
	"testing"
	"time"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong/dto"
)

// need not test on method AdjustCreatedAt
func TestKongCredentialDto_AdjustCreatedAt(t *testing.T) {
	var d dto.KongCredentialDto
	now := time.Now().Unix()
	d.CreatedAt = now
	d.AdjustCreatedAt()
	t.Logf("now: %d, createdAt: %d", now, d.CreatedAt)
}
