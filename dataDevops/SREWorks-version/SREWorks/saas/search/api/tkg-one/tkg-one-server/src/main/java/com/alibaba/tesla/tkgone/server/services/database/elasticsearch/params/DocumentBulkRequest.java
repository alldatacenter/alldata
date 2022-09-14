package com.alibaba.tesla.tkgone.server.services.database.elasticsearch.params;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author yangjinghua
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentBulkRequest {

    String index;
    String id;
    Map<String, Object> document;

}
