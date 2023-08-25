//go:build !linux
// +build !linux

/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package system

import (
	"fmt"
)

func ProcCmdLine(procRoot string, pid int) ([]string, error) {
	return []string{}, fmt.Errorf("only support linux")
}

var PidOf = pidOfFn

func pidOfFn(procRoot string, name string) ([]int, error) {
	return []int{}, fmt.Errorf("only support linux")
}

var ExecCmdOnHost = execCmdOnHostFn

func execCmdOnHostFn(cmds []string) ([]byte, int, error) {
	return nil, -1, fmt.Errorf("only support linux")
}

func WorkingDirOf(pid int) (string, error) {
	return "", fmt.Errorf("only support linux")
}
