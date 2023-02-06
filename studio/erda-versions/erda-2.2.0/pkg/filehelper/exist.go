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

package filehelper

import (
	"fmt"
	"os"

	"github.com/pkg/errors"
)

// CheckExist please check error is nil or not
func CheckExist(path string, needDir bool) error {
	f, err := os.Stat(path)
	if err != nil {
		if os.IsNotExist(err) {
			return errors.Wrap(err, fmt.Sprintf("%s not exist", path))
		}
		return errors.Wrap(err,
			fmt.Sprintf("%s does exist, but throw other error when checking", path))
	}
	if needDir {
		if f.IsDir() {
			return nil
		} else {
			return errors.Errorf("%s exist, but is not a directory", path)
		}
	}
	return nil
}
