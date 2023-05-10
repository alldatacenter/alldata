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

package demo

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/executortypes"
)

func init() {
	executortypes.Register("DEMO", func(name executortypes.Name, clustername string, options map[string]string, optionsPlus interface{}) (executortypes.Executor, error) {
		return &Demo{
			name: name,
		}, nil
	})
}

type Demo struct {
	name executortypes.Name
}

func (d *Demo) Kind() executortypes.Kind {
	return "DEMO"
}

func (d *Demo) Name() executortypes.Name {
	return d.name
}

func (d *Demo) Create(ctx context.Context, specObj interface{}) (interface{}, error) {
	logrus.Infof("demo create ...")

	time.Sleep(time.Second * 5)
	return nil, nil
}

func (d *Demo) Destroy(ctx context.Context, spec interface{}) error {
	logrus.Infof("demo destroy ...")

	time.Sleep(time.Second * 5)
	return nil
}

func (d *Demo) Status(ctx context.Context, specObj interface{}) (apistructs.StatusDesc, error) {
	logrus.Infof("demo status ...")

	time.Sleep(time.Second * 5)
	return apistructs.StatusDesc{
		Status: apistructs.StatusStoppedOnOK,
	}, nil
}

func (d *Demo) Remove(ctx context.Context, spec interface{}) error {
	logrus.Infof("demo remove ...")

	time.Sleep(time.Second * 5)
	return nil
}

func (d *Demo) Update(ctx context.Context, specObj interface{}) (interface{}, error) {
	logrus.Infof("demo update ...")

	time.Sleep(time.Second * 5)
	return nil, nil
}

func (d *Demo) Inspect(ctx context.Context, spec interface{}) (interface{}, error) {
	logrus.Infof("demo inspect ...")

	time.Sleep(time.Second * 5)
	return nil, nil
}

func (d *Demo) Cancel(ctx context.Context, spec interface{}) (interface{}, error) {
	logrus.Infof("demo cancel ...")

	time.Sleep(time.Second * 5)
	return nil, nil
}
func (d *Demo) Precheck(ctx context.Context, specObj interface{}) (apistructs.ServiceGroupPrecheckData, error) {
	return apistructs.ServiceGroupPrecheckData{Status: "ok"}, nil
}

func (d *Demo) SetNodeLabels(setting executortypes.NodeLabelSetting, hosts []string, labels map[string]string) error {
	return errors.New("SetNodeLabels not implemented in Demo")
}

func (d *Demo) CapacityInfo() apistructs.CapacityInfoData {
	return apistructs.CapacityInfoData{}
}
func (d *Demo) ResourceInfo(brief bool) (apistructs.ClusterResourceInfoData, error) {
	return apistructs.ClusterResourceInfoData{}, fmt.Errorf("resourceinfo not support for demo")
}
func (k *Demo) CleanUpBeforeDelete() {}
func (k *Demo) JobVolumeCreate(ctx context.Context, spec interface{}) (string, error) {
	return "", fmt.Errorf("not support for demo")
}
func (*Demo) KillPod(podname string) error {
	return fmt.Errorf("not support for demo")
}

func (*Demo) Scale(ctx context.Context, spec interface{}) (interface{}, error) {
	return apistructs.ServiceGroup{}, fmt.Errorf("scale not support for demo")
}
