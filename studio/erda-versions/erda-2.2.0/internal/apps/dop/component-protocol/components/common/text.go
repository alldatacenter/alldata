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

package common

import (
	"context"
	"strconv"
	"time"

	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
)

func UpdatedTime(ctx context.Context, activeTime time.Time) string {
	var subStr string
	nowTime := time.Now()
	sub := nowTime.Sub(activeTime)
	if int64(sub.Hours()) >= 24*30*12 {
		subStr = strconv.FormatInt(int64(sub.Hours())/(24*30*12), 10) + " " + cputil.I18n(ctx, "yearAgo")
	} else if int64(sub.Hours()) >= 24*30 {
		subStr = strconv.FormatInt(int64(sub.Hours())/(24*30), 10) + " " + cputil.I18n(ctx, "monthAgo")
	} else if int64(sub.Hours()) >= 24 {
		subStr = strconv.FormatInt(int64(sub.Hours())/24, 10) + " " + cputil.I18n(ctx, "dayAgo")
	} else if int64(sub.Hours()) > 0 {
		subStr = strconv.FormatInt(int64(sub.Hours()), 10) + " " + cputil.I18n(ctx, "hourAgo")
	} else if int64(sub.Minutes()) > 0 {
		subStr = strconv.FormatInt(int64(sub.Minutes()), 10) + " " + cputil.I18n(ctx, "minuteAgo")
	} else {
		subStr = cputil.I18n(ctx, "secondAgo")
	}
	return subStr
}
