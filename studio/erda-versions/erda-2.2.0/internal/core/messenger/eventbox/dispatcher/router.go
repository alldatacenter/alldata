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

package dispatcher

import (
	"fmt"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/core/messenger/eventbox/dispatcher/errors"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/dispatcher/filters"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/types"
)

// dispatcher 内部处理 message 的相关逻辑

//               +-------------------------+
//               |     Router              |
//               |                         |
//               |     +-------------+     |      +-------------+
//               |     |             |     |      |  backend    |
// src ----------+---->|      A      +-----+------>  message    |
//               |     |             |     |      |  consumer   |
//               |     +-------------+     |      | e.g.dingding|
//               +-------------------------+      +-------------+
// A: []filter
//
// []filter:
//     +---------------+  +---------------+  +----------------+	 +-----------------+
//     | unifylabels   +--> registerlabel +--> webhookfilter  +-->  lastfilter     |
//     |               |  |               |  |                |	 |                 |
//     +---------------+  +---------------+  +----------------+	 +-----------------+
//
//
type Router struct {
	dispatcher *DispatcherImpl
	filters    []filters.Filter
}

func NewRouter(dispatcher *DispatcherImpl) (*Router, error) {
	r := &Router{
		dispatcher: dispatcher,
		filters:    []filters.Filter{},
	}

	unifyLabelsFilter := filters.NewUnifyLabelsFilter()
	registerFilter := filters.NewRegisterFilter(dispatcher.GetRegister())
	webhookFilter, err := filters.NewWebhookFilter()
	if err != nil {
		return nil, fmt.Errorf("init webhookfilter: %v", err)
	}
	lastFilter := filters.NewLastFilter(dispatcher.GetSubscribersPool(), dispatcher.GetSubscribers())

	r.RegisterFilter(unifyLabelsFilter)
	r.RegisterFilter(registerFilter)
	r.RegisterFilter(webhookFilter)
	r.RegisterFilter(lastFilter)

	return r, nil
}

func (r *Router) RegisterFilter(f filters.Filter) {
	logrus.Infof("Router register filter [%s]", f.Name())
	r.filters = append(r.filters, f)
}

func (r *Router) Route(m *types.Message) *errors.DispatchError {
	for i, f := range r.filters {
		derr := f.Filter(m)
		if derr != nil && !derr.IsOK() {
			if len(r.filters)-1 != i {
				logrus.Warnf("Route: %v", derr)
			}
			return derr
		}
		if derr != nil && derr.FilterInfo != "" {
			logrus.Warnf("Route: FilterInfo: %s", derr.FilterInfo)
		}
	}
	return errors.New()
}

func (r *Router) GetFilters() []filters.Filter {
	return r.filters
}
