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

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	messenger "github.com/erda-project/erda-proto-go/core/messenger/notify/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/common"
)

func SetNotifyIndexToGlobalState(gs cptype.GlobalStateData, alertIndex *messenger.AlertNotifyDetail) {
	gs[AlertIndex] = alertIndex
	gs[StateKeyPageTitle] = alertIndex.AlertName
}

func GetNotifyIndexFromGlobalState(gs cptype.GlobalStateData) *messenger.AlertNotifyDetail {
	item, ok := gs[AlertIndex]
	if !ok {
		return nil
	}
	typedItem, ok := item.(*messenger.AlertNotifyDetail)
	if !ok {
		return nil
	}
	return typedItem
}

func GetMessengerServiceFromContext(ctx context.Context) messenger.NotifyServiceServer {
	val := ctx.Value(common.ContextKeyServiceMessengerService)
	if val == nil {
		return nil
	}

	typed, ok := val.(messenger.NotifyServiceServer)
	if !ok {
		return nil
	}
	return typed
}

func GetCoreServiceUrlFromContext(ctx context.Context) *bundle.Bundle {
	val := ctx.Value(common.ContextKeyCoreServicesUrl)
	if val == nil {
		return nil
	}

	typed, ok := val.(*bundle.Bundle)
	if !ok {
		return nil
	}
	return typed
}
