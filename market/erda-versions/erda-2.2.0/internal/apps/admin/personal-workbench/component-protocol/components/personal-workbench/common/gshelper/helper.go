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

package gshelper

import (
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/components/personal-workbench/common"
)

type GSHelper struct {
	gs *cptype.GlobalStateData
}

func NewGSHelper(gs *cptype.GlobalStateData) *GSHelper {
	return &GSHelper{gs: gs}
}

func (h *GSHelper) SetWorkbenchItemType(wbType string) {
	if h.gs == nil {
		return
	}
	(*h.gs)[common.WorkTabKey] = wbType
}

func (h *GSHelper) GetWorkbenchItemType() (apistructs.WorkbenchItemType, bool) {
	if h.gs == nil {
		return "", false
	}
	v, ok := (*h.gs)[common.WorkTabKey]
	if !ok {
		logrus.Warnf("cat not get workTabKey, set default type [project]")
		return apistructs.WorkbenchItemProj, false
	}
	var wbType apistructs.WorkbenchItemType
	cputil.MustObjJSONTransfer(v, &wbType)
	return wbType, true
}

func (h *GSHelper) SetFilterName(name string) {
	if h.gs == nil {
		return
	}
	(*h.gs)[common.FilterNameKey] = name
}

func (h *GSHelper) GetFilterName() (string, bool) {
	if h.gs == nil {
		return "", false
	}
	v, ok := (*h.gs)[common.FilterNameKey]
	if !ok {
		return "", false
	}
	var name string
	cputil.MustObjJSONTransfer(v, &name)
	return name, true
}

func (h *GSHelper) SetMsgTabName(name string) {
	if h.gs == nil {
		return
	}
	(*h.gs)[common.MsgTabKey] = name
}

func (h *GSHelper) GetMsgTabName() (apistructs.WorkbenchItemType, bool) {
	if h.gs == nil {
		return "", false
	}
	v, ok := (*h.gs)[common.MsgTabKey]
	if !ok {
		logrus.Warnf("cat not get messageTabKey, set default type [unreadMessages]")
		return apistructs.WorkbenchItemUnreadMes, false
	}
	var name apistructs.WorkbenchItemType
	cputil.MustObjJSONTransfer(v, &name)
	return name, true
}
