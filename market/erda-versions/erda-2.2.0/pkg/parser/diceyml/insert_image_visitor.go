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

package diceyml

import (
	"fmt"

	"github.com/pkg/errors"
)

type InsertImageVisitor struct {
	DefaultVisitor
	collectError error
	images       map[string]string
	envs         map[string]map[string]string
}

func NewInsertImageVisitor(images map[string]string, envs map[string]map[string]string) DiceYmlVisitor {
	return &InsertImageVisitor{images: images, envs: envs}
}

func (o *InsertImageVisitor) VisitServices(v DiceYmlVisitor, obj *Services) {
	if len(*obj) == 0 {
		return
	}
	o.DefaultVisitor.VisitServices(v, obj)
	if len(o.images) > 0 {
		images := fmt.Sprintf("%v", o.images)
		o.collectError = errors.Wrap(notfoundService, images)
	}
}

func (o *InsertImageVisitor) VisitService(v DiceYmlVisitor, obj *Service) {
	image, ok := o.images[o.currentService]
	if !ok {
		return
	}
	obj.Image = image
	if tmpenvs, ok := o.envs[o.currentService]; ok && len(tmpenvs) != 0 {
		if obj.Envs == nil {
			obj.Envs = make(map[string]string)
		}
		for k, v := range tmpenvs {
			obj.Envs[k] = v
		}
	}
	delete(o.images, o.currentService)
}

func (o *InsertImageVisitor) VisitJobs(v DiceYmlVisitor, obj *Jobs) {
	if len(*obj) == 0 {
		return
	}
	o.DefaultVisitor.VisitJobs(v, obj)

	if len(o.images) > 0 {
		images := fmt.Sprintf("%v", o.images)
		o.collectError = errors.Wrap(notfoundJob, images)
	}
}

func (o *InsertImageVisitor) VisitJob(v DiceYmlVisitor, obj *Job) {
	image, ok := o.images[o.currentJob]
	if !ok {
		return
	}
	obj.Image = image

	delete(o.images, o.currentJob)
}

func InsertImage(obj *Object, images map[string]string, envs map[string]map[string]string) error {
	visitor := NewInsertImageVisitor(images, envs)
	obj.Accept(visitor)
	return visitor.(*InsertImageVisitor).collectError
}
