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

package model

import (
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model/odata"
)

type ObservableDataConsumerFunc func(data odata.ObservableData)

type RuntimeReceiver struct {
	Name     string
	Receiver Receiver
	Filter   *DataFilter
}

type Receiver interface {
	Component
	// TODO
	RegisterConsumer(consumer ObservableDataConsumerFunc)
}

type NoopReceiver struct {
}

func (n *NoopReceiver) ComponentConfig() interface{} {
	return nil
}

func (n *NoopReceiver) RegisterConsumer(consumer ObservableDataConsumerFunc) {}
