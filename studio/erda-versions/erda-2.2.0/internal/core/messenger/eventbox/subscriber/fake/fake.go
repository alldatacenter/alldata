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

package fake

import (
	"os"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/core/messenger/eventbox/subscriber"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/types"
)

const (
	FakeTestFilePath = "fake_subscriber.txt"
)

type FakeSubscriber struct {
	file *os.File
}

func New(filepath string) (subscriber.Subscriber, error) {
	file, err := os.OpenFile(filepath, os.O_CREATE|os.O_RDWR|os.O_TRUNC, 0666)
	if err != nil {
		return nil, err
	}
	return &FakeSubscriber{file}, nil
}

func (s *FakeSubscriber) Publish(dest string, content string, timestamp int64, msg *types.Message) []error {
	time.Sleep(100 * time.Millisecond)
	logrus.Infof("FAKE: publish message: %s", string(content))
	s.file.WriteString(time.Unix(0, timestamp).Format("2006-01-02 15:04:05"))
	s.file.WriteString(" | ")
	s.file.WriteString(content)
	s.file.WriteString("\n")
	return nil
}

func (s *FakeSubscriber) Status() interface{} {
	return nil
}

func (s *FakeSubscriber) Name() string {
	return "FAKE"
}
