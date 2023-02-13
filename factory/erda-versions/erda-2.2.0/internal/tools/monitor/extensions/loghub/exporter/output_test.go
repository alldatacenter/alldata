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

package exporter

import (
	"reflect"
	"testing"
	"time"

	"github.com/golang/mock/gomock"

	"github.com/erda-project/erda-infra/base/logs"
)

// -go:generate mockgen -destination=./mock_log.go -package exporter github.com/erda-project/erda-infra/base/logs Logger
// -go:generate mockgen -destination=./mock_output.go -package exporter -source=./interface.go Output
func TestInvoke_WithLogAnalyticsPattern_Should_Call_Output(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	logger := NewMockLogger(ctrl)
	output := NewMockOutput(ctrl)

	output.EXPECT().Write(gomock.Any(), gomock.Any()).Return(nil)

	c := &consumer{
		filters: map[string]string{
			"monitor_log_key": "n4e4d034460114086b2a2b203312f5522",
		},
		log:    logger,
		output: output,
	}

	topic := "topic"
	err := c.Invoke(nil, []byte(`{
		"source": "container",
		"id": "3eb75b2ba0d1560c6148f3023e63c16915e32b077857591dbdb42beca98d997f",
		"stream": "stdout",
		"content": "\u001b[37mDEBU\u001b[0m[2021-08-24 09:50:02.404177939] service: core-services endpoint acquired: core-services.project-387-test.svc.cluster.local:9526 ",
		"offset": 8403051,
		"timestamp": 1629769802404,
		"tags": {
			"container_name": "dop",
			"dice_application_id": "5880",
			"dice_application_name": "erda",
			"dice_cluster_name": "erda-hongkong",
			"dice_org_id": "100060",
			"dice_project_id": "387",
			"dice_project_name": "erda-project",
			"dice_runtime_id": "12496",
			"dice_runtime_name": "develop",
			"dice_service_name": "dop",
			"dice_workspace": "test",
			"module": "erda-project/erda/dop",
			"monitor_log_key": "n4e4d034460114086b2a2b203312f5522",
			"origin": "dice",
			"pod_name": "dop-a525e02f6c-55959d6dbf-7sq8q",
			"pod_namespace": "project-387-test",
			"terminus_key": "t6f7b240844ad47cd8473c30da36ae5dd",
			"terminus_log_key": "n4e4d034460114086b2a2b203312f5522"
		},
		"labels":{
			"container_name": "dop"
		},
		"uniId": "5"
	}`), &topic, time.Now())

	if err != nil {
		t.Errorf("should not throw error")
	}
}

func TestInvoke_WithLogServicePattern_Should_Call_Output(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	logger := NewMockLogger(ctrl)
	output := NewMockOutput(ctrl)

	output.EXPECT().Write(gomock.Any(), gomock.Any()).Return(nil)

	c := &consumer{
		filters: map[string]string{
			"msp_env_id":     "n4e4d034460114086b2a2b203312f5522",
			"msp_log_attach": "",
		},
		log:    logger,
		output: output,
	}

	topic := "topic"
	err := c.Invoke(nil, []byte(`{
		"source": "container",
		"id": "3eb75b2ba0d1560c6148f3023e63c16915e32b077857591dbdb42beca98d997f",
		"stream": "stdout",
		"content": "\u001b[37mDEBU\u001b[0m[2021-08-24 09:50:02.404177939] service: core-services endpoint acquired: core-services.project-387-test.svc.cluster.local:9526 ",
		"offset": 8403051,
		"timestamp": 1629769802404,
		"tags": {
			"container_name": "dop",
			"dice_application_id": "5880",
			"dice_application_name": "erda",
			"dice_cluster_name": "erda-hongkong",
			"dice_org_id": "100060",
			"dice_project_id": "387",
			"dice_project_name": "erda-project",
			"dice_runtime_id": "12496",
			"dice_runtime_name": "develop",
			"dice_service_name": "dop",
			"dice_workspace": "test",
			"module": "erda-project/erda/dop",
			"origin": "dice",
			"pod_name": "dop-a525e02f6c-55959d6dbf-7sq8q",
			"pod_namespace": "project-387-test",
			"terminus_key": "t6f7b240844ad47cd8473c30da36ae5dd",
			"msp_env_id": "n4e4d034460114086b2a2b203312f5522",
			"msp_log_attach": "true"
		},
		"labels":{
			"container_name": "dop"
		},
		"uniId": "5"
	}`), &topic, time.Now())

	if err != nil {
		t.Errorf("should not throw error")
	}
}

func TestInvoke_WithNilLabels_Should_Call_Output(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	logger := NewMockLogger(ctrl)
	output := NewMockOutput(ctrl)

	output.EXPECT().Write(gomock.Any(), gomock.Any()).Return(nil)

	c := &consumer{
		filters: map[string]string{
			"msp_env_id":     "n4e4d034460114086b2a2b203312f5522",
			"msp_log_attach": "",
		},
		log:    logger,
		output: output,
	}

	topic := "topic"
	err := c.Invoke(nil, []byte(`{
		"source": "container",
		"id": "3eb75b2ba0d1560c6148f3023e63c16915e32b077857591dbdb42beca98d997f",
		"stream": "stdout",
		"content": "\u001b[37mDEBU\u001b[0m[2021-08-24 09:50:02.404177939] service: core-services endpoint acquired: core-services.project-387-test.svc.cluster.local:9526 ",
		"offset": 8403051,
		"timestamp": 1629769802404,
		"tags": {
			"container_name": "dop",
			"dice_application_id": "5880",
			"dice_application_name": "erda",
			"dice_cluster_name": "erda-hongkong",
			"dice_org_id": "100060",
			"dice_project_id": "387",
			"dice_project_name": "erda-project",
			"dice_runtime_id": "12496",
			"dice_runtime_name": "develop",
			"dice_service_name": "dop",
			"dice_workspace": "test",
			"module": "erda-project/erda/dop",
			"origin": "dice",
			"pod_name": "dop-a525e02f6c-55959d6dbf-7sq8q",
			"pod_namespace": "project-387-test",
			"terminus_key": "t6f7b240844ad47cd8473c30da36ae5dd",
			"msp_env_id": "n4e4d034460114086b2a2b203312f5522",
			"msp_log_attach": "true"
		},
		"uniId": "5"
	}`), &topic, time.Now())

	if err != nil {
		t.Errorf("should not throw error")
	}
}

func TestInvoke_WithNoneExistsFilterKey_Should_Not_Call_Output(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	logger := NewMockLogger(ctrl)

	c := &consumer{
		filters: map[string]string{
			"msp_env_id":      "n4e4d034460114086b2a2b203312f5522",
			"_not_exist_key_": "",
		},
		log:    logger,
		output: nil,
	}

	topic := "topic"
	err := c.Invoke(nil, []byte(`{
		"source": "container",
		"id": "3eb75b2ba0d1560c6148f3023e63c16915e32b077857591dbdb42beca98d997f",
		"stream": "stdout",
		"content": "\u001b[37mDEBU\u001b[0m[2021-08-24 09:50:02.404177939] service: core-services endpoint acquired: core-services.project-387-test.svc.cluster.local:9526 ",
		"offset": 8403051,
		"timestamp": 1629769802404,
		"tags": {
			"container_name": "dop",
			"dice_application_id": "5880",
			"dice_application_name": "erda",
			"dice_cluster_name": "erda-hongkong",
			"dice_org_id": "100060",
			"dice_project_id": "387",
			"dice_project_name": "erda-project",
			"dice_runtime_id": "12496",
			"dice_runtime_name": "develop",
			"dice_service_name": "dop",
			"dice_workspace": "test",
			"module": "erda-project/erda/dop",
			"origin": "dice",
			"pod_name": "dop-a525e02f6c-55959d6dbf-7sq8q",
			"pod_namespace": "project-387-test",
			"terminus_key": "t6f7b240844ad47cd8473c30da36ae5dd",
			"msp_env_id": "n4e4d034460114086b2a2b203312f5522",
			"msp_log_attach": "true"
		},
		"labels":{
			"container_name": "dop"
		},
		"uniId": "5"
	}`), &topic, time.Now())

	if err != nil {
		t.Errorf("should not throw error")
	}
}

func TestInvoke_WithNoneMatchFilterKey_Should_Not_Call_Output(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	logger := NewMockLogger(ctrl)

	c := &consumer{
		filters: map[string]string{
			"msp_env_id":     "_not_exists_",
			"msp_log_attach": "",
		},
		log:    logger,
		output: nil,
	}

	topic := "topic"
	err := c.Invoke(nil, []byte(`{
		"source": "container",
		"id": "3eb75b2ba0d1560c6148f3023e63c16915e32b077857591dbdb42beca98d997f",
		"stream": "stdout",
		"content": "\u001b[37mDEBU\u001b[0m[2021-08-24 09:50:02.404177939] service: core-services endpoint acquired: core-services.project-387-test.svc.cluster.local:9526 ",
		"offset": 8403051,
		"timestamp": 1629769802404,
		"tags": {
			"container_name": "dop",
			"dice_application_id": "5880",
			"dice_application_name": "erda",
			"dice_cluster_name": "erda-hongkong",
			"dice_org_id": "100060",
			"dice_project_id": "387",
			"dice_project_name": "erda-project",
			"dice_runtime_id": "12496",
			"dice_runtime_name": "develop",
			"dice_service_name": "dop",
			"dice_workspace": "test",
			"module": "erda-project/erda/dop",
			"origin": "dice",
			"pod_name": "dop-a525e02f6c-55959d6dbf-7sq8q",
			"pod_namespace": "project-387-test",
			"terminus_key": "t6f7b240844ad47cd8473c30da36ae5dd",
			"msp_env_id": "n4e4d034460114086b2a2b203312f5522",
			"msp_log_attach": "true"
		},
		"labels":{
			"container_name": "dop"
		},
		"uniId": "5"
	}`), &topic, time.Now())

	if err != nil {
		t.Errorf("should not throw error")
	}
}

func TestInvoke_WithNilTags_Should_Not_Call_Output(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	logger := NewMockLogger(ctrl)
	logger.EXPECT().Warnf(gomock.Any(), gomock.Any())

	c := &consumer{
		filters: map[string]string{
			"msp_env_id":      "n4e4d034460114086b2a2b203312f5522",
			"_not_exist_key_": "",
		},
		log:    logger,
		output: nil,
	}

	topic := "topic"
	err := c.Invoke(nil, []byte(`{
		"source": "container",
		"id": "3eb75b2ba0d1560c6148f3023e63c16915e32b077857591dbdb42beca98d997f",
		"stream": "stdout",
		"content": "\u001b[37mDEBU\u001b[0m[2021-08-24 09:50:02.404177939] service: core-services endpoint acquired: core-services.project-387-test.svc.cluster.local:9526 ",
		"offset": 8403051,
		"timestamp": 1629769802404,
		"uniId": "5"
	}`), &topic, time.Now())

	if err != nil {
		t.Errorf("should not throw error")
	}
}

// MockLogger is a mock of Logger interface.
type MockLogger struct {
	ctrl     *gomock.Controller
	recorder *MockLoggerMockRecorder
}

// MockLoggerMockRecorder is the mock recorder for MockLogger.
type MockLoggerMockRecorder struct {
	mock *MockLogger
}

// NewMockLogger creates a new mock instance.
func NewMockLogger(ctrl *gomock.Controller) *MockLogger {
	mock := &MockLogger{ctrl: ctrl}
	mock.recorder = &MockLoggerMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockLogger) EXPECT() *MockLoggerMockRecorder {
	return m.recorder
}

// Debug mocks base method.
func (m *MockLogger) Debug(arg0 ...interface{}) {
	m.ctrl.T.Helper()
	varargs := []interface{}{}
	for _, a := range arg0 {
		varargs = append(varargs, a)
	}
	m.ctrl.Call(m, "Debug", varargs...)
}

// Debug indicates an expected call of Debug.
func (mr *MockLoggerMockRecorder) Debug(arg0 ...interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Debug", reflect.TypeOf((*MockLogger)(nil).Debug), arg0...)
}

// Debugf mocks base method.
func (m *MockLogger) Debugf(arg0 string, arg1 ...interface{}) {
	m.ctrl.T.Helper()
	varargs := []interface{}{arg0}
	for _, a := range arg1 {
		varargs = append(varargs, a)
	}
	m.ctrl.Call(m, "Debugf", varargs...)
}

// Debugf indicates an expected call of Debugf.
func (mr *MockLoggerMockRecorder) Debugf(arg0 interface{}, arg1 ...interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	varargs := append([]interface{}{arg0}, arg1...)
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Debugf", reflect.TypeOf((*MockLogger)(nil).Debugf), varargs...)
}

// Error mocks base method.
func (m *MockLogger) Error(arg0 ...interface{}) {
	m.ctrl.T.Helper()
	varargs := []interface{}{}
	for _, a := range arg0 {
		varargs = append(varargs, a)
	}
	m.ctrl.Call(m, "Error", varargs...)
}

// Error indicates an expected call of Error.
func (mr *MockLoggerMockRecorder) Error(arg0 ...interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Error", reflect.TypeOf((*MockLogger)(nil).Error), arg0...)
}

// Errorf mocks base method.
func (m *MockLogger) Errorf(arg0 string, arg1 ...interface{}) {
	m.ctrl.T.Helper()
	varargs := []interface{}{arg0}
	for _, a := range arg1 {
		varargs = append(varargs, a)
	}
	m.ctrl.Call(m, "Errorf", varargs...)
}

// Errorf indicates an expected call of Errorf.
func (mr *MockLoggerMockRecorder) Errorf(arg0 interface{}, arg1 ...interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	varargs := append([]interface{}{arg0}, arg1...)
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Errorf", reflect.TypeOf((*MockLogger)(nil).Errorf), varargs...)
}

// Fatal mocks base method.
func (m *MockLogger) Fatal(arg0 ...interface{}) {
	m.ctrl.T.Helper()
	varargs := []interface{}{}
	for _, a := range arg0 {
		varargs = append(varargs, a)
	}
	m.ctrl.Call(m, "Fatal", varargs...)
}

// Fatal indicates an expected call of Fatal.
func (mr *MockLoggerMockRecorder) Fatal(arg0 ...interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Fatal", reflect.TypeOf((*MockLogger)(nil).Fatal), arg0...)
}

// Fatalf mocks base method.
func (m *MockLogger) Fatalf(arg0 string, arg1 ...interface{}) {
	m.ctrl.T.Helper()
	varargs := []interface{}{arg0}
	for _, a := range arg1 {
		varargs = append(varargs, a)
	}
	m.ctrl.Call(m, "Fatalf", varargs...)
}

// Fatalf indicates an expected call of Fatalf.
func (mr *MockLoggerMockRecorder) Fatalf(arg0 interface{}, arg1 ...interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	varargs := append([]interface{}{arg0}, arg1...)
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Fatalf", reflect.TypeOf((*MockLogger)(nil).Fatalf), varargs...)
}

// Info mocks base method.
func (m *MockLogger) Info(arg0 ...interface{}) {
	m.ctrl.T.Helper()
	varargs := []interface{}{}
	for _, a := range arg0 {
		varargs = append(varargs, a)
	}
	m.ctrl.Call(m, "Info", varargs...)
}

// Info indicates an expected call of Info.
func (mr *MockLoggerMockRecorder) Info(arg0 ...interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Info", reflect.TypeOf((*MockLogger)(nil).Info), arg0...)
}

// Infof mocks base method.
func (m *MockLogger) Infof(arg0 string, arg1 ...interface{}) {
	m.ctrl.T.Helper()
	varargs := []interface{}{arg0}
	for _, a := range arg1 {
		varargs = append(varargs, a)
	}
	m.ctrl.Call(m, "Infof", varargs...)
}

// Infof indicates an expected call of Infof.
func (mr *MockLoggerMockRecorder) Infof(arg0 interface{}, arg1 ...interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	varargs := append([]interface{}{arg0}, arg1...)
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Infof", reflect.TypeOf((*MockLogger)(nil).Infof), varargs...)
}

// Panic mocks base method.
func (m *MockLogger) Panic(arg0 ...interface{}) {
	m.ctrl.T.Helper()
	varargs := []interface{}{}
	for _, a := range arg0 {
		varargs = append(varargs, a)
	}
	m.ctrl.Call(m, "Panic", varargs...)
}

// Panic indicates an expected call of Panic.
func (mr *MockLoggerMockRecorder) Panic(arg0 ...interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Panic", reflect.TypeOf((*MockLogger)(nil).Panic), arg0...)
}

// Panicf mocks base method.
func (m *MockLogger) Panicf(arg0 string, arg1 ...interface{}) {
	m.ctrl.T.Helper()
	varargs := []interface{}{arg0}
	for _, a := range arg1 {
		varargs = append(varargs, a)
	}
	m.ctrl.Call(m, "Panicf", varargs...)
}

// Panicf indicates an expected call of Panicf.
func (mr *MockLoggerMockRecorder) Panicf(arg0 interface{}, arg1 ...interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	varargs := append([]interface{}{arg0}, arg1...)
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Panicf", reflect.TypeOf((*MockLogger)(nil).Panicf), varargs...)
}

// SetLevel mocks base method.
func (m *MockLogger) SetLevel(arg0 string) error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "SetLevel", arg0)
	ret0, _ := ret[0].(error)
	return ret0
}

// SetLevel indicates an expected call of SetLevel.
func (mr *MockLoggerMockRecorder) SetLevel(arg0 interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "SetLevel", reflect.TypeOf((*MockLogger)(nil).SetLevel), arg0)
}

// Sub mocks base method.
func (m *MockLogger) Sub(arg0 string) logs.Logger {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Sub", arg0)
	ret0, _ := ret[0].(logs.Logger)
	return ret0
}

// Sub indicates an expected call of Sub.
func (mr *MockLoggerMockRecorder) Sub(arg0 interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Sub", reflect.TypeOf((*MockLogger)(nil).Sub), arg0)
}

// Warn mocks base method.
func (m *MockLogger) Warn(arg0 ...interface{}) {
	m.ctrl.T.Helper()
	varargs := []interface{}{}
	for _, a := range arg0 {
		varargs = append(varargs, a)
	}
	m.ctrl.Call(m, "Warn", varargs...)
}

// Warn indicates an expected call of Warn.
func (mr *MockLoggerMockRecorder) Warn(arg0 ...interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Warn", reflect.TypeOf((*MockLogger)(nil).Warn), arg0...)
}

// Warnf mocks base method.
func (m *MockLogger) Warnf(arg0 string, arg1 ...interface{}) {
	m.ctrl.T.Helper()
	varargs := []interface{}{arg0}
	for _, a := range arg1 {
		varargs = append(varargs, a)
	}
	m.ctrl.Call(m, "Warnf", varargs...)
}

// Warnf indicates an expected call of Warnf.
func (mr *MockLoggerMockRecorder) Warnf(arg0 interface{}, arg1 ...interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	varargs := append([]interface{}{arg0}, arg1...)
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Warnf", reflect.TypeOf((*MockLogger)(nil).Warnf), varargs...)
}

// MockOutput is a mock of Output interface.
type MockOutput struct {
	ctrl     *gomock.Controller
	recorder *MockOutputMockRecorder
}

// MockOutputMockRecorder is the mock recorder for MockOutput.
type MockOutputMockRecorder struct {
	mock *MockOutput
}

// NewMockOutput creates a new mock instance.
func NewMockOutput(ctrl *gomock.Controller) *MockOutput {
	mock := &MockOutput{ctrl: ctrl}
	mock.recorder = &MockOutputMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockOutput) EXPECT() *MockOutputMockRecorder {
	return m.recorder
}

// Write mocks base method.
func (m *MockOutput) Write(key string, data []byte) error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Write", key, data)
	ret0, _ := ret[0].(error)
	return ret0
}

// Write indicates an expected call of Write.
func (mr *MockOutputMockRecorder) Write(key, data interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Write", reflect.TypeOf((*MockOutput)(nil).Write), key, data)
}

// MockInterface is a mock of Interface interface.
type MockInterface struct {
	ctrl     *gomock.Controller
	recorder *MockInterfaceMockRecorder
}

// MockInterfaceMockRecorder is the mock recorder for MockInterface.
type MockInterfaceMockRecorder struct {
	mock *MockInterface
}

// NewMockInterface creates a new mock instance.
func NewMockInterface(ctrl *gomock.Controller) *MockInterface {
	mock := &MockInterface{ctrl: ctrl}
	mock.recorder = &MockInterfaceMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockInterface) EXPECT() *MockInterfaceMockRecorder {
	return m.recorder
}

// NewConsumer mocks base method.
func (m *MockInterface) NewConsumer(arg0 OutputFactory) error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "NewConsumer", arg0)
	ret0, _ := ret[0].(error)
	return ret0
}

// NewConsumer indicates an expected call of NewConsumer.
func (mr *MockInterfaceMockRecorder) NewConsumer(arg0 interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "NewConsumer", reflect.TypeOf((*MockInterface)(nil).NewConsumer), arg0)
}
