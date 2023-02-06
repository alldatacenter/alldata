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
	"bytes"
	"io/ioutil"
	"net/http"
	"reflect"
	"testing"

	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-proto-go/msp/apm/checker/pb"
)

func TestTriggering_HttpCodeStrategy(t *testing.T) {
	type fields struct {
		Key     string
		Operate string
		Value   *structpb.Value
	}
	type args struct {
		resp *http.Response
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   bool
	}{
		{"case > true", fields{Key: "http_code", Operate: ">", Value: structpb.NewNumberValue(200)}, args{resp: &http.Response{StatusCode: 100}}, true},
		{"case > false", fields{Key: "http_code", Operate: ">", Value: structpb.NewNumberValue(200)}, args{resp: &http.Response{StatusCode: 300}}, false},
		{"case >= true", fields{Key: "http_code", Operate: ">=", Value: structpb.NewNumberValue(200)}, args{resp: &http.Response{StatusCode: 100}}, true},
		{"case >= false", fields{Key: "http_code", Operate: ">=", Value: structpb.NewNumberValue(200)}, args{resp: &http.Response{StatusCode: 200}}, false},
		{"case = true", fields{Key: "http_code", Operate: "=", Value: structpb.NewNumberValue(200)}, args{resp: &http.Response{StatusCode: 400}}, true},
		{"case = false", fields{Key: "http_code", Operate: "=", Value: structpb.NewNumberValue(200)}, args{resp: &http.Response{StatusCode: 200}}, false},
		{"case < true", fields{Key: "http_code", Operate: "<", Value: structpb.NewNumberValue(200)}, args{resp: &http.Response{StatusCode: 200}}, true},
		{"case < false", fields{Key: "http_code", Operate: "<", Value: structpb.NewNumberValue(200)}, args{resp: &http.Response{StatusCode: 100}}, false},
		{"case <= true", fields{Key: "http_code", Operate: "<=", Value: structpb.NewNumberValue(200)}, args{resp: &http.Response{StatusCode: 300}}, true},
		{"case <= false", fields{Key: "http_code", Operate: "<=", Value: structpb.NewNumberValue(200)}, args{resp: &http.Response{StatusCode: 200}}, false},
		{"case != true", fields{Key: "http_code", Operate: "!=", Value: structpb.NewNumberValue(200)}, args{resp: &http.Response{StatusCode: 200}}, true},
		{"case != false", fields{Key: "http_code", Operate: "!=", Value: structpb.NewNumberValue(200)}, args{resp: &http.Response{StatusCode: 100}}, false},
		{"case not support", fields{Key: "http_code", Operate: "x", Value: structpb.NewNumberValue(200)}, args{resp: &http.Response{StatusCode: 200}}, true},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			condition := &Triggering{
				Key:     tt.fields.Key,
				Operate: tt.fields.Operate,
				Value:   tt.fields.Value,
			}
			if got := condition.HttpCodeStrategy(tt.args.resp); got != tt.want {
				t.Errorf("HttpCodeStrategy() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestTriggering_BodyStrategy(t *testing.T) {
	body1 := ioutil.NopCloser(bytes.NewReader([]byte("This is a test body, hello world!")))
	body2 := ioutil.NopCloser(bytes.NewReader([]byte("This is a test body, hello world!")))
	body3 := ioutil.NopCloser(bytes.NewReader([]byte("This is a test body, hello world!")))
	body4 := ioutil.NopCloser(bytes.NewReader([]byte("This is a test body, hello world!")))
	body5 := ioutil.NopCloser(bytes.NewReader([]byte("This is a test body, hello world!")))
	body6 := ioutil.NopCloser(bytes.NewReader([]byte("This is a test body, hello world!")))
	body7 := ioutil.NopCloser(bytes.NewReader([]byte("This is a test body, hello world!")))
	body8 := ioutil.NopCloser(bytes.NewReader([]byte("This is a test body, hello world!")))
	type fields struct {
		Key     string
		Operate string
		Value   *structpb.Value
	}
	type args struct {
		resp *http.Response
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   bool
	}{
		{"case contains y", fields{Key: "body", Operate: "contains", Value: structpb.NewStringValue("hello")}, args{resp: &http.Response{Body: body1}}, false},
		{"case contains n", fields{Key: "body", Operate: "contains", Value: structpb.NewStringValue("error")}, args{resp: &http.Response{Body: body2}}, true},
		{"case not_contains y", fields{Key: "body", Operate: "not_contains", Value: structpb.NewStringValue("error")}, args{resp: &http.Response{Body: body3}}, false},
		{"case not_contains n", fields{Key: "body", Operate: "not_contains", Value: structpb.NewStringValue("hello")}, args{resp: &http.Response{Body: body4}}, true},
		{"case regex y", fields{Key: "body", Operate: "regex", Value: structpb.NewStringValue("test.*hello")}, args{resp: &http.Response{Body: body5}}, false},
		{"case regex n", fields{Key: "body", Operate: "regex", Value: structpb.NewStringValue("test.*hallo")}, args{resp: &http.Response{Body: body6}}, true},
		{"case not_regex y", fields{Key: "body", Operate: "not_regex", Value: structpb.NewStringValue("test.*hallo")}, args{resp: &http.Response{Body: body7}}, false},
		{"case not_regex n", fields{Key: "body", Operate: "not_regex", Value: structpb.NewStringValue("test.*hello")}, args{resp: &http.Response{Body: body8}}, true},
		{"case not support", fields{Key: "body", Operate: "x", Value: structpb.NewStringValue("Error")}, args{resp: &http.Response{Body: nil}}, true},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			condition := &Triggering{
				Key:     tt.fields.Key,
				Operate: tt.fields.Operate,
				Value:   tt.fields.Value,
			}
			got := condition.BodyStrategy(tt.args.resp)
			if got != tt.want {
				t.Errorf("BodyStrategy() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestTriggering_Executor(t *testing.T) {
	type fields struct {
		Key     string
		Operate string
		Value   *structpb.Value
	}
	type args struct {
		resp *http.Response
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   bool
	}{
		{"case > true", fields{Key: "http_code", Operate: ">", Value: structpb.NewNumberValue(200)}, args{resp: &http.Response{StatusCode: 100}}, true},
		{"case contains true", fields{Key: "body", Operate: "contains", Value: structpb.NewStringValue("Error")}, args{resp: &http.Response{Body: nil}}, true},
		{"case not support", fields{Key: "xx", Operate: "contains", Value: structpb.NewStringValue("Error")}, args{resp: &http.Response{Body: nil}}, true},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			condition := &Triggering{
				Key:     tt.fields.Key,
				Operate: tt.fields.Operate,
				Value:   tt.fields.Value,
			}
			if got := condition.Executor(tt.args.resp); got != tt.want {
				t.Errorf("Executor() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestNew(t *testing.T) {
	type args struct {
		c *pb.Condition
	}
	tests := []struct {
		name string
		args args
		want *Triggering
	}{
		{"case 1", args{c: &pb.Condition{
			Key:     "http_code",
			Operate: ">",
			Value:   structpb.NewNumberValue(200),
		}}, &Triggering{
			Key:     "http_code",
			Operate: ">",
			Value:   structpb.NewNumberValue(200),
		}},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := New(tt.args.c); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("New() = %v, want %v", got, tt.want)
			}
		})
	}
}
