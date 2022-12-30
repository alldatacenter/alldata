/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.rest;

import org.jboss.logging.Logger;
import org.jboss.resteasy.client.exception.ResteasyWebApplicationException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ResteasyWebApplicationExceptionMapper implements ExceptionMapper<ResteasyWebApplicationException> {

    private static final Logger LOGGER = Logger.getLogger(ResteasyWebApplicationExceptionMapper.class);

    @Override
    public Response toResponse(ResteasyWebApplicationException e) {
        LOGGER.warn(e.getMessage(), e);
        return Response.fromResponse(e.unwrap().getResponse()).type(MediaType.APPLICATION_JSON).build();
    }
}
