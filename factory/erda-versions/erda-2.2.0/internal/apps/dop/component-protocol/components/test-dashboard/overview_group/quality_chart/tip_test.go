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
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
)

func Test_genTipI18nKey(t *testing.T) {
	type args struct {
		key string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "title",
			args: args{key: "title"},
			want: i18nKeyPrefix + "title",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := genTipI18nKey(tt.args.key); got != tt.want {
				t.Errorf("genTipI18nKey() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_genNormalTipLine(t *testing.T) {
	sdk := cptype.SDK{Tran: &i18n.NopTranslator{}}
	ctx := context.WithValue(context.Background(), cptype.GlobalInnerKeyCtxSDK, &sdk)
	line := genNormalTipLine(ctx, "test-key")
	assert.Equal(t, paddingLeftNormal, line.Style.PaddingLeft)
	assert.Empty(t, fontWeightNormal)
	assert.Equal(t, genTipI18nKey("test-key"), line.Text)
}

func Test_genBoldTipLine(t *testing.T) {
	sdk := cptype.SDK{Tran: &i18n.NopTranslator{}}
	ctx := context.WithValue(context.Background(), cptype.GlobalInnerKeyCtxSDK, &sdk)
	line := genBoldTipLine(ctx, "test-key")
	assert.Equal(t, paddingLeftBold, line.Style.PaddingLeft)
	assert.Equal(t, fontWeightBold, line.Style.FontWeight)
	assert.Equal(t, genTipI18nKey("test-key"), line.Text)
}
