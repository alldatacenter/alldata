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

package batchOperationTipModal

import (
	"context"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/bundle"
)

//batchOperationTipModal: {
//        type: 'Modal',
//        props: {
//          status: 'warning',
//          content: '您将解冻以下节点：\n cn-hongkong.172.15.3.3 \ncn-hongkong.172.15.3.3',
//          title: '提示',
//        },
//        state: { visible: true },
//        operations: {
//          onOk: {
//            key: 'submit',
//            reload: true,
//            meta: {
//              type: 'uncordon',
//            },
//          },
//        },
//      },
type BatchOperationTipModal struct {
	Type       string                 `json:"type"`
	Props      Props                  `json:"props"`
	State      State                  `json:"state"`
	Operations map[string]interface{} `json:"operations"`
	SDK        *cptype.SDK
	CtxBdl     *bundle.Bundle
	ctx        context.Context
}

type Props struct {
	Status  string `json:"status"`
	Content string `json:"content"`
	Title   string `json:"title"`
}

type State struct {
	Visible         bool     `json:"visible"`
	SelectedRowKeys []string `json:"selectedRowKeys"`
}

type Operation struct {
	Key        string `json:"key"`
	Reload     bool   `json:"reload"`
	Meta       Meta   `json:"meta"`
	SuccessMsg string `json:"successMsg"`
}

type Meta struct {
	Type cptype.OperationKey `json:"type"`
}
