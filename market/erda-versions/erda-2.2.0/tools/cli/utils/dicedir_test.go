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

package utils

import (
	"fmt"
	"os"
	"os/user"
	"path/filepath"
	"testing"

	"github.com/stretchr/testify/assert"
)

// func TestFindGlobalDiceDir(t *testing.T) {
// 	u, _ := user.Current()
// 	assert.Nil(t, os.Mkdir(u.HomeDir+"/.dice.d", 0666))
// 	defer func() { os.Remove(u.HomeDir + "/.dice.d") }()

// 	_, err := FindGlobalDiceDir()
// 	assert.Nil(t, err)
// }

func TestFindProjectDiceDir(t *testing.T) {
	u, _ := user.Current()
	assert.Nil(t, os.Mkdir(u.HomeDir+"/.dice", 0666))
	defer func() { os.Remove(u.HomeDir + "/.dice") }()

	curr, err := os.Getwd()
	upper := filepath.Dir(curr)
	assert.Nil(t, err)
	assert.Nil(t, os.Mkdir(upper+"/.dice", 0666))
	defer func() { os.Remove(upper + "/.dice") }()

	s, err := FindProjectDiceDir()
	assert.Nil(t, err)
	fmt.Printf("%+v\n", s) // debug print

}
