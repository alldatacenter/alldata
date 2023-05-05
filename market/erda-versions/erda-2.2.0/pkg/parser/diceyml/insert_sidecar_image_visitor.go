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

type InsertSideCarImageVisitor struct {
	DefaultVisitor
	collectError error
	images       map[string]map[string]string
}

func NewInsertSideCarImageVisitor(images map[string]map[string]string) DiceYmlVisitor {
	return &InsertSideCarImageVisitor{images: images}
}

func (o *InsertSideCarImageVisitor) VisitServices(v DiceYmlVisitor, obj *Services) {
	if len(*obj) == 0 {
		return
	}
	o.DefaultVisitor.VisitServices(v, obj)
	if len(o.images) > 0 {
		images := fmt.Sprintf("%v", o.images)
		o.collectError = errors.Wrap(notfoundService, images)
	}
}

func (o *InsertSideCarImageVisitor) VisitService(v DiceYmlVisitor, obj *Service) {
	images, ok := o.images[o.currentService]
	if !ok {
		return
	}
	for name := range obj.SideCars {
		image, ok := images[name]
		if !ok {
			continue
		}
		delete(images, name)
		obj.SideCars[name].Image = image
	}
	if len(images) == 0 {
		delete(o.images, o.currentService)
	}
}

// images: map[servicename]map[sidecarname]image
func InsertSideCarImage(obj *Object, images map[string]map[string]string) error {
	visitor := NewInsertSideCarImageVisitor(images)
	obj.Accept(visitor)
	return visitor.(*InsertSideCarImageVisitor).collectError
}
