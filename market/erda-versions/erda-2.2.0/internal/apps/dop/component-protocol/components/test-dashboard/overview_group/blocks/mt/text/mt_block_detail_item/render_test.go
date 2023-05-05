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

package mt_block_detail_item

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/numeral"
	"github.com/erda-project/erda/pkg/strutil"
)

func Test_makeMtCaseNumAndRate(t *testing.T) {
	ctx := context.Background()
	ctx = context.WithValue(ctx, cptype.GlobalInnerKeyCtxSDK, &cptype.SDK{Tran: &i18n.NopTranslator{}})
	plan1 := apistructs.TestPlan{RelsCount: apistructs.TestPlanRelsCount{Total: 10, Init: 4, Succ: 3, Fail: 2, Block: 1}}
	plan2 := apistructs.TestPlan{RelsCount: apistructs.TestPlanRelsCount{Total: 10, Init: 1, Succ: 2, Fail: 3, Block: 4}}
	plan3 := apistructs.TestPlan{RelsCount: apistructs.TestPlanRelsCount{Total: 20, Init: 0, Succ: 20, Fail: 0, Block: 0}}
	plan4 := apistructs.TestPlan{RelsCount: apistructs.TestPlanRelsCount{Total: 30, Init: 0, Succ: 28, Fail: 1, Block: 1}}
	mtPlans := []apistructs.TestPlan{plan1, plan2, plan3, plan4}

	// total
	totalValue := makeMtCaseNumTotal(ctx, mtPlans)
	assert.Equal(t, strutil.String(10+10+20+30), totalValue.Value)

	// block
	blockValue := makeMtCaseNumBlock(ctx, mtPlans)
	assert.Equal(t, strutil.String(1+4+0+1), blockValue.Value)

	// succ
	succValue := makeMtCaseNumSucc(ctx, mtPlans)
	assert.Equal(t, strutil.String(3+2+20+28), succValue.Value)

	// fail
	failValue := makeMtCaseNumFail(ctx, mtPlans)
	assert.Equal(t, strutil.String(2+3+0+1), failValue.Value)

	// init
	initValue := makeMtCaseNumInit(ctx, mtPlans)
	assert.Equal(t, strutil.String(4+1+0+0), initValue.Value)

	// passed rate
	passedRateValue := makeMtCaseRatePassed(ctx, mtPlans)
	assert.Equal(t, strutil.String(numeral.Round(float64(3+2+20+28)/float64(10+10+20+30)*100, 2))+"%", passedRateValue.Value)

	// executed rate
	executedRateValue := makeMtCaseRateExecuted(ctx, mtPlans)
	assert.Equal(t, strutil.String(numeral.Round(float64(10+10+20+30-(4+1+0+0))/float64(10+10+20+30)*100, 2))+"%", executedRateValue.Value)
}
