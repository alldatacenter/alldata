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

package istioctl

import (
	"context"

	"github.com/sirupsen/logrus"
	"istio.io/client-go/pkg/clientset/versioned"

	"github.com/erda-project/erda/apistructs"
)

type ExecResult int

const (
	ExecSuccess ExecResult = iota
	ExecSkip
	ExecComplete
	ExecError
)

var EmptyEngine IstioEngine

type ServiceOp string

type IstioEngine interface {
	OnServiceOperator(ServiceOp, *apistructs.Service) error
}

const (
	ServiceCreate ServiceOp = "create"
	ServiceUpdate           = "update"
	ServiceDelete           = "delete"
)

type IstioExecutor interface {
	GetName() string
	SetIstioClient(versioned.Interface)
	OnServiceCreate(context.Context, *apistructs.Service) (ExecResult, error)
	OnServiceUpdate(context.Context, *apistructs.Service) (ExecResult, error)
	OnServiceDelete(context.Context, *apistructs.Service) (ExecResult, error)
}

type DefaultEngine struct {
	executors []IstioExecutor
	ctx       context.Context
}

func NewDefaultEngine(executors ...IstioExecutor) DefaultEngine {
	ctx := context.Background()
	return DefaultEngine{
		executors: executors,
		ctx:       ctx,
	}
}

// OnServiceOperator
func (engine DefaultEngine) OnServiceOperator(op ServiceOp, svc *apistructs.Service) error {
	for _, executor := range engine.executors {
		var result ExecResult
		var err error
		switch op {
		case ServiceCreate:
			if svc.MeshEnable != nil && *svc.MeshEnable {
				result, err = executor.OnServiceCreate(engine.ctx, svc)
			}
		case ServiceUpdate:
			if svc.MeshEnable != nil && *svc.MeshEnable {
				result, err = executor.OnServiceUpdate(engine.ctx, svc)
			} else {
				// 关闭了 service mesh
				result, err = executor.OnServiceDelete(engine.ctx, svc)
			}
		case ServiceDelete:
			// 总是清理，允许 Not Found
			result, err = executor.OnServiceDelete(engine.ctx, svc)
		}
		if err != nil {
			logrus.Errorf("op:%s, svc:%s, error happened: %+v", op, svc.Name, err)
		}
		switch result {
		case ExecComplete:
			return nil
		case ExecError:
			return err
		case ExecSkip:
			logrus.Errorf("istio executor:%s skiped", executor.GetName())
		}
	}
	return nil
}
