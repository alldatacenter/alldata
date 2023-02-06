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

package trace

import (
	"context"
	"fmt"
	"log"
	"net/url"
	"os"
	"strconv"
	"time"

	"go.opentelemetry.io/otel/attribute"
	"go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracehttp"
	stdout "go.opentelemetry.io/otel/exporters/stdout/stdouttrace"
	"go.opentelemetry.io/otel/sdk/resource"
	sdktrace "go.opentelemetry.io/otel/sdk/trace"
	semconv "go.opentelemetry.io/otel/semconv/v1.4.0"

	traceinject "github.com/erda-project/erda-infra/pkg/trace/inject"
	"github.com/erda-project/erda/pkg/common/entrance"
)

func getServiceName() string {
	for _, name := range []string{
		os.Getenv("DICE_COMPONENT"),
		entrance.GetAppName(),
	} {
		if len(name) > 0 {
			return name
		}
	}
	return "" // unknown
}

func getResourceAttributes() []attribute.KeyValue {
	attrs := []attribute.KeyValue{
		semconv.ServiceNameKey.String(getServiceName()),
	}

	addEnvIfExist := func(key attribute.Key, env ...string) {
		for _, k := range env {
			value := os.Getenv(k)
			if len(value) > 0 {
				attrs = append(attrs, key.String(value))
				return
			}
		}
	}

	// Not required
	addEnvIfExist(semconv.K8SNamespaceNameKey, "POD_NAMESPACE", "DICE_NAMESPACE")
	addEnvIfExist(semconv.K8SPodNameKey, "POD_NAME")
	addEnvIfExist(semconv.K8SPodUIDKey, "POD_UUID")
	addEnvIfExist(attribute.Key("k8s.pod.ip"), "POD_IP")
	addEnvIfExist(semconv.K8SClusterNameKey, "DICE_CLUSTER_NAME")
	addEnvIfExist(attribute.Key("host.ip"), "HOST_IP")

	// Not required
	addEnvIfExist(attribute.Key("erda.org.id"), "DICE_ORG_ID")
	addEnvIfExist(attribute.Key("erda.org.name"), "ERDA_ORG", "DICE_ORG_NAME")
	addEnvIfExist(attribute.Key("erda.project.id"), "DICE_PROJECT_ID", "DICE_PROJECT")
	addEnvIfExist(attribute.Key("erda.project.name"), "DICE_PROJECT_NAME")
	addEnvIfExist(attribute.Key("erda.application.id"), "DICE_APPLICATION_ID", "DICE_APPLICATION")
	addEnvIfExist(attribute.Key("erda.application.name"), "DICE_APPLICATION_NAME")
	addEnvIfExist(attribute.Key("erda.runtime.id"), "DICE_RUNTIME_ID", "DICE_RUNTIME")
	addEnvIfExist(attribute.Key("erda.application.name"), "DICE_RUNTIME_NAME")
	addEnvIfExist(attribute.Key("erda.workspace"), "DICE_WORKSPACE")
	addEnvIfExist(attribute.Key("erda.env.id"), "ERDA_ENV_ID", "TERMINUS_KEY")
	addEnvIfExist(semconv.ServiceVersionKey, "DICE_VERSION")
	return attrs
}

func getSamplingRate() (rate float64) {
	rate = 0.1
	envValue := os.Getenv("OTEL_TRACES_SAMPLER_ARG")
	if len(envValue) > 0 {
		v, err := strconv.ParseFloat(envValue, 64)
		if err == nil {
			rate = v
		}
	}
	return rate
}

var (
	debug         = os.Getenv("OTEL_TRACES_DEBUG") == "true"
	tracesEnabled = os.Getenv("OTEL_TRACES_ENABLED") == "true"
)

func newExporter() (exporter sdktrace.SpanExporter, err error) {
	if debug {
		return stdout.New(stdout.WithPrettyPrint())
	}

	headers := make(map[string]string)
	addHeaderIfExist := func(name string, keys ...string) {
		for _, key := range keys {
			value := os.Getenv(key)
			if len(value) > 0 {
				headers[name] = value
				return
			}
		}
	}

	// Required
	addHeaderIfExist("x-erda-env-id", "ERDA_ENV_ID", "TERMINUS_KEY")
	addHeaderIfExist("x-erda-env-token", "ERDA_ENV_TOKEN")
	addHeaderIfExist("x-erda-org", "ERDA_ORG", "DICE_ORG_NAME")

	opts := []otlptracehttp.Option{
		// otlptracehttp.WithCompression(otlptracehttp.GzipCompression),
		otlptracehttp.WithRetry(otlptracehttp.RetryConfig{
			Enabled: false,
		}),
		otlptracehttp.WithTimeout(60 * time.Second),
		otlptracehttp.WithHeaders(headers),
	}
	endpoint, err := getEndpointOptions()
	if err != nil {
		return nil, err
	}
	opts = append(opts, endpoint...)
	return otlptracehttp.New(context.Background(), opts...)
}

func getEndpointOptions() ([]otlptracehttp.Option, error) {
	opts := []otlptracehttp.Option{
		otlptracehttp.WithURLPath("/api/otlp/v1/traces"),
	}
	otlpEndpoint := os.Getenv("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT")
	if len(otlpEndpoint) > 0 {
		u, err := url.Parse(otlpEndpoint)
		if err != nil {
			return nil, fmt.Errorf("invalid OTEL_EXPORTER_OTLP_TRACES_ENDPOINT: %w", err)
		}
		opts = append(opts, otlptracehttp.WithEndpoint(u.Host), otlptracehttp.WithURLPath(u.Path))
		if u.Scheme != "https" {
			opts = append(opts, otlptracehttp.WithInsecure())
		}
	} else {
		isEdge, _ := strconv.ParseBool(os.Getenv("DICE_IS_EDGE"))

		if isEdge {
			collectorURL := os.Getenv("COLLECTOR_PUBLIC_URL")
			if len(collectorURL) <= 0 {
				return nil, fmt.Errorf("not found COLLECTOR_PUBLIC_URL")
			}
			u, err := url.Parse(collectorURL)
			if err != nil {
				return nil, fmt.Errorf("invalid COLLECTOR_PUBLIC_URL: %w", err)
			}
			opts = append(opts, otlptracehttp.WithEndpoint(u.Host))
			if u.Scheme != "https" {
				opts = append(opts, otlptracehttp.WithInsecure())
			}
		} else {
			collectorAddr := os.Getenv("COLLECTOR_ADDR")
			if len(collectorAddr) <= 0 {
				return nil, fmt.Errorf("not found COLLECTOR_ADDR")
			}
			opts = append(opts,
				otlptracehttp.WithEndpoint(collectorAddr),
				otlptracehttp.WithInsecure(),
			)
		}
	}

	return opts, nil
}

func Init() {
	if !tracesEnabled {
		log.Println("[INFO] Opentelemetry traces exporter disabled ")
		return
	}
	exporter, err := newExporter()
	if err != nil {
		log.Printf("[ERROR] failed to initialize trace exporter %s\n", err)
		return
	}

	sampler := sdktrace.ParentBased(sdktrace.TraceIDRatioBased(getSamplingRate()))

	traceinject.Init(sdktrace.WithSampler(sampler),
		sdktrace.WithBatcher(exporter),
		sdktrace.WithResource(resource.NewWithAttributes(semconv.SchemaURL, getResourceAttributes()...)),
	)
}
