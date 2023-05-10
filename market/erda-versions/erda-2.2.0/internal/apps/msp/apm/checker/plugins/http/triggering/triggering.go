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

package triggering

import (
	"io/ioutil"
	"net/http"
	"regexp"
	"strings"

	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-proto-go/msp/apm/checker/pb"
	_ "github.com/erda-project/erda-proto-go/msp/apm/checker/pb"
)

const (
	BodyStrategy     string = "body"
	HttpCodeStrategy string = "http_code"
)

type Interface interface {
	Executor(resp *http.Response) bool
	BodyStrategy(resp *http.Response) bool
	HttpCodeStrategy(resp *http.Response) bool
}

type Triggering struct {
	Key     string
	Operate string
	Value   *structpb.Value
}

func New(c *pb.Condition) *Triggering {
	return &Triggering{
		Key:     c.Key,
		Operate: c.Operate,
		Value:   c.Value,
	}
}

func (condition *Triggering) Executor(resp *http.Response) bool {
	switch condition.Key {
	case HttpCodeStrategy:
		return condition.HttpCodeStrategy(resp)
	case BodyStrategy:
		return condition.BodyStrategy(resp)
	default:
		return true
	}
}

func (condition *Triggering) BodyStrategy(resp *http.Response) bool {
	var body string
	if resp.Body != nil {
		bodyBytes, err := ioutil.ReadAll(resp.Body)
		if err != nil {
			return false
		}
		body = string(bodyBytes)
	}
	switch condition.Operate {
	case "contains":
		return !strings.Contains(body, condition.Value.GetStringValue())
	case "not_contains":
		return strings.Contains(body, condition.Value.GetStringValue())
	case "regex":
		re := regexp.MustCompile(condition.Value.GetStringValue())
		return !re.MatchString(body)
	case "not_regex":
		re := regexp.MustCompile(condition.Value.GetStringValue())
		return re.MatchString(body)
	default:
		return true
	}
}

func (condition *Triggering) HttpCodeStrategy(resp *http.Response) bool {
	switch condition.Operate {
	case ">":
		if resp.StatusCode > int(condition.Value.GetNumberValue()) {
			return false
		}
	case ">=":
		if resp.StatusCode >= int(condition.Value.GetNumberValue()) {
			return false
		}
	case "=":
		if resp.StatusCode == int(condition.Value.GetNumberValue()) {
			return false
		}
	case "<":
		if resp.StatusCode < int(condition.Value.GetNumberValue()) {
			return false
		}
	case "<=":
		if resp.StatusCode <= int(condition.Value.GetNumberValue()) {
			return false
		}
	case "!=":
		if resp.StatusCode != int(condition.Value.GetNumberValue()) {
			return false
		}
	default:
		return true
	}
	return true
}
