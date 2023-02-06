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

package localvolume

import (
	"bufio"
	"errors"
	"io"
	"os"
	"os/exec"
	"sort"
	"strconv"

	"github.com/erda-project/erda/pkg/strutil"
)

var (
	NotFoundErr = errors.New("not found available mount point")
	//           key: MAJ:MIN
	lsblkInfo map[string]lsblk
)

type mountPoint [2]string // mountPoint, MAJ:MIN
type mountPoints []mountPoint

// DiscoverMountPoint auto discover localvolume's mountInfo
// How to parse mountInfo please refer to 'http://man7.org/linux/man-pages/man5/proc.5.html'
func DiscoverMountPoint() (string, error) {
	mountInfo, err := os.Open("/proc/self/mountinfo")
	if err != nil {
		return "", err
	}
	reader := bufio.NewReader(mountInfo)

	var mountInfoList []string
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
	discovered := mountPoints{}

	for _, l := range mountInfoList {
		splited := strutil.Split(l, " ", true)
		if len(splited) < 10 {
			continue
		}
		if strutil.HasPrefixes(splited[9], "/dev/") &&
			splited[4] != "/" &&
			splited[3] == "/" &&
			strutil.Contains(splited[8], "ext4", "ext3", "xfs") {
			discovered = append(discovered, mountPoint{splited[4], splited[2]})
		}
	}
	if len(discovered) == 0 {
		return "", NotFoundErr
	}
	lsblkInfo, err = parseLsblk()
	// 1. max size & not root device
	// 2. fallback: root device
	sort.Sort(discovered)
	return discovered[0][0], nil
}

type lsblk struct {
	maj_min    string
	size       int64
	mountPoint string
}

func parseLsblk() (map[string]lsblk, error) {
	lsblkInfo := map[string]lsblk{}
	cmd := exec.Command("lsblk", "-P", "-b")
	reader, err := cmd.StdoutPipe()
	if err != nil {
		return lsblkInfo, err
	}
	if err := cmd.Start(); err != nil {
		return lsblkInfo, err
	}
	for {
		l, err := bufio.NewReader(reader).ReadString('\n')
		if err != nil {
			if err == io.EOF {
				break
			}
			return lsblkInfo, err
		}
		splited := strutil.Split(l, " ", true)
		lsblk := lsblk{}
		for _, i := range splited {
			kv := strutil.Split(i, "=")
			v := strutil.Trim(kv[1], "\"")
			switch kv[0] {
			case "MAJ:MIN":
				lsblk.maj_min = v
			case "SIZE":
				size, err := strconv.ParseInt(v, 10, 64)
				if err != nil {
					return lsblkInfo, err
				}
				lsblk.size = size
			case "MOUNTPOINT":
				lsblk.mountPoint = v
			}
		}
		lsblkInfo[lsblk.maj_min] = lsblk
	}
	return lsblkInfo, err
}

func (mp mountPoints) Len() int      { return len(mp) }
func (mp mountPoints) Swap(i, j int) { mp[i], mp[j] = mp[j], mp[i] }
func (mp mountPoints) Less(i, j int) bool {
	lsblki, ok := lsblkInfo[mp[i][1]]
	if !ok {
		return false
	}
	lsblkj, ok := lsblkInfo[mp[j][1]]
	if !ok {
		return true
	}
	if lsblki.mountPoint == "/hostfs" {
		return false
	}
	if lsblkj.mountPoint == "/hostfs" {
		return true
	}
	if lsblki.size < lsblkj.size {
		return false
	}
	return true
}
