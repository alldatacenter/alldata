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

package card

import (
	"context"
	"fmt"
	"strconv"

	"google.golang.org/protobuf/types/known/structpb"

	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/common"
	"github.com/erda-project/erda/pkg/common/errors"
)

type Card interface {
	GetCard(ctx context.Context) (*ServiceCard, error)
}

type CardType string

const (
	CardTypeReqCount    CardType = "reqCount"
	CardTypeAvgDuration CardType = "avgDuration"
	CardTypeErrorCount  CardType = "errorCount"
	CardTypeErrorRate   CardType = "errorRate"
	CardTypeRps         CardType = "rps"
	CardTypeSlowCount   CardType = "slowCount"
)

type ServiceCard struct {
	Name  string
	Value float64
	Unit  string
}

type BaseCard struct {
	StartTime int64
	EndTime   int64
	TenantId  string
	ServiceId string
	Layer     common.TransactionLayerType
	LayerPath string
	FuzzyPath bool
	Metric    metricpb.MetricServiceServer
}

func (b *BaseCard) QueryAsServiceCard(ctx context.Context, statement string, params map[string]*structpb.Value, name string, unit string, postProcess func(value float64) float64) (*ServiceCard, error) {
	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(b.StartTime, 10),
		End:       strconv.FormatInt(b.EndTime, 10),
		Statement: statement,
		Params:    params,
	}
	response, err := b.Metric.QueryWithInfluxFormat(ctx, request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}

	rows := response.Results[0].Series[0].Rows
	if len(rows) == 0 || len(rows[0].Values) == 0 {
		return nil, errors.NewInternalServerErrorMessage("empty query result")
	}

	val := rows[0].Values[0].GetNumberValue()
	if postProcess != nil {
		val = postProcess(val)
	}

	return &ServiceCard{
		Name:  name,
		Unit:  unit,
		Value: val,
	}, nil
}

func GetCard(ctx context.Context, cardType CardType, baseCard *BaseCard) (*ServiceCard, error) {
	var builder Card
	switch cardType {
	case CardTypeReqCount:
		builder = &ReqCountCard{BaseCard: baseCard}
	case CardTypeAvgDuration:
		builder = &AvgDurationCard{BaseCard: baseCard}
	case CardTypeErrorCount:
		builder = &ErrorCountCard{BaseCard: baseCard}
	case CardTypeErrorRate:
		builder = &ErrorRateCard{BaseCard: baseCard}
	case CardTypeRps:
		builder = &RpsCard{BaseCard: baseCard}
	case CardTypeSlowCount:
		builder = &SlowCountCard{BaseCard: baseCard}
	default:
		return nil, fmt.Errorf("not supported cardType: %v", cardType)
	}

	return builder.GetCard(ctx)
}
