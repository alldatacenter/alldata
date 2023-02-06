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

package quality_chart

import (
	"context"

	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
)

const (
	i18nKeyPrefix     = "quality_score_tip--"
	paddingLeftNormal = uint64(16)
	paddingLeftBold   = uint64(0)
)

func genTipI18nKey(key string) string {
	return i18nKeyPrefix + key
}

func genNormalTipLine(ctx context.Context, key string) TipLine {
	return TipLine{Text: cputil.I18n(ctx, genTipI18nKey(key)), Style: TipLineStyle{FontWeight: fontWeightNormal, PaddingLeft: 16}}
}

func genBoldTipLine(ctx context.Context, key string) TipLine {
	return TipLine{Text: cputil.I18n(ctx, genTipI18nKey(key)), Style: TipLineStyle{FontWeight: fontWeightBold, PaddingLeft: paddingLeftBold}}
}

func genTip(ctx context.Context) []TipLine {
	return []TipLine{
		// title
		genBoldTipLine(ctx, "title"),

		// mt plan score
		genBoldTipLine(ctx, "mt_plan"),
		genNormalTipLine(ctx, "mt_plan_rate_passed"),
		genNormalTipLine(ctx, "mt_plan_rate_executed"),
		genNormalTipLine(ctx, "mt_plan_score_formula"),

		// at plan score
		genBoldTipLine(ctx, "at_plan"),
		genNormalTipLine(ctx, "at_plan_rate_passed"),
		genNormalTipLine(ctx, "at_plan_rate_executed"),
		genNormalTipLine(ctx, "at_plan_score_formula"),

		// bug score
		genBoldTipLine(ctx, "unclosed_bug"),
		genNormalTipLine(ctx, "unclosed_bug_DI"),
		genNormalTipLine(ctx, "unclosed_bug_score_formula"),

		// bug reopen
		genBoldTipLine(ctx, "bug_reopen"),
		genNormalTipLine(ctx, "bug_reopen_rate"),
		genNormalTipLine(ctx, "bug_reopen_score_formula"),

		// coco
		genBoldTipLine(ctx, "coco"),
		genNormalTipLine(ctx, "coco_score_formula"),

		// total
		genBoldTipLine(ctx, "total"),
		genNormalTipLine(ctx, "total_score_formula"),
	}
}
