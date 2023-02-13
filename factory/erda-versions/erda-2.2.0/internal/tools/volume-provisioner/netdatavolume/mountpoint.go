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

package netdatavolume

import (
	"bufio"
	"errors"
	"io"
	"os"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/strutil"
)

var (
	NotFoundErr            = errors.New("not found mountpoint")
	MultipleAlternativeErr = errors.New("multiple alternative mountpoint found")
)

// DiscoverMountPoint auto discover netdata's mountpoint
// How to parse mountInfo please refer to 'http://man7.org/linux/man-pages/man5/proc.5.html'
func DiscoverMountPoint() (string, error) {
	mountInfo, err := os.Open("/proc/self/mountinfo")
	if err != nil {
		return "", err
	}
	reader := bufio.NewReader(mountInfo)
	mountInfoList := make([]string, 0)
	for {
		l, err := reader.ReadString('\n')
		if err != nil {
			if err == io.EOF {
				break
			}
			return "", err
		}
		mountInfoList = append(mountInfoList, l)

	}

	discovered := []string{}

	for _, l := range mountInfoList {
		splited := strutil.Split(l, " ", true)
		if len(splited) < 9 {
			continue
		}
		// splited[8] is fstype
		// Currently only consider fstype to be 'fuse.glusterfs' or 'nfs*'
		if splited[3] == "/" && (strutil.Contains(splited[8], "nfs") ||
			strutil.Contains(splited[8], "fuse.glusterfs")) {
			discovered = append(discovered, splited[4])
		}
	}
	if len(discovered) == 0 {
		return "", NotFoundErr
	}
	if len(discovered) > 1 {
		logrus.Errorf("found multiple alternative: %v", discovered)
		return "", MultipleAlternativeErr
	}
	return discovered[0], nil
}
