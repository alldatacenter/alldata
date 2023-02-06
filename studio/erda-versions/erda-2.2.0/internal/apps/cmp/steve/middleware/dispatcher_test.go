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

package middleware

import (
	"reflect"
	"testing"

	"github.com/bugaolengdeyuxiaoer/go-ansiterm"
)

var testDispatcher *dispatcher

func init() {
	auditReqChan := make(chan *cmdWithTimestamp, 10)
	closeChan := make(chan struct{}, 10)
	testDispatcher = NewDispatcher(auditReqChan, closeChan)
}
func TestNewDispatcher(t *testing.T) {
	closeChan := make(chan struct{}, 10)
	auditReqChan := make(chan *cmdWithTimestamp, 10)
	tests := []struct {
		name string
		want *dispatcher
	}{
		{
			name: "1",
			want: NewDispatcher(auditReqChan, closeChan),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := NewDispatcher(auditReqChan, closeChan); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("NewDispatcher() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_commandQueue_Back(t *testing.T) {
	type fields struct {
		cmds    []string
		length  int
		maxSize int
	}
	tests := []struct {
		name   string
		fields fields
		want   string
	}{
		{
			name: "1",
			fields: fields{
				cmds:    make([]string, 10),
				length:  0,
				maxSize: 3,
			},
			want: "123",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			c := &commandQueue{
				cmds:    tt.fields.cmds,
				length:  tt.fields.length,
				maxSize: tt.fields.maxSize,
			}
			err := c.pushBack("123")
			if err != nil {
				return
			}
		})
	}
}

func Test_commandQueue_Front(t *testing.T) {
	type fields struct {
		cmds    []string
		length  int
		maxSize int
	}
	tests := []struct {
		name   string
		fields fields
		want   string
	}{
		{
			name: "1",
			fields: fields{
				cmds:    make([]string, 10),
				length:  0,
				maxSize: 3,
			},
			want: "123",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			c := &commandQueue{
				cmds:    tt.fields.cmds,
				length:  tt.fields.length,
				maxSize: tt.fields.maxSize,
			}
			err := c.pushFront("123")
			if err != nil {
				return
			}
		})
	}
}

func Test_commandQueue_Get(t *testing.T) {
	type fields struct {
		cmds    []string
		length  int
		maxSize int
	}
	type args struct {
		i int
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    string
		wantErr bool
	}{
		{
			name: "1",
			fields: fields{
				cmds:    make([]string, 10),
				length:  0,
				maxSize: 3,
			},
			want: "123",
			args: args{i: 2},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			c := &commandQueue{
				cmds:    tt.fields.cmds,
				length:  tt.fields.length,
				maxSize: tt.fields.maxSize,
			}
			c.pushFront("123")
			c.pushFront("123")
			c.pushFront("123")
			got, err := c.Get(tt.args.i)
			if (err != nil) != tt.wantErr {
				t.Errorf("Get() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("Get() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_commandQueue_Size(t *testing.T) {
	type fields struct {
		cmds    []string
		length  int
		maxSize int
	}
	tests := []struct {
		name   string
		fields fields
		want   int
	}{
		{
			name: "1",
			fields: fields{
				cmds:    make([]string, 10),
				length:  0,
				maxSize: 3,
			},
			want: 1,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			c := &commandQueue{
				cmds:    tt.fields.cmds,
				length:  tt.fields.length,
				maxSize: tt.fields.maxSize,
			}
			c.pushFront("123")
			if got := c.Size(); got != tt.want {
				t.Errorf("Size() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_dispatcher_Print(t *testing.T) {
	type fields struct {
		queue          Queue
		cursorLine     int
		cursorIdx      int
		length         int
		buf            []byte
		BufferMaxSize  int
		executeCommand string
		cmds           []*cmdWithTimestamp
	}
	closeChan := make(chan struct{}, 10)
	auditReqChan := make(chan *cmdWithTimestamp, 10)
	type args struct {
		b byte
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			name: "1",
			args: args{b: 97},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			d := NewDispatcher(auditReqChan, closeChan)
			if err := d.Print(tt.args.b); (err != nil) != tt.wantErr {
				t.Errorf("Print() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func Test_dispatcher_Execute(t *testing.T) {
	type fields struct {
		queue          Queue
		cursorLine     int
		cursorIdx      int
		length         int
		buf            []byte
		BufferMaxSize  int
		executeCommand string
		cmds           []*cmdWithTimestamp
	}
	closeChan := make(chan struct{}, 10)
	auditReqChan := make(chan *cmdWithTimestamp, 10)
	type args struct {
		b byte
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			name: "1",
			args: args{b: 13},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			d := NewDispatcher(auditReqChan, closeChan)
			if err := d.Execute(tt.args.b); (err != nil) != tt.wantErr {
				t.Errorf("Execute() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func Test_dispatcher_CUU(t *testing.T) {
	type fields struct {
		queue          Queue
		cursorLine     int
		cursorIdx      int
		length         int
		buf            []byte
		BufferMaxSize  int
		executeCommand string
		cmds           []*cmdWithTimestamp
	}
	closeChan := make(chan struct{}, 10)
	auditReqChan := make(chan *cmdWithTimestamp, 10)
	type args struct {
		i int
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			name: "1",
			args: args{i: 2},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			d := NewDispatcher(auditReqChan, closeChan)
			parser := ansiterm.CreateParser("Ground", d)
			parser.Parse([]byte{97})
			parser.Parse([]byte{13})
			parser.Parse([]byte{98})
			parser.Parse([]byte{13})
			if err := d.CUU(tt.args.i); (err != nil) != tt.wantErr {
				t.Errorf("CUU() error = %v, wantErr %v", err, tt.wantErr)
			}
			parser.Parse([]byte{13})
			d.CUU(tt.args.i)
			if err := d.CUD(tt.args.i - 1); (err != nil) != tt.wantErr {
				t.Errorf("CUU() error = %v, wantErr %v", err, tt.wantErr)
			}
			parser.Parse([]byte{13})
		})
	}
}

func Test_dispatcher_CUF(t *testing.T) {
	type fields struct {
		queue          Queue
		cursorLine     int
		cursorIdx      int
		length         int
		buf            []byte
		BufferMaxSize  int
		executeCommand string
		cmds           []*cmdWithTimestamp
	}
	closeChan := make(chan struct{}, 10)
	auditReqChan := make(chan *cmdWithTimestamp, 10)
	type args struct {
		i int
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			name: "1",
			args: args{i: 2},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			d := NewDispatcher(auditReqChan, closeChan)
			parser := ansiterm.CreateParser("Ground", d)
			parser.Parse([]byte{97})
			parser.Parse([]byte{98})
			if err := d.CUF(tt.args.i); (err != nil) != tt.wantErr {
				t.Errorf("CUF() error = %v, wantErr %v", err, tt.wantErr)
			}
			if err := d.CUF(tt.args.i - 1); (err != nil) != tt.wantErr {
				t.Errorf("CUF() error = %v, wantErr %v", err, tt.wantErr)
			}
			parser.Parse([]byte{98})
			parser.Parse([]byte{13})
		})
	}
}

func Test_dispatcher_RemoveBackwardCharacter(t *testing.T) {

	tests := []struct {
		name    string
		wantErr bool
	}{
		{
			name: "1",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := testDispatcher.RemoveBackwardCharacter(); (err != nil) != tt.wantErr {
				t.Errorf("RemoveBackwardCharacter() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func Test_dispatcher_ReverseSearch(t *testing.T) {
	type args struct {
		b byte
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		// TODO: Add test cases.
		{
			name: "1",
			args: args{'a'},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			d := testDispatcher
			if err := d.ReverseSearch(tt.args.b); (err != nil) != tt.wantErr {
				t.Errorf("search() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func Test_dispatcher_RemoveBackwardWord(t *testing.T) {
	tests := []struct {
		name    string
		wantErr bool
	}{
		// TODO: Add test cases.
		{
			name: "1",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			d := testDispatcher
			d.buf = []byte(" ads sda edg ")
			d.cursorIdx = 5
			d.length = len(d.buf) - 1
			if err := d.RemoveBackwardWord(); (err != nil) != tt.wantErr {
				t.Errorf("RemoveBackwardWord() error = %v, wantErr %v", err, tt.wantErr)
			}
			if string(d.buf[startIdx:startIdx+d.length]) != "sda edg " {
				t.Errorf("RemoveBackwardWord() :%s required ,%s got", "sda edg ", string(d.buf[startIdx:startIdx+d.length]))
			}
			d.cursorIdx = 1
			if err := d.RemoveBackwardWord(); (err != nil) != tt.wantErr {
				t.Errorf("RemoveBackwardWord() error = %v, wantErr %v", err, tt.wantErr)
			}
			if string(d.buf[startIdx:startIdx+d.length]) != "sda edg " {
				t.Errorf("RemoveBackwardWord() :%s required ,%s got", "sda edg ", string(d.buf[startIdx:startIdx+d.length]))
			}
			d.cursorIdx = 4
			if err := d.RemoveBackwardWord(); (err != nil) != tt.wantErr {
				t.Errorf("RemoveBackwardWord() error = %v, wantErr %v", err, tt.wantErr)
			}
			if string(d.buf[startIdx:startIdx+d.length]) != " edg " {
				t.Errorf("RemoveBackwardWord() :%s required ,%s got", " edg ", string(d.buf[startIdx:startIdx+d.length]))
			}
			d.cursorIdx = 5
			if err := d.RemoveBackwardWord(); (err != nil) != tt.wantErr {
				t.Errorf("RemoveBackwardWord() error = %v, wantErr %v", err, tt.wantErr)
			}
			if string(d.buf[startIdx:startIdx+d.length]) != "  " {
				t.Errorf("RemoveBackwardWord() :%s required ,%s got", "  ", string(d.buf[startIdx:startIdx+d.length]))
			}
			d.cursorIdx = 3
			if err := d.RemoveBackwardWord(); (err != nil) != tt.wantErr {
				t.Errorf("RemoveBackwardWord() error = %v, wantErr %v", err, tt.wantErr)
			}
			if string(d.buf[startIdx:startIdx+d.length]) != "" {
				t.Errorf("RemoveBackwardWord() :%s required ,%s got", "", string(d.buf[startIdx:startIdx+d.length]))
			}

		})
	}
}

func Test_dispatcher_MoveForwardWord(t *testing.T) {

	tests := []struct {
		name    string
		wantErr bool
	}{
		// TODO: Add test cases.
		{
			name: "1",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			d := testDispatcher
			d.buf = []byte(" who is your daddy?")
			d.cursorIdx = startIdx
			d.length = len(d.buf) - 1
			if err := d.MoveForwardWord(); (err != nil) != tt.wantErr {
				t.Errorf("MoveForwardWord() error = %v, wantErr %v", err, tt.wantErr)
			}
			if d.cursorIdx != 4 {
				t.Errorf("MoveForwardWord() get cursor :%d ,required idx :%d", d.cursorIdx, 4)
			}
			d.buf = []byte("         who is your daddy?")
			d.cursorIdx = startIdx
			d.length = len(d.buf) - 1
			if err := d.MoveForwardWord(); (err != nil) != tt.wantErr {
				t.Errorf("MoveForwardWord() error = %v, wantErr %v", err, tt.wantErr)
			}
			if d.cursorIdx != 12 {
				t.Errorf("MoveForwardWord() get cursor :%d ,required idx :%d", d.cursorIdx, 12)
			}
			d.buf = []byte("        ")
			d.cursorIdx = startIdx
			d.length = len(d.buf) - 1
			if err := d.MoveForwardWord(); (err != nil) != tt.wantErr {
				t.Errorf("MoveForwardWord() error = %v, wantErr %v", err, tt.wantErr)
			}
			if d.cursorIdx != 8 {
				t.Errorf("MoveForwardWord() get cursor :%d ,required idx :%d", d.cursorIdx, 8)
			}
		})
	}
}

func Test_dispatcher_MoveBackwardWord(t *testing.T) {
	tests := []struct {
		name    string
		wantErr bool
	}{
		// TODO: Add test cases.
		{
			name: "1",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			d := testDispatcher
			d.buf = []byte(" who is your   daddy?")
			d.length = len(d.buf) - 1
			d.cursorIdx = len(d.buf)
			if err := d.MoveBackwardWord(); (err != nil) != tt.wantErr {
				t.Errorf("MoveBackwardWord() error = %v, wantErr %v", err, tt.wantErr)
			}
			if d.cursorIdx != 15 {
				t.Errorf("MoveForwardWord() get cursor :%d ,required idx :%d", d.cursorIdx, 15)
			}
			if err := d.MoveBackwardWord(); (err != nil) != tt.wantErr {
				t.Errorf("MoveBackwardWord() error = %v, wantErr %v", err, tt.wantErr)
			}
			if d.cursorIdx != 8 {
				t.Errorf("MoveForwardWord() get cursor :%d ,required idx :%d", d.cursorIdx, 8)
			}
			d.cursorIdx = 3
			if err := d.MoveBackwardWord(); (err != nil) != tt.wantErr {
				t.Errorf("MoveBackwardWord() error = %v, wantErr %v", err, tt.wantErr)
			}
			if d.cursorIdx != startIdx {
				t.Errorf("MoveForwardWord() get cursor :%d ,required idx :%d", d.cursorIdx, startIdx)
			}
		})
	}
}

func Test_dispatcher_SwapLastTwoCharacter(t *testing.T) {

	tests := []struct {
		name string
	}{
		// TODO: Add test cases.
		{
			name: "1",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			d := testDispatcher
			d.buf = []byte(" 321 ")
			d.cursorIdx = len(d.buf)
			d.length = len(d.buf) - 1
			d.SwapLastTwoCharacter()
			if string(d.buf[startIdx:]) != "32 1" {
				t.Errorf("SwapLastTwoCharacte() get %s ,required :%s", string(d.buf[startIdx:]), "32 1")
			}
			d.buf = []byte(" 3")
			d.cursorIdx = len(d.buf)
			d.length = len(d.buf) - 1
			d.SwapLastTwoCharacter()
			if string(d.buf[startIdx:]) != "3" {
				t.Errorf("SwapLastTwoCharacte() get %s ,required :%s", string(d.buf[startIdx:]), "3")
			}
			d.cursorIdx = startIdx
			d.SwapLastTwoCharacter()
			if string(d.buf[startIdx:]) != "3" {
				t.Errorf("SwapLastTwoCharacte() get %s ,required :%s", string(d.buf[startIdx:]), "3")
			}
			d.buf = []byte(" 321")
			d.cursorIdx = 2
			d.length = len(d.buf) - 1
			d.SwapLastTwoCharacter()
			if string(d.buf[startIdx:]) != "231" {
				t.Errorf("SwapLastTwoCharacte() get %s ,required :%s", string(d.buf[startIdx:]), "231")
			}
		})
	}
}
func Test_dispatcher_reverseContains(t *testing.T) {
	type args struct {
		str    string
		substr string
	}
	tests := []struct {
		name  string
		args  args
		want  bool
		want1 int
	}{
		// TODO: Add test cases.
		{
			name: "1",
			args: args{
				str:    "ababab",
				substr: "ab",
			},
			want:  true,
			want1: 4,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			d := testDispatcher
			got, got1 := d.reverseContains(tt.args.str, tt.args.substr)
			if got != tt.want {
				t.Errorf("reverseContains() got = %v, want %v", got, tt.want)
			}
			if got1 != tt.want1 {
				t.Errorf("reverseContains() got1 = %v, want %v", got1, tt.want1)
			}
		})
	}
}

func Test_commandQueue_Set(t *testing.T) {
	type fields struct {
		cmds    []string
		length  int
		maxSize int
	}
	type args struct {
		i   int
		str string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		// TODO: Add test cases.
		{
			name:   "1",
			fields: fields{cmds: []string{"12", "23"}},
			args: args{
				i:   0,
				str: "34",
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			c := &commandQueue{
				cmds:    tt.fields.cmds,
				length:  tt.fields.length,
				maxSize: tt.fields.maxSize,
			}
			if err := c.Set(tt.args.i, tt.args.str); (err != nil) != tt.wantErr {
				t.Errorf("Set() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func Test_dispatcher_removeAndStore(t *testing.T) {
	type args struct {
		start int
		end   int
	}
	tests := []struct {
		name string
		args args
	}{
		// TODO: Add test cases.
		{
			name: "1",
			args: args{
				start: 1,
				end:   3,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			d := testDispatcher
			d.buf = []byte(" 123 321")
			d.length = 7
			d.removeAndStore(tt.args.start, tt.args.end)
			if string(d.buf[startIdx:startIdx+d.length]) != " 321" {
				t.Errorf("remove error")
			}
		})
	}
}
