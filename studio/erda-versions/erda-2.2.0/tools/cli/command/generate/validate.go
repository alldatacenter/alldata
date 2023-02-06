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

package main

import (
	"github.com/pkg/errors"

	"github.com/erda-project/erda/tools/cli/command"
)

var (
	ErrName                = errors.New("not provide Name")
	ErrOptionalArgPosition = errors.New("optional Arg should be the last arg")
	ErrOptionalArgNum      = errors.New("too many optional arg, support only 1 optional arg yet")
)

func validate(cmd command.Command, fname string) error {
	if cmd.Name == "" {
		return errors.Wrap(ErrName, fname)
	}

	optionalArgNum := 0
	for _, arg := range cmd.Args {
		if arg.IsOption() {
			optionalArgNum++
		} else {
			if optionalArgNum > 0 {
				return errors.Wrap(ErrOptionalArgPosition, fname)
			}
		}
	}
	if optionalArgNum > 1 {
		return errors.Wrap(ErrOptionalArgNum, fname)
	}
	return nil
}
