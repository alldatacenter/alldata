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

package common

const Layout = "2006-01-02 15:04:05"

type Void struct{}

const (
	// CallAnalysisHttpClient http-client
	CallAnalysisHttpClient = "span_call_analysis_http_client"
	// CallAnalysisHttpServer http-server
	CallAnalysisHttpServer = "span_call_analysis_http_server"
	// CallAnalysisRpcClient rpc-client
	CallAnalysisRpcClient = "span_call_analysis_rpc_client"
	// CallAnalysisRpcServer rpc-server
	CallAnalysisRpcServer = "span_call_analysis_rpc_server"
	// CallAnalysisCacheClient cache client
	CallAnalysisCacheClient = "span_call_analysis_cache_client"
	// CallAnalysisMqProducer mq producer
	CallAnalysisMqProducer = "span_call_analysis_mq_producer"
	// CallAnalysisMqConsumer mq consumer
	CallAnalysisMqConsumer = "span_call_analysis_mq_consumer"
	// CallAnalysisInvokeLocal invoke local
	CallAnalysisInvokeLocal = "span_call_analysis_invoke_local"
)
