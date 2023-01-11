/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.authorization.elasticsearch.plugin.utils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.cluster.shards.ClusterSearchShardsRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheRequest;
import org.elasticsearch.action.admin.indices.close.CloseIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsRequest;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.flush.SyncedFlushRequest;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.admin.indices.recovery.RecoveryRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.rollover.RolloverRequest;
import org.elasticsearch.action.admin.indices.segments.IndicesSegmentsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.admin.indices.shards.IndicesShardStoresRequest;
import org.elasticsearch.action.admin.indices.shrink.ResizeRequest;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequest;
import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesRequest;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.action.admin.indices.upgrade.get.UpgradeStatusRequest;
import org.elasticsearch.action.admin.indices.upgrade.post.UpgradeRequest;
import org.elasticsearch.action.admin.indices.validate.query.ValidateQueryRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.fieldcaps.FieldCapabilitiesRequest;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetRequest.Item;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.replication.ReplicationRequest;
import org.elasticsearch.action.support.single.instance.InstanceShardOperationRequest;
import org.elasticsearch.action.support.single.shard.SingleShardRequest;
import org.elasticsearch.action.termvectors.MultiTermVectorsRequest;
import org.elasticsearch.action.termvectors.TermVectorsRequest;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.ReindexRequest;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.rest.RestRequest;

public class RequestUtils {
	public static final String CLIENT_IP_ADDRESS = "ClientIPAddress";

	public static String getClientIPAddress(RestRequest request) {
		SocketAddress socketAddress = request.getHttpChannel().getRemoteAddress();
		if (socketAddress instanceof InetSocketAddress) {
			return ((InetSocketAddress) socketAddress).getAddress().getHostAddress();
		}

		return null;
	}

	// To support all kinds of request in elasticsearch
	public static <Request extends ActionRequest> List<String> getIndexFromRequest(Request request) {
		List<String> indexs = new ArrayList<>();

		if (request instanceof SingleShardRequest) {
			indexs.add(((SingleShardRequest<?>) request).index());
			return indexs;
		}

		if (request instanceof ReplicationRequest) {
			indexs.add(((ReplicationRequest<?>) request).index());
			return indexs;
		}

		if (request instanceof InstanceShardOperationRequest) {
			indexs.add(((InstanceShardOperationRequest<?>) request).index());
			return indexs;
		}

		if (request instanceof CreateIndexRequest) {
			indexs.add(((CreateIndexRequest) request).index());
			return indexs;
		}

		if (request instanceof PutMappingRequest) {
			if (((PutMappingRequest) request).getConcreteIndex() != null) {
				indexs.add(((PutMappingRequest) request).getConcreteIndex().getName());
				return indexs;
			} else {
				return Arrays.asList(((PutMappingRequest) request).indices());
			}
		}

		if (request instanceof SearchRequest) {
			return Arrays.asList(((SearchRequest) request).indices());
		}

		if (request instanceof IndicesStatsRequest) {
			return Arrays.asList(((IndicesStatsRequest) request).indices());
		}

		if (request instanceof OpenIndexRequest) {
			return Arrays.asList(((OpenIndexRequest) request).indices());
		}

		if (request instanceof DeleteIndexRequest) {
			return Arrays.asList(((DeleteIndexRequest) request).indices());
		}

		if (request instanceof BulkRequest) {
			@SuppressWarnings("rawtypes") List<DocWriteRequest<?>> requests = ((BulkRequest) request).requests();

			if (CollectionUtils.isNotEmpty(requests)) {
				for (DocWriteRequest<?> docWriteRequest : requests) {
					indexs.add(docWriteRequest.index());
				}
				return indexs;
			}
		}

		if (request instanceof MultiGetRequest) {
			List<Item> items = ((MultiGetRequest) request).getItems();
			if (CollectionUtils.isNotEmpty(items)) {
				for (Item item : items) {
					indexs.add(item.index());
				}
				return indexs;
			}
		}

		if (request instanceof GetMappingsRequest) {
			return Arrays.asList(((GetMappingsRequest) request).indices());
		}

		if (request instanceof GetSettingsRequest) {
			return Arrays.asList(((GetSettingsRequest) request).indices());
		}

		if (request instanceof IndicesExistsRequest) {
			return Arrays.asList(((IndicesExistsRequest) request).indices());
		}

		if (request instanceof GetAliasesRequest) {
			return Arrays.asList(((GetAliasesRequest) request).indices());
		}

		if (request instanceof GetIndexRequest) {
			return Arrays.asList(((GetIndexRequest) request).indices());
		}

		if (request instanceof GetFieldMappingsRequest) {
			return Arrays.asList(((GetFieldMappingsRequest) request).indices());
		}

		if (request instanceof TypesExistsRequest) {
			return Arrays.asList(((TypesExistsRequest) request).indices());
		}

		if (request instanceof ValidateQueryRequest) {
			return Arrays.asList(((ValidateQueryRequest) request).indices());
		}

		if (request instanceof RecoveryRequest) {
			return Arrays.asList(((RecoveryRequest) request).indices());
		}

		if (request instanceof IndicesSegmentsRequest) {
			return Arrays.asList(((IndicesSegmentsRequest) request).indices());
		}

		if (request instanceof IndicesShardStoresRequest) {
			return Arrays.asList(((IndicesShardStoresRequest) request).indices());
		}

		if (request instanceof UpgradeStatusRequest) {
			return Arrays.asList(((UpgradeStatusRequest) request).indices());
		}

		if (request instanceof ClusterSearchShardsRequest) {
			return Arrays.asList(((ClusterSearchShardsRequest) request).indices());
		}

		if (request instanceof IndicesAliasesRequest) {
			List<IndicesAliasesRequest.AliasActions> aliasActions = ((IndicesAliasesRequest) request).getAliasActions();
			if (CollectionUtils.isNotEmpty(aliasActions)) {
				for (IndicesAliasesRequest.AliasActions action : aliasActions) {
					indexs.addAll(Arrays.asList(action.indices()));
				}
				return indexs;
			}
		}

		if (request instanceof ClearIndicesCacheRequest) {
			return Arrays.asList(((ClearIndicesCacheRequest) request).indices());
		}

		if (request instanceof CloseIndexRequest) {
			return Arrays.asList(((CloseIndexRequest) request).indices());
		}

		if (request instanceof FlushRequest) {
			return Arrays.asList(((FlushRequest) request).indices());
		}

		if (request instanceof SyncedFlushRequest) {
			return Arrays.asList(((SyncedFlushRequest) request).indices());
		}

		if (request instanceof ForceMergeRequest) {
			return Arrays.asList(((ForceMergeRequest) request).indices());
		}

		if (request instanceof RefreshRequest) {
			return Arrays.asList(((RefreshRequest) request).indices());
		}

		if (request instanceof RolloverRequest) {
			return Arrays.asList(((RolloverRequest) request).indices());
		}

		if (request instanceof UpdateSettingsRequest) {
			return Arrays.asList(((UpdateSettingsRequest) request).indices());
		}

		if (request instanceof ResizeRequest) {
			return Arrays.asList(((ResizeRequest) request).indices());
		}

		if (request instanceof DeleteIndexTemplateRequest) {
			indexs.add(((DeleteIndexTemplateRequest) request).name());
			return indexs;
		}

		if (request instanceof GetIndexTemplatesRequest) {
			return Arrays.asList(((GetIndexTemplatesRequest) request).names());
		}

		if (request instanceof PutIndexTemplateRequest) {
			indexs.add(((PutIndexTemplateRequest) request).name());
			return indexs;
		}

		if (request instanceof UpgradeRequest) {
			return Arrays.asList(((UpgradeRequest) request).indices());
		}

		if (request instanceof FieldCapabilitiesRequest) {
			return Arrays.asList(((FieldCapabilitiesRequest) request).indices());
		}

		if (request instanceof MultiSearchRequest) {
			List<SearchRequest> searchRequests = ((MultiSearchRequest) request).requests();
			if (CollectionUtils.isNotEmpty(searchRequests)) {
				for (SearchRequest singleRequest : searchRequests) {
					indexs.addAll(Arrays.asList(singleRequest.indices()));
				}
				return indexs;
			}
		}

		if (request instanceof MultiTermVectorsRequest) {
			List<TermVectorsRequest> termVectorsRequests = ((MultiTermVectorsRequest) request).getRequests();
			if (CollectionUtils.isNotEmpty(termVectorsRequests)) {
				for (TermVectorsRequest singleRequest : termVectorsRequests) {
					indexs.addAll(Arrays.asList(singleRequest.indices()));
				}
				return indexs;
			}
		}

		if (request instanceof UpdateByQueryRequest) {
			return Arrays.asList(((UpdateByQueryRequest) request).indices());
		}

		if (request instanceof DeleteByQueryRequest) {
			return Arrays.asList(((DeleteByQueryRequest) request).indices());
		}

		if (request instanceof ReindexRequest) {
			indexs.addAll(Arrays.asList(((ReindexRequest) request).getSearchRequest().indices()));
			indexs.addAll(Arrays.asList(((ReindexRequest) request).getDestination().indices()));
			return indexs;
		}

		//ClearScrollRequest does not carry any index, so return empty List
		if (request instanceof ClearScrollRequest) {
			return indexs;
		}

		// No matched request type to find specific index , set default value *
		indexs.add("*");
		return indexs;
	}
}
