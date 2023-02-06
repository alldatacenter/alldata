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

package types

import (
	"context"
	"fmt"
	"regexp"
	"strings"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

type ActionExecutor interface {
	Kind() Kind
	Name() Name

	// Exist 返回 created, started, error
	Exist(ctx context.Context, action *spec.PipelineTask) (created bool, started bool, err error)
	// Create 保证幂等
	Create(ctx context.Context, action *spec.PipelineTask) (interface{}, error)
	// Start 保证幂等
	Start(ctx context.Context, action *spec.PipelineTask) (interface{}, error)
	Update(ctx context.Context, action *spec.PipelineTask) (interface{}, error)

	// Status 只做简单重试
	Status(ctx context.Context, action *spec.PipelineTask) (apistructs.PipelineStatusDesc, error)
	Inspect(ctx context.Context, action *spec.PipelineTask) (apistructs.TaskInspect, error)

	Cancel(ctx context.Context, action *spec.PipelineTask) (interface{}, error)
	Remove(ctx context.Context, action *spec.PipelineTask) (interface{}, error)
	BatchDelete(ctx context.Context, actions []*spec.PipelineTask) (interface{}, error)
}

const kindNameFormat = `^[A-Z0-9]+$`

var formatter = regexp.MustCompile(kindNameFormat)

// Name represents an executor's name.
type Name string

func (s Name) String() string {
	return string(s)
}

func (s Name) Validate() bool {
	return formatter.MatchString(string(s))
}

// Kind represents an executor's type.
type Kind string

func (s Kind) String() string {
	return string(s)
}

func (s Kind) Validate() bool {
	return formatter.MatchString(string(s))
}

func (s Kind) IsK8sKind() bool {
	switch s {
	case Kind(spec.PipelineTaskExecutorKindK8sJob), Kind(spec.PipelineTaskExecutorKindK8sFlink), Kind(spec.PipelineTaskExecutorKindK8sSpark):
		return true
	}
	return false
}

func (s Kind) MakeK8sKindExecutorName(clusterName string) Name {
	switch s {
	case Kind(spec.PipelineTaskExecutorKindK8sJob):
		return Name(fmt.Sprintf("%s-%s", spec.PipelineTaskExecutorNameK8sJobDefault, clusterName))
	case Kind(spec.PipelineTaskExecutorKindK8sFlink):
		return Name(fmt.Sprintf("%s-%s", spec.PipelineTaskExecutorNameK8sFlinkDefault, clusterName))
	case Kind(spec.PipelineTaskExecutorKindK8sSpark):
		return Name(fmt.Sprintf("%s-%s", spec.PipelineTaskExecutorNameK8sSparkDefault, clusterName))
	default:
		return Name(fmt.Sprintf("%s-%s", spec.PipelineTaskExecutorNameK8sJobDefault, clusterName))
	}
}

func (s Kind) GetClusterNameByExecutorName(executorName Name) (string, error) {
	switch s {
	case Kind(spec.PipelineTaskExecutorKindK8sJob):
		clusterName := strings.TrimPrefix(executorName.String(), fmt.Sprintf("%s-", spec.PipelineTaskExecutorNameK8sJobDefault))
		if clusterName == "" {
			return "", errors.Errorf("invalid executor name %s", executorName)
		}
		return clusterName, nil
	case Kind(spec.PipelineTaskExecutorKindK8sFlink):
		clusterName := strings.TrimPrefix(executorName.String(), fmt.Sprintf("%s-", spec.PipelineTaskExecutorNameK8sFlinkDefault))
		if clusterName == "" {
			return "", errors.Errorf("invalid executor name %s", executorName)
		}
		return clusterName, nil
	case Kind(spec.PipelineTaskExecutorKindK8sSpark):
		clusterName := strings.TrimPrefix(executorName.String(), fmt.Sprintf("%s-", spec.PipelineTaskExecutorNameK8sSparkDefault))
		if clusterName == "" {
			return "", errors.Errorf("invalid executor name %s", executorName)
		}
		return clusterName, nil
	default:
		return "", errors.New("invalid executor name")
	}
}

// Create be used to create an action executor instance.
type CreateFn func(name Name, options map[string]string) (ActionExecutor, error)

var Factory = map[Kind]CreateFn{}

// Register add an executor's create function.
func Register(kind Kind, create CreateFn) error {
	if !kind.Validate() {
		return errors.Errorf("invalid kind: %s", kind)
	}
	if _, ok := Factory[kind]; ok {
		return errors.Errorf("duplicate to register action executor: %s", kind)
	}
	Factory[kind] = create
	return nil
}

// MustRegister panic if register failed.
func MustRegister(kind Kind, create CreateFn) {
	err := Register(kind, create)
	if err != nil {
		panic(fmt.Errorf("failed to register action executor, kind: %s, err: %v", kind, err))
	}
}
