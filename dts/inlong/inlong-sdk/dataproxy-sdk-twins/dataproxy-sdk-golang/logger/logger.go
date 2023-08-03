//
// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package logger

import "fmt"

// Logger is the logger interface used in this module
type Logger interface {
	Debug(args ...interface{})
	Info(args ...interface{})
	Warn(args ...interface{})
	Error(args ...interface{})
	Fatal(args ...interface{})
	Panic(args ...interface{})
	Debugf(template string, args ...interface{})
	Infof(template string, args ...interface{})
	Warnf(template string, args ...interface{})
	Errorf(template string, args ...interface{})
	Fatalf(template string, args ...interface{})
	Panicf(template string, args ...interface{})
}

var (
	std = stdLogger{}
)

type stdLogger struct {
}

func (s stdLogger) Debug(args ...interface{}) {
	fmt.Println(args...)
}

func (s stdLogger) Info(args ...interface{}) {
	fmt.Println(args...)
}

func (s stdLogger) Warn(args ...interface{}) {
	fmt.Println(args...)
}

func (s stdLogger) Error(args ...interface{}) {
	fmt.Println(args...)
}

func (s stdLogger) Fatal(args ...interface{}) {
	fmt.Println(args...)
}

func (s stdLogger) Panic(args ...interface{}) {
	fmt.Println(args...)
}

func (s stdLogger) Debugf(template string, args ...interface{}) {
	fmt.Println(fmt.Sprintf(template, args...))
}

func (s stdLogger) Infof(template string, args ...interface{}) {
	fmt.Println(fmt.Sprintf(template, args...))
}

func (s stdLogger) Warnf(template string, args ...interface{}) {
	fmt.Println(fmt.Sprintf(template, args...))
}

func (s stdLogger) Errorf(template string, args ...interface{}) {
	fmt.Println(fmt.Sprintf(template, args...))
}

func (s stdLogger) Fatalf(template string, args ...interface{}) {
	fmt.Println(fmt.Sprintf(template, args...))
}

func (s stdLogger) Panicf(template string, args ...interface{}) {
	fmt.Println(fmt.Sprintf(template, args...))
}

// Std returns a standard logger that writes logger to the stdOut
func Std() Logger {
	return std
}
