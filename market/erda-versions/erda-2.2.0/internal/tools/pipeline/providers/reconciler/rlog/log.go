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

package rlog

import (
	"fmt"

	"github.com/sirupsen/logrus"
)

const (
	errorFormat  = "reconciler: %s"
	pErrorFormat = "reconciler: pipelineID: %d, %s"
	tErrorFormat = "reconciler: pipelineID: %d, taskID: %d, %s"
)

func Infof(format string, args ...interface{}) {
	format = handleFormat(format)
	logrus.Infof(format, args...)
}

func PInfof(pipelineID uint64, format string, args ...interface{}) {
	format = handlePFormat(pipelineID, format)
	logrus.Infof(format, args...)
}

func TInfof(pipelineID, taskID uint64, format string, args ...interface{}) {
	format = handleTFormat(pipelineID, taskID, format)
	logrus.Infof(format, args...)
}

func PWarnf(pipelineID uint64, format string, args ...interface{}) {
	format = handlePFormat(pipelineID, format)
	logrus.Warnf(format, args...)
}

func TWarnf(pipelineID, taskID uint64, format string, args ...interface{}) {
	format = handleTFormat(pipelineID, taskID, format)
	format = handleAlert(format)
	logrus.Warnf(format, args...)
}

func PDebugf(pipelineID uint64, format string, args ...interface{}) {
	format = handlePFormat(pipelineID, format)
	logrus.Debugf(format, args...)
}

func TDebugf(pipelineID, taskID uint64, format string, args ...interface{}) {
	format = handleTFormat(pipelineID, taskID, format)
	logrus.Debugf(format, args...)
}

func Errorf(format string, args ...interface{}) {
	format = handleFormat(format)
	format = handleAlert(format)
	logrus.Errorf(format, args...)
}

func PErrorf(pipelineID uint64, format string, args ...interface{}) {
	format = handlePFormat(pipelineID, format)
	format = handleAlert(format)
	logrus.Errorf(format, args...)
}

func TErrorf(pipelineID, taskID uint64, format string, args ...interface{}) {
	format = handleTFormat(pipelineID, taskID, format)
	format = handleAlert(format)
	logrus.Errorf(format, args...)
}

// ErrorAndReturn print polished log and return original error.
func ErrorAndReturn(err error) error {
	format := handleFormat(err.Error())
	format = handleAlert(format)
	logrus.Errorf(format)
	return err
}

// PErrorAndReturn print polished log and return original error.
func PErrorAndReturn(pipelineID uint64, err error) error {
	format := handlePFormat(pipelineID, err.Error())
	format = handleAlert(format)
	logrus.Errorf(format)
	return err
}

// TErrorAndReturn print polished log and return original error.
func TErrorAndReturn(pipelineID, taskID uint64, err error) error {
	format := handleTFormat(pipelineID, taskID, err.Error())
	format = handleAlert(format)
	logrus.Errorf(format)
	return err
}

func handleFormat(format string) string {
	return fmt.Sprintf(errorFormat, format)
}

func handlePFormat(pipelineID uint64, format string) string {
	return fmt.Sprintf(pErrorFormat, pipelineID, format)
}

func handleTFormat(pipelineID, taskID uint64, format string) string {
	return fmt.Sprintf(tErrorFormat, pipelineID, taskID, format)
}

func handleAlert(format string) string {
	return fmt.Sprintf("[alert] %s", format)
}
