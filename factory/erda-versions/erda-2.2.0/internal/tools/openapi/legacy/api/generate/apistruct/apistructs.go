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

package apistruct

import (
	"fmt"

	"github.com/erda-project/erda/pkg/structparser"
	"github.com/erda-project/erda/pkg/strutil"
)

var (
	// 特殊的类型的 swagger 属性对应表
	specialTypes = map[string]object{
		"time.Time": {
			Typ:    "string",
			Format: "date-time",
		},
	}
)

type context struct {
	request bool
}

// requestParam 用于生成 request 参数
type requestParam struct {
	Name     string `json:"name,omitempty"`
	In       string `json:"in,omitempty"`
	Desc     string `json:"description,omitempty"`
	Required bool   `json:"required,omitempty"`
	Typ      string `json:"type,omitempty"`

	// 只有 Typ = array 时存在 Items
	Items  *object `json:"items,omitempty"`
	Format string  `json:"format,omitempty"`

	// 只有 Typ = object 时存在 Schema
	Schema *object `json:"schema,omitempty"`
}

// responseParam 用于生成 response 参数
type responseParam object

type object struct {

	// Name 当前 object 对应的 key
	Name   string `json:"-"`
	Typ    string `json:"type"`
	Format string `json:"format,omitempty"`

	// 只有 Typ = object 时存在 Properties
	Properties map[string]object `json:"properties,omitempty"`
	Desc       string            `json:"description,omitempty"`

	// 只有 Typ = array 时存在 Items
	Items *object `json:"items,omitempty"`

	// 当前 object 所属的 node
	node structparser.Node `json:"-"`
}

type walkInfo = object

func structToParam(ctx context, tp interface{}) ([]requestParam, *responseParam) {
	walkfunc := func(curr structparser.Node, children []structparser.Node) {
		if s, ok := specialTypes[curr.TypeName()]; ok {
			extra := curr.Extra()
			// found in specialTypes, skip recursively reducing children
			s.Name = name(curr)
			s.Desc = desc(curr)
			s.node = curr
			*extra = s
			return
		}
		switch v := curr.(type) {
		case *structparser.BoolNode:
			extra := v.Extra()
			*extra = object{
				Name: name(v),
				Desc: desc(v),
				Typ:  "boolean",
				node: curr,
			}
		case *structparser.IntNode:
			extra := v.Extra()
			*extra = object{
				Name:   name(v),
				Desc:   desc(v),
				Typ:    "integer",
				Format: "int32",
				node:   curr,
			}
		case *structparser.Int64Node:
			extra := v.Extra()
			*extra = object{
				Name:   name(v),
				Desc:   desc(v),
				Typ:    "integer",
				Format: "int64",
				node:   curr,
			}
		case *structparser.FloatNode:
			extra := v.Extra()
			*extra = object{
				Name:   name(v),
				Desc:   desc(v),
				Typ:    "number",
				Format: "float",
				node:   curr,
			}
		case *structparser.StringNode:
			extra := v.Extra()
			*extra = object{
				Name: name(v),
				Desc: desc(v),
				Typ:  "string",
				node: curr,
			}
		case *structparser.SliceNode:
			child := walkInfo{}
			if len(children) > 0 {
				child = (*(children[0].Extra())).(walkInfo)
			}
			extra := v.Extra()
			*extra = object{
				Name:  name(v),
				Desc:  desc(v),
				Typ:   "array",
				Items: &child,
				node:  curr,
			}
		case *structparser.MapNode:
			child := walkInfo{}
			if len(children) > 0 {
				child = (*(children[1].Extra())).(walkInfo)
			}
			extra := v.Extra()
			*extra = object{
				Name:       name(v),
				Desc:       desc(v),
				Typ:        "object",
				Properties: map[string]object{"additionalProperties": child},
				node:       curr,
			}
		case *structparser.StructNode:
			properties := map[string]object{}
			var info walkInfo
			for _, child := range children {
				if ignore(child) {
					continue
				}
				info = (*(child.Extra())).(walkInfo)
				properties[info.Name] = info
			}
			extra := v.Extra()
			*extra = object{
				Name:       name(v),
				Desc:       desc(v),
				Typ:        "object",
				Properties: properties,
				node:       curr,
			}
		default:
			panic(fmt.Sprintf("unreachable: node: %v", curr))
		}
	}
	node := structparser.Parse(tp)
	node = node.Compress()
	structparser.BottomUpWalk(node, walkfunc)

	walkinfo, ok := (*node.Extra()).(walkInfo)
	if !ok {
		panic("unreachable")
	}

	if ctx.request {
		requestParams := []requestParam{}
		if walkinfo.Typ != "object" {
			panic("unreachable")
		}
		for i := range walkinfo.Properties {
			properties := walkinfo.Properties[i]
			requestParams = append(requestParams, requestParam{
				Name:     properties.Name,
				In:       requesttype(properties.node),
				Desc:     properties.Desc,
				Required: required(properties.node),
				Typ:      properties.Typ,
				Items: map[bool]*object{
					true:  properties.Items,
					false: nil}[properties.Typ == "array"],
				Schema: &properties,
				Format: properties.Format,
			})

		}
		return clearRequestFields(collectRequestBody(requestParams)), nil
	}
	resp := responseParam(walkinfo)
	return nil, &resp
}

func clearRequestFields(reqparams []requestParam) []requestParam {
	newReqParams := []requestParam{}
	for i := range reqparams {
		newReqParams = append(newReqParams, requestParam{
			Name:     reqparams[i].Name,
			In:       reqparams[i].In,
			Desc:     reqparams[i].Desc,
			Required: reqparams[i].Required,
			Typ:      map[bool]string{true: reqparams[i].Typ, false: ""}[reqparams[i].Typ != "object"],
			Items:    map[bool]*object{true: reqparams[i].Items, false: nil}[reqparams[i].Typ == "array"],
			Format:   reqparams[i].Format,
			Schema:   map[bool]*object{true: reqparams[i].Schema, false: nil}[reqparams[i].Typ == "object"],
		})
	}
	return newReqParams
}

// collectRequestBody 将 request 结构顶层 body field 收集在同一个 field 内
func collectRequestBody(reqparams []requestParam) []requestParam {
	bodies := map[string]object{}
	newReqParams := []requestParam{}
	for i := range reqparams {
		if reqparams[i].In != "body" {
			newReqParams = append(newReqParams, reqparams[i])
			continue
		}
		var properties map[string]object
		if reqparams[i].Typ == "object" {
			properties = reqparams[i].Schema.Properties
		}
		bodies[reqparams[i].Name] = object{
			Name:       reqparams[i].Name,
			Typ:        reqparams[i].Typ,
			Format:     reqparams[i].Format,
			Properties: properties,
			Desc:       reqparams[i].Desc,
			Items:      reqparams[i].Items,
		}
	}
	if len(bodies) <= 1 {
		return reqparams
	}
	newReqParams = append(newReqParams, requestParam{
		Name: "body",
		In:   "body",
		Desc: "Body",
		Schema: &object{
			Typ:        "object",
			Properties: bodies,
		},
		Typ: "object",
	})
	return newReqParams
}

func required(n structparser.Node) bool {
	_, ok := n.Tag().Lookup("path")
	return ok
}

func requesttype(n structparser.Node) string {
	if _, ok := n.Tag().Lookup("path"); ok {
		return "path"
	}
	if _, ok := n.Tag().Lookup("query"); ok {
		return "query"
	}
	return "body"
}

func name(n structparser.Node) string {
	tag := n.Tag()
	name, ok := tag.Lookup("path")
	if ok {
		return name
	}
	name, ok = tag.Lookup("query")
	if ok {
		return name
	}
	name, ok = tag.Lookup("json")
	if ok {
		// json:"xxx,omitempty", omit `,omitempty`
		trimed := strutil.Trim(strutil.Split(name, ",")[0])
		if trimed == "-" {
			return ""
		}
		return trimed
	}
	return n.Name()
}
func ignore(n structparser.Node) bool {
	return name(n) == ""
}

func desc(n structparser.Node) string {
	if n.Comment() != "" {
		return n.Comment()
	}
	return n.Tag().Get("desc")
}
