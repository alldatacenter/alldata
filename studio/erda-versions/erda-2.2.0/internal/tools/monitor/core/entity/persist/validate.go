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

package persist

import "github.com/erda-project/erda-proto-go/oap/entity/pb"

// Validator .
type Validator interface {
	Validate(m *pb.Entity) error
}

type nopValidator struct{}

func (*nopValidator) Validate(*pb.Entity) error { return nil }

// NopValidator .
var NopValidator Validator = &nopValidator{}

func newValidator(cfg *config) Validator {
	return &validator{}
}

type validator struct {
}

func (v *validator) Validate(e *pb.Entity) error {
	// TODO: check something
	return nil
}
