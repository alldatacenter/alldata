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

package publish_item

import (
	"fmt"
	"sort"
	"testing"

	"github.com/sirupsen/logrus"
)

func TestSlice(t *testing.T) {
	//result, _ := strconv.ParseFloat(fmt.Sprintf("%.2f", (float64(60267-78686)/float64(78686))*100.0), 64)

	recentAvgActiveTime := 60267
	lastAvgActiveTime := 78686
	fmt.Println((float64(60267-78686) / float64(78686)) * 100.0)
	fmt.Println((float64(recentAvgActiveTime-lastAvgActiveTime) / float64(lastAvgActiveTime)) * 100.0)

	logrus.Infof("monitor request params 往前推7天 recentAvgActiveTime: %d,lastAvgActiveTime: %d", recentAvgActiveTime, lastAvgActiveTime)
	logrus.Infof("monitor request params 往前推7天 result: %f", (float64(recentAvgActiveTime-lastAvgActiveTime)/float64(lastAvgActiveTime))*100.0)
	logrus.Infof("monitor request params 往前推7天 resultStr: %s", fmt.Sprintf("%.2f", (float64(recentAvgActiveTime-lastAvgActiveTime)/float64(lastAvgActiveTime))*100.0))

}

func TestMetrics(t *testing.T) {
	strslice := []string{"1.2.3", "1.1.2", "1.1.9", "1.3.1"}

	sort.Sort(sort.StringSlice(strslice))

	fmt.Println(strslice)
}
