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

package executor

import (
	"context"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/executortypes"
)

type Empty struct {
	ExecutorName executortypes.Name `json:"name,omitempty"`
	ExecutorKind executortypes.Kind `json:"kind,omitempty"`
}

func (e *Empty) Kind() executortypes.Kind {
	logrus.Infof("Kind on empty")
	return e.ExecutorKind
}

func (e *Empty) Name() executortypes.Name {
	logrus.Infof("Name on empty")
	return e.ExecutorName
}

func (e *Empty) Create(ctx context.Context, specObj interface{}) (interface{}, error) {
	logrus.Infof("Create: %v", specObj)
	return nil, nil
}

func (e *Empty) Destroy(ctx context.Context, spec interface{}) error {
	logrus.Infof("Destroy on empty")
	return nil
}

func (e *Empty) Status(ctx context.Context, specObj interface{}) (apistructs.StatusDesc, error) {
	logrus.Infof("Status on empty")
	return apistructs.StatusDesc{}, nil
}

func (e *Empty) Remove(ctx context.Context, spec interface{}) error {
	logrus.Infof("Remove on empty")
	return nil
}

func (e *Empty) Update(ctx context.Context, specObj interface{}) (interface{}, error) {
	logrus.Infof("Update: %v", specObj)
	return nil, nil
}

func (e *Empty) Inspect(ctx context.Context, spec interface{}) (interface{}, error) {
	logrus.Infof("Inspect on empty")
	return nil, nil
}

func (e *Empty) Cancel(ctx context.Context, spec interface{}) (interface{}, error) {
	logrus.Infof("Cancel on empty")
	return nil, nil
}
