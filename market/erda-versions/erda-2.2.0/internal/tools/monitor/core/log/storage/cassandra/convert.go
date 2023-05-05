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

package cassandra

import (
	"bytes"
	"compress/gzip"
	"io"
	"strconv"
	"strings"

	"github.com/erda-project/erda-proto-go/core/monitor/log/query/pb"
)

func convertToLogItems(list []*SavedLog, matcher func(data *pb.LogItem) bool) ([]interface{}, error) {
	logs := make([]interface{}, 0, len(list))
	for _, log := range list {
		data, err := wrapToLogItem(log)
		if err != nil {
			return logs, err
		}
		if matcher(data) {
			logs = append(logs, data)
		}
	}
	return logs, nil
}

func wrapToLogItem(sl *SavedLog) (*pb.LogItem, error) {
	content, err := gunzipContent(sl.Content)
	if err != nil {
		return nil, err
	}
	return &pb.LogItem{
		Id:        sl.ID,
		Source:    sl.Source,
		Stream:    sl.Stream,
		Timestamp: strconv.FormatInt(sl.Timestamp, 10),
		UnixNano:  sl.Timestamp,
		Offset:    sl.Offset,
		Content:   content,
		Level:     sl.Level,
		RequestId: sl.RequestID,
	}, nil
}

func gunzipContent(content []byte) (string, error) {
	r, err := gzip.NewReader(bytes.NewReader(content))
	if err != nil {
		return "", err
	}
	defer r.Close()
	sb := &strings.Builder{}
	_, err = io.Copy(sb, r)
	if err != nil {
		return "", err
	}
	return sb.String(), nil
}
