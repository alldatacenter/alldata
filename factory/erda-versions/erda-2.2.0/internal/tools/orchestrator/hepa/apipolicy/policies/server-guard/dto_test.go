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

package serverguard_test

import (
	"testing"

	serverguard "github.com/erda-project/erda/internal/tools/orchestrator/hepa/apipolicy/policies/server-guard"
)

func TestPolicyDto_ApplicationJson(t *testing.T) {
	var dto = serverguard.PolicyDto{RefuseResponse: `{\"success\": true, \"msg\": \"访问频繁\"}`}
	if dto.RefuseResonseCanBeJson() {
		t.Fatal("the refuseResponse can not be json")
	}
	dto.RefuseResponse = `{"success": true, "msg": "访问频繁"}`
	if !dto.RefuseResonseCanBeJson() {
		t.Fatal("the refuseResponse can be json")
	}
}

func TestPolicyDto_RefuseResponseQuote(t *testing.T) {
	var dto = serverguard.PolicyDto{RefuseResponse: `{"success": true, "msg": "访问频繁"}`}
	t.Logf("quote: %s", dto.RefuseResponseQuote())
	quoted := `"{\"success\": true, \"msg\": \"访问频繁\"}"`
	if dto.RefuseResponseQuote() != quoted {
		t.Fatalf("quote error,\n quote: %s,\nquoted: %s", dto.RefuseResponseQuote(), quoted)
	}
}

func TestPolicyDto_AdjustDto(t *testing.T) {
	var dto serverguard.PolicyDto
	{
	}
	dto.MaxTps = -1
	dto.AdjustDto()
	if dto.Switch {
		t.Fatal("switch should be false")
	}

	dto.MaxTps = 100
	dto.RefuseCode = 99
	dto.AdjustDto()
	if dto.RefuseCode != 429 {
		t.Fatal("refuseCode should be 429")
	}

	dto.RefuseCode = 302
	dto.RefuseResponse = "busy"
	dto.AdjustDto()
	if dto.RefuseCode != 429 {
		t.Fatal("refuseCode should be 429")
	}

	dto.ExtraLatency = 10
	dto.AdjustDto()
	if dto.ExtraLatency != 20 {
		t.Fatalf("expected extraLatency: 20, actual: %v", dto.ExtraLatency)
	}
}
