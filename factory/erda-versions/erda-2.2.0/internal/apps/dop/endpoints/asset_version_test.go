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

package endpoints_test

import (
	"testing"

	"github.com/erda-project/erda/internal/apps/dop/endpoints"
)

func TestAttachment(t *testing.T) {
	var (
		assetID      = "demo"
		version      = "1"
		specProtocol = "oas2-json"
	)
	attachment := endpoints.Attachment(assetID, version, specProtocol)
	t.Log(attachment)
	if attachment != `attachment; filename="demo-1-oas2.json"` {
		t.Error("error")
	}

	specProtocol = "csv"
	attachment = endpoints.Attachment(assetID, version, specProtocol)
	t.Log(attachment)
	if attachment != `attachment; filename="demo-1-oas3.csv"` {
		t.Error("error")
	}
}
