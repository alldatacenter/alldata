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

package monitor

import "strconv"

const _InfoType_name = "EtcdInputEtcdInputDropHTTPInputDINGDINGOutputDINGDINGWorkNoticeOutputMYSQLOutputHTTPOutputLastType"

var _InfoType_index = [...]uint8{0, 9, 22, 31, 45, 69, 80, 90, 98}

func (i InfoType) String() string {
	if i < 0 || i >= InfoType(len(_InfoType_index)-1) {
		return "InfoType(" + strconv.FormatInt(int64(i), 10) + ")"
	}
	return _InfoType_name[_InfoType_index[i]:_InfoType_index[i+1]]
}
