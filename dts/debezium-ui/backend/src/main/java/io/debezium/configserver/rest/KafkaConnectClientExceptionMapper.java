/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.rest;

import io.debezium.configserver.rest.model.ServerError;
import io.debezium.configserver.service.StacktraceHelper;
import org.jboss.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class KafkaConnectClientExceptionMapper implements ExceptionMapper<KafkaConnectClientException> {

    private static final Logger LOGGER = Logger.getLogger(KafkaConnectClientExceptionMapper.class);

    @Override
    public Response toResponse(KafkaConnectClientException e) {
        LOGGER.error(e.getMessage(), e);
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(new ServerError(e.getMessage(), StacktraceHelper.traceAsString(e))).build();
    }
}
