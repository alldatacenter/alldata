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
	"io/ioutil"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/time/readable_time"
)

// shortID for containerID
func ShortID(id string) string {
	if len(id) <= 12 {
		return id
	}
	return id[:12]
}

func Time(t interface{}) string {
	var p time.Time
	var err error
	switch v := t.(type) {
	case string:
		p, err = time.Parse(time.RFC3339, v)
		if err != nil {
			p, err = time.Parse("2006-01-02T15:04:05Z0700", v)
			if err != nil {
				return v
			}
		}
	case time.Time:
		p = v
	case *time.Time:
		p = *v
	default:
		return fmt.Sprintf("%v", t)
	}

	return readable_time.Readable(p).String()
}

func ToTimeSpanString(seconds int) string {
	hours := seconds / 3600
	seconds -= hours * 3600
	minutes := seconds / 60
	seconds -= minutes * 60

	return fmt.Sprintf("%02d:%02d:%02d", hours, minutes, seconds)
}

func FormatErrMsg(name string, str string, withHelp bool) string {
	str = strings.TrimSuffix(str, ".") + "."

	if withHelp {
		str += "\nSee '" + os.Args[0] + " " + name + " --help'."
	}

	return fmt.Sprintf("%s: %s\n", os.Args[0], str)
}

func ReadYml(path string) ([]byte, error) {
	f, err := os.Open(path)
	if err != nil {
		return nil, fmt.Errorf("failed to open %s", path)
	}
	content, err := ioutil.ReadAll(f)
	if err != nil {
		return nil, fmt.Errorf("failed to read file %s", path)
	}
	return content, nil
}

func FormatMemOutput(mem uint64) string {
	if mem/(1024*1024*1024) > 0 {
		return strutil.Concat(fmt.Sprintf("%.2f", float64(mem/(1024*1024))/1024), "GiB")
	}

	return strutil.Concat(strconv.FormatUint(mem/(1024*1024), 10), "MiB")
}
