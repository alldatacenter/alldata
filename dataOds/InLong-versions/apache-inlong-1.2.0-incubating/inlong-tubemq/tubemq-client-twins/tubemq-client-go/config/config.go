// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

// Package config defines the all the TubeMQ configuration options.
package config

import (
	"encoding/json"
	"errors"
	"fmt"
	"net/url"
	"strconv"
	"strings"
	"time"

	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/errs"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/util"
)

const (
	MaxRPCTimeout              = 300000 * time.Millisecond
	MinRPCTimeout              = 8000 * time.Millisecond
	MaxSessionKeyLen           = 1024
	MaxGroupLen                = 1024
	MaxTopicLen                = 64
	MaxFilterLen               = 256
	MaxFilterItemCount         = 500
	ConsumeFromFirstOffset     = -1
	ConsumeFromLatestOffset    = 0
	ConsumeFromMaxOffsetAlways = 1
)

// Config defines multiple configuration options.
// Refer to: https://github.com/apache/incubator-inlong/blob/3249de37acf054a9c43677131cfbb09fc6d366d1/tubemq-client/src/main/java/org/apache/tubemq/client/config/ConsumerConfig.java
type Config struct {
	// Net is the namespace for network-level properties used by broker and Master.
	Net struct {
		// ReadTimeout represents how long to wait for a response.
		ReadTimeout time.Duration
		// TLS based authentication with broker and master.
		TLS struct {
			// Enable represents whether or not to use TLS.
			Enable bool
			// CACertFile for TLS.
			CACertFile string
			// TLSCertFile for TLS.
			TLSCertFile string
			// TLSKeyFile for TLS.
			TLSKeyFile string
			// TTSServerName for TLS.
			TLSServerName string
		}
		// Auth represents the account based authentication with broker and master.
		Auth struct {
			// Enable represents whether or not to use authentication.
			Enable bool
			// Username for authentication.
			UserName string
			// Password for authentication.
			Password string
		}
	}

	// Consumer is the namespace for configuration related to consume messages,
	// used by the consumer
	Consumer struct {
		// Masters is the addresses of master.
		Masters string
		// Topics of the consumption.
		Topics []string
		// TopicFilters is the map of topic to filters.
		TopicFilters map[string][]string
		// PartitionOffset is the map of partition to its corresponding offset.
		PartitionOffset map[string]int64
		// ConsumerPosition is the initial offset to use if no offset was previously committed.
		ConsumePosition int
		// Group is the consumer group name.
		Group string
		// BoundConsume represents whether or not to specify the offset.
		BoundConsume bool
		// SessionKey is defined by the client.
		// The session key will be the same in a batch.
		SessionKey string
		// SourceCount is the number of consumers in a batch.
		SourceCount int
		// SelectBig specifies if multiple consumers want to reset the offset of the same partition,
		// whether or not to use the biggest offset.
		// The server will use the biggest offset if set, otherwise the server will use the smallest offset.
		SelectBig bool
		// RollbackIfConfirmTimeout represents if the confirm request timeouts,
		// whether or not this batch of data should be considered as successful.
		// This batch of data will not be considered as successful if set.
		RollbackIfConfirmTimeout bool
		// MaxSubInfoReportInterval is maximum interval for the client to report subscription information.
		MaxSubInfoReportInterval int
		// MaxPartCheckPeriod is the maximum interval to check the partition.
		MaxPartCheckPeriod time.Duration
		// PartCheckSlice is the interval to check the partition.
		PartCheckSlice time.Duration
		// MsgNotFoundWait is the maximum wait time the offset of a partition has reached the maximum offset.
		MsgNotFoundWait time.Duration
		// RebConfirmWait represents how long to wait
		// when the server is rebalancing and the partition is being occupied by the client.
		RebConfirmWait time.Duration
		// MaxConfirmWait is the maximum wait time a partition consumption command is released.
		MaxConfirmWait time.Duration
		// ShutdownRebWait represents how long to wait when shutdown is called and the server is rebalancing.
		ShutdownRebWait time.Duration
	}

	// Heartbeat is the namespace for configuration related to heartbeat messages,
	// used by the consumer
	Heartbeat struct {
		// Interval represents how frequently to send heartbeat.
		Interval time.Duration
		// MaxRetryTimes is the total number of times to retry sending heartbeat.
		MaxRetryTimes int
		// AfterFail is the heartbeat timeout after a heartbeat failure.
		AfterFail time.Duration
	}
}

// NewDefaultConfig returns a default config of the client.
func NewDefaultConfig() *Config {
	c := &Config{}

	c.Net.ReadTimeout = 15000 * time.Millisecond
	c.Net.TLS.Enable = false
	c.Net.Auth.Enable = false
	c.Net.Auth.UserName = ""
	c.Net.Auth.Password = ""

	c.Consumer.Group = ""
	c.Consumer.BoundConsume = false
	c.Consumer.SessionKey = ""
	c.Consumer.SourceCount = 0
	c.Consumer.SelectBig = true
	c.Consumer.ConsumePosition = 0
	c.Consumer.RollbackIfConfirmTimeout = true
	c.Consumer.MaxSubInfoReportInterval = 6
	c.Consumer.MaxPartCheckPeriod = 60000 * time.Millisecond
	c.Consumer.PartCheckSlice = 300 * time.Millisecond
	c.Consumer.MsgNotFoundWait = 400 * time.Millisecond
	c.Consumer.RebConfirmWait = 3000 * time.Millisecond
	c.Consumer.MaxConfirmWait = 60000 * time.Millisecond
	c.Consumer.ShutdownRebWait = 10000 * time.Millisecond
	c.Consumer.TopicFilters = make(map[string][]string)

	c.Heartbeat.Interval = 10000 * time.Millisecond
	c.Heartbeat.MaxRetryTimes = 5
	c.Heartbeat.AfterFail = 60000 * time.Millisecond

	return c
}

// New returns a config by the options.
func New(opts ...Option) *Config {
	c := NewDefaultConfig()
	for _, o := range opts {
		o(c)
	}
	return c
}

// String returns the config as a string.
func (c *Config) String() string {
	bytes, err := json.Marshal(c)
	if err != nil {
		return err.Error()
	}
	return string(bytes)
}

// ValidateConsumer validates the config of the consumer.
func (c *Config) ValidateConsumer() error {
	if c.Net.Auth.Enable {
		if len(c.Net.Auth.UserName) == 0 {
			return errs.New(errs.RetInvalidConfig, "illegal parameter: usrName is empty")
		}
		if len(c.Net.Auth.Password) == 0 {
			return errs.New(errs.RetInvalidConfig, "illegal password: password is empty")
		}
	}

	if c.Net.TLS.Enable {
		if len(c.Net.TLS.CACertFile) == 0 {
			return errs.New(errs.RetInvalidConfig, "illegal tls CACert file: CACert file is empty")
		}
	}

	if err := c.validateConsumePosition(); err != nil {
		return err
	}

	err := c.validateMaster()
	if err != nil {
		return errs.New(errs.RetInvalidConfig, err.Error())
	}

	err = c.validateGroup(c.Consumer.Group)
	if err != nil {
		return errs.New(errs.RetInvalidConfig, err.Error())
	}

	err = c.validateTopicFilters()
	if err != nil {
		return errs.New(errs.RetInvalidConfig, err.Error())
	}

	if c.Consumer.BoundConsume {
		err = c.validateSessionKey()
		if err != nil {
			return err
		}
		err = c.validatePartOffset()
		if err != nil {
			return err
		}
	}
	return nil
}

func (c *Config) validateMaster() error {
	if len(c.Consumer.Masters) == 0 {
		return errors.New("illegal parameter: len of masters is 0")
	}
	if len(util.SplitToMap(c.Consumer.Masters, ",", ":")) == 0 {
		return errs.New(errs.RetInvalidConfig, "illegal parameter: unrecognized master addresses information")
	}
	return nil
}

func (c *Config) validateSessionKey() error {
	if len(c.Consumer.SessionKey) == 0 {
		return errs.New(errs.RetInvalidConfig, "illegal parameter: session key is empty")
	}
	if len(c.Consumer.SessionKey) > MaxSessionKeyLen {
		return errs.New(errs.RetInvalidConfig,
			fmt.Sprintf("illegal parameter: session_key's length over max length %d", MaxSessionKeyLen))
	}
	return nil
}

func (c *Config) validatePartOffset() error {
	for partition, offset := range c.Consumer.PartitionOffset {
		s := strings.Split(partition, ":")
		if len(s) != 3 {
			return errs.New(errs.RetInvalidConfig,
				fmt.Sprintf("illegal parameter: partOffset's key %s which should be a:b:c", partition))
		}
		if _, ok := c.Consumer.TopicFilters[s[1]]; !ok {
			return errs.New(errs.RetInvalidConfig,
				fmt.Sprintf("illegal parameter: %s topic %s not included in topicFilter's topic list", partition, s[1]))
		}
		if strings.Index(partition, ",") != -1 {
			return errs.New(errs.RetInvalidConfig,
				fmt.Sprintf("illegal parameter: key %s include ,", partition))
		}
		if offset < 0 {
			return errs.New(errs.RetInvalidConfig,
				fmt.Sprintf("illegal parameter: partition %s's offset must >= 0, value is %d", partition, offset))
		}
	}
	return nil
}

func (c *Config) validateTopicFilters() error {
	for topic, filters := range c.Consumer.TopicFilters {
		if len(topic) > MaxTopicLen {
			return errs.New(errs.RetInvalidConfig,
				fmt.Sprintf("Check parameter topicFilters error: topic's length " +
					"over max length %d", MaxTopicLen))
		}
		if valid, err := util.IsValidString(topic); !valid {
			return errs.New(errs.RetInvalidConfig, err.Error())
		}
		for _, filter := range filters {
			if len(filter) == 0 {
				return errs.New(errs.RetInvalidConfig,
					fmt.Sprintf("Check parameter topicFilters error: topic %s's filter is empty", topic))
			}
			if len(filter) > MaxFilterLen {
				return errs.New(errs.RetInvalidConfig,
					fmt.Sprintf("Check parameter topicFilters error: topic %s's filter's " +
						"length over max length %d", topic, MaxFilterLen))
			}
			if valid, err := util.IsValidFilterItem(filter); !valid {
				return errs.New(errs.RetInvalidConfig, err.Error())
			}
		}
		if len(filters) > MaxFilterLen {
			return errs.New(errs.RetInvalidConfig,
				fmt.Sprintf("Check parameter topicFilters error: topic %s's filter item " +
					"over max item count %d", topic, MaxFilterLen))
		}
	}
	return nil
}

func (c *Config) validateGroup(group string) error {
	if len(group) == 0 {
		return errs.New(errs.RetInvalidConfig, "group name is empty")
	}
	if len(group) > MaxGroupLen {
		return errs.New(errs.RetInvalidConfig,
			fmt.Sprintf("illegal parameter: %s over max length, the max allowed length is %d", group, MaxGroupLen))
	}
	if valid, err := util.IsValidString(group); !valid {
		return errs.New(errs.RetInvalidConfig, err.Error())
	}
	return nil
}

func (c *Config) validateConsumePosition() error {
	if c.Consumer.ConsumePosition < ConsumeFromFirstOffset || c.Consumer.ConsumePosition > ConsumeFromMaxOffsetAlways {
		return errs.New(errs.RetInvalidConfig,
			fmt.Sprintf("consumePosition should be only in (-1, 0, 1), while %d is passed", c.Consumer.ConsumePosition))
	}
	return nil
}

// ParseAddress parses the address to user-defined config.
func ParseAddress(address string) (config *Config, err error) {
	c := NewDefaultConfig()

	tokens := strings.SplitN(address, "?", 2)
	if len(tokens) != 2 {
		return nil, fmt.Errorf("address format invalid: address: %v, token: %v", address, tokens)
	}

	c.Consumer.Masters = tokens[0]

	tokens = strings.Split(tokens[1], "&")
	if len(tokens) == 0 {
		return nil, fmt.Errorf("address formata invalid: Masters: %v with empty params", config.Consumer.Masters)
	}

	for _, token := range tokens {
		values := strings.SplitN(token, "=", 2)
		if len(values) != 2 {
			return nil, fmt.Errorf("address format invalid, key=value missing: %v", values)
		}

		values[1], _ = url.QueryUnescape(values[1])
		if err := getConfigFromToken(c, values); err != nil {
			return nil, err
		}
	}
	return c, nil
}

func getConfigFromToken(config *Config, values []string) error {
	var err error
	switch values[0] {
	case "readTimeout":
		config.Net.ReadTimeout, err = parseDuration(values[1])
	case "tlsEnable":
		config.Net.TLS.Enable, err = strconv.ParseBool(values[1])
	case "CACertFile":
		config.Net.TLS.CACertFile = values[1]
	case "tlsCertFile":
		config.Net.TLS.TLSCertFile = values[1]
	case "tlsKeyFile":
		config.Net.TLS.TLSKeyFile = values[1]
	case "tlsServerName":
		config.Net.TLS.TLSServerName = values[1]
	case "group":
		config.Consumer.Group = values[1]
	case "topic":
		config.Consumer.Topics = append(config.Consumer.Topics, values[1])
	case "filters":
		topicFilters := config.Consumer.TopicFilters
		if len(config.Consumer.Topics) == 0 {
			return errs.New(errs.RetInvalidConfig, fmt.Sprintf("topic filters %s should have topic", values[1]))
		}
		topic := config.Consumer.Topics[len(config.Consumer.Topics)-1]
		topicFilters[topic] = append(topicFilters[topic], values[1])
	case "consumePosition":
		config.Consumer.ConsumePosition, err = strconv.Atoi(values[1])
	case "boundConsume":
		config.Consumer.BoundConsume, err = strconv.ParseBool(values[1])
	case "sessionKey":
		config.Consumer.SessionKey = values[1]
	case "sourceCount":
		config.Consumer.SourceCount, err = strconv.Atoi(values[1])
	case "selectBig":
		config.Consumer.SelectBig, err = strconv.ParseBool(values[1])
	case "rollbackIfConfirmTimeout":
		config.Consumer.RollbackIfConfirmTimeout, err = strconv.ParseBool(values[1])
	case "maxSubInfoReportInterval":
		config.Consumer.MaxSubInfoReportInterval, err = strconv.Atoi(values[1])
	case "maxPartCheckPeriod":
		config.Consumer.MaxPartCheckPeriod, err = parseDuration(values[1])
	case "partCheckSlice":
		config.Consumer.PartCheckSlice, err = parseDuration(values[1])
	case "msgNotFoundWait":
		config.Consumer.MsgNotFoundWait, err = parseDuration(values[1])
	case "rebConfirmWait":
		config.Consumer.RebConfirmWait, err = parseDuration(values[1])
	case "maxConfirmWait":
		config.Consumer.MaxConfirmWait, err = parseDuration(values[1])
	case "shutdownRebWait":
		config.Consumer.ShutdownRebWait, err = parseDuration(values[1])
	case "heartbeatInterval":
		config.Heartbeat.Interval, err = parseDuration(values[1])
	case "heartbeatMaxRetryTimes":
		config.Heartbeat.MaxRetryTimes, err = strconv.Atoi(values[1])
	case "heartbeatAfterFail":
		config.Heartbeat.AfterFail, err = parseDuration(values[1])
	case "authEnable":
		config.Net.Auth.Enable, err = strconv.ParseBool(values[1])
	case "authUserName":
		config.Net.Auth.UserName = values[1]
	case "authPassword":
		config.Net.Auth.Password = values[1]
	default:
		return fmt.Errorf("address format invalid, unknown keys: %v", values[0])
	}
	if err != nil {
		return fmt.Errorf("address format invalid(%v) err:%s", values[0], err.Error())
	}
	return err
}

func parseDuration(val string) (time.Duration, error) {
	maxWait, err := strconv.Atoi(val)
	if err != nil {
		return 0, err
	}
	return time.Duration(maxWait) * time.Millisecond, err
}

type Option func(*Config)

func WithMaxPartCheckPeriod(d time.Duration) Option {
	return func(c *Config) {
		c.Consumer.MaxPartCheckPeriod = d
	}
}

func WithPartCheckSlice(d time.Duration) Option {
	return func(c *Config) {
		c.Consumer.PartCheckSlice = d
	}
}

func WithMsgNotFoundWait(d time.Duration) Option {
	return func(c *Config) {
		c.Consumer.MsgNotFoundWait = d
	}
}

func WithMaxSubInfoReportInterval(i int) Option {
	return func(c *Config) {
		c.Consumer.MaxSubInfoReportInterval = i
	}
}

func WithRebConfirmWait(d time.Duration) Option {
	return func(c *Config) {
		c.Consumer.RebConfirmWait = d
	}
}

func WithMaxConfirmWait(d time.Duration) Option {
	return func(c *Config) {
		c.Consumer.MaxConfirmWait = d
	}
}

func WithShutdownRebWait(d time.Duration) Option {
	return func(c *Config) {
		c.Consumer.ShutdownRebWait = d
	}
}

func WithTopicFilters(topicFilters map[string][]string) Option {
	return func(c *Config) {
		c.Consumer.TopicFilters = topicFilters
		for topic := range c.Consumer.TopicFilters {
			topic = strings.TrimSpace(topic)
		}
	}
}

func WithSourceCount(count int) Option {
	return func(c *Config) {
		c.Consumer.SourceCount = count
	}
}

func WithSelectBig(selectBig bool) Option {
	return func(c *Config) {
		c.Consumer.SelectBig = selectBig
	}
}

func WithPartOffsets(partOffsets map[string]int64) Option {
	return func(c *Config) {
		c.Consumer.PartitionOffset = partOffsets
	}
}

func WithAuth(enable bool, userName string, password string) Option {
	return func(c *Config) {
		c.Net.Auth.Enable = enable
		c.Net.Auth.UserName = strings.TrimSpace(userName)
		c.Net.Auth.Password = strings.TrimSpace(password)
	}
}

func WithTLS(enable bool, certFile, keyFile, caFile, serverName string) Option {
	return func(c *Config) {
		c.Net.TLS.Enable = enable
		c.Net.TLS.TLSCertFile = strings.TrimSpace(certFile)
		c.Net.TLS.TLSKeyFile = strings.TrimSpace(keyFile)
		c.Net.TLS.CACertFile = strings.TrimSpace(caFile)
		c.Net.TLS.TLSServerName = strings.TrimSpace(serverName)
	}
}

func WithGroup(group string) Option {
	return func(c *Config) {
		c.Consumer.Group = strings.TrimSpace(group)
	}
}

func WithTopics(topics []string) Option {
	return func(c *Config) {
		c.Consumer.Topics = topics
		c.Consumer.TopicFilters = make(map[string][]string)
		for _, topic := range c.Consumer.Topics {
			c.Consumer.TopicFilters[topic] = nil
		}
	}
}

func WithConsumerMasters(masters string) Option {
	return func(c *Config) {
		c.Consumer.Masters = strings.TrimSpace(masters)
	}
}

func WithRPCReadTimeout(d time.Duration) Option {
	return func(c *Config) {
		if d >= MaxRPCTimeout {
			c.Net.ReadTimeout = MaxRPCTimeout
		} else if d <= MinRPCTimeout {
			c.Net.ReadTimeout = MinRPCTimeout
		} else {
			c.Net.ReadTimeout = d
		}
	}
}

func WithBoundConsume(sessionKey string, sourceCount int, selectBig bool, partOffset map[string]int64) Option {
	return func(c *Config) {
		c.Consumer.BoundConsume = true
		c.Consumer.SessionKey = strings.TrimSpace(sessionKey)
		c.Consumer.SourceCount = sourceCount
		c.Consumer.SelectBig = selectBig
		c.Consumer.PartitionOffset = partOffset
	}
}

func WithConsumePosition(consumePosition int) Option {
	return func(c *Config) {
		c.Consumer.ConsumePosition = consumePosition
	}
}
