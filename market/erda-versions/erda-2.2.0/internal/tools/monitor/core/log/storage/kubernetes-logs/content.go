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

package kuberneteslogs

import (
	"bufio"
	"io"
	"math"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/erda-project/erda-proto-go/core/monitor/log/query/pb"
)

func parseLines(reader io.ReadCloser, process func(line []byte) error) (err error) {
	r := bufio.NewReader(reader)
	for {
		var line []byte
		for {
			l, prefix, err := r.ReadLine()
			if err != nil {
				if err == io.ErrUnexpectedEOF {
					return io.EOF
				}
				return err
			}
			if prefix {
				line = append(line, l...)
				continue
			}
			if line == nil {
				line = l
			} else {
				line = append(line, l...)
			}
			break
		}
		err := process(line)
		if err != nil {
			return err
		}
	}
}

func parseLine(line string, it *logsIterator) (*pb.LogItem, string) {
	var ts int64
	idx := strings.Index(line, " ")
	if idx > 0 {
		t, err := time.Parse(time.RFC3339Nano, string(line[:idx]))
		if err == nil {
			line = line[idx+1:]
			ts = t.UnixNano()
		}
	}
	data := &pb.LogItem{
		Id:        it.id,
		Timestamp: strconv.FormatInt(ts, 10),
		UnixNano:  ts,
		Source:    "container",
		Offset:    math.MaxInt64,
	}
	return data, line
}

var contentRegex = regexp.MustCompile("(?P<timedate>^\\d{4}-\\d{2}-\\d{2} \\d{1,2}:\\d{1,2}:\\d{1,2}(\\.\\d+)*)\\s+(?P<log_level>[Aa]lert|ALERT|[Tt]race|TRACE|[Dd]ebug|DEBUG|[Nn]otice|NOTICE|[Ii]nfo|INFO|[Ww]arn(?:ing)?|WARN(?:ING)?|[Ee]rr(?:or)?|ERR(?:OR)?|[Cc]rit(?:ical)?|CRIT(?:ICAL)?|[Ff]atal|FATAL|[Ss]evere|SEVERE|[Ee]merg(?:ency)?|EMERG(?:ENCY))\\s+\\[(?P<ext_info>.*?)\\](?P<content>[\\s\\S]*$)")

func parseContent(text string, data *pb.LogItem) {
	content, tags := parseContentTags(text)
	data.Content = content
	data.RequestId = tags["request_id"]
	data.Level = tags["level"]
}

func parseContentTags(message string) (string, map[string]string) {
	groupNames := contentRegex.SubexpNames()
	level, tagstr := "", ""
	for _, matches := range contentRegex.FindAllStringSubmatch(message, -1) {
		for idx, name := range groupNames {
			switch name {
			case "log_level":
				level = matches[idx]
			case "ext_info":
				tagstr = matches[idx]
			}
		}
	}
	newTagstr, tags := extractTags(tagstr)
	if level != "" {
		tags["level"] = level
	}
	return strings.ReplaceAll(message, tagstr, newTagstr), tags
}

func extractTags(raw string) (tagstr string, tags map[string]string) {
	tagstr, tags = "", make(map[string]string)
	for idx, item := range strings.Split(raw, ",") {
		tmp := strings.Split(item, "=")
		if len(tmp) == 2 {
			tags[tmp[0]] = tmp[1]
			continue
		}
		switch idx {
		case 1:
			tags["request-id"] = item
		}
		if tagstr == "" {
			tagstr = item
		} else {
			tagstr += "," + item
		}
	}
	return
}
