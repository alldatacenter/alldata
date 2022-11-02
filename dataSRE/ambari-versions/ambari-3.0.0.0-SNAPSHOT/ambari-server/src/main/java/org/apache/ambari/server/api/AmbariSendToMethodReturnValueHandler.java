/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.api;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.annotation.support.DestinationVariableMethodArgumentResolver;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.support.MissingSessionUserException;
import org.springframework.messaging.simp.annotation.support.SendToMethodReturnValueHandler;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PropertyPlaceholderHelper;

public class AmbariSendToMethodReturnValueHandler extends SendToMethodReturnValueHandler {
  private final SimpMessageSendingOperations messagingTemplate;
  private final boolean annotationRequired;
  private PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("{", "}", null, false);
  private String defaultUserDestinationPrefix = "/queue";
  private String defaultDestinationPrefix = "/topic";
  public static final String CORRELATION_ID_HEADER = "correlationId";
  public static final String NATIVE_HEADERS = "nativeHeaders";
  public AmbariSendToMethodReturnValueHandler(SimpMessageSendingOperations messagingTemplate, boolean annotationRequired) {
    super(messagingTemplate, annotationRequired);
    this.messagingTemplate = messagingTemplate;
    this.annotationRequired = annotationRequired;
  }

  @Override
  public void handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> message) throws Exception {
    if (returnValue == null) {
      return;
    }

    MessageHeaders headers = message.getHeaders();
    String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
    PropertyPlaceholderHelper.PlaceholderResolver varResolver = initVarResolver(headers);
    Object annotation = findAnnotation(returnType);

    String correlationId = getCorrelationId(message);
    if (annotation != null && annotation instanceof SendToUser) {
      SendToUser sendToUser = (SendToUser) annotation;
      boolean broadcast = sendToUser.broadcast();
      String user = getUserName(message, headers);
      if (user == null) {
        if (sessionId == null) {
          throw new MissingSessionUserException(message);
        }
        user = sessionId;
        broadcast = false;
      }
      String[] destinations = getTargetDestinations(sendToUser, message, this.defaultUserDestinationPrefix);
      for (String destination : destinations) {
        destination = this.placeholderHelper.replacePlaceholders(destination, varResolver);
        if (broadcast) {
          this.messagingTemplate.convertAndSendToUser(
            user, destination, returnValue, createHeaders(null, returnType, correlationId));
        }
        else {
          this.messagingTemplate.convertAndSendToUser(
            user, destination, returnValue, createHeaders(sessionId, returnType, correlationId));
        }
      }
    }
    else {
      SendTo sendTo = (SendTo) annotation;
      String[] destinations = getTargetDestinations(sendTo, message, this.defaultDestinationPrefix);
      for (String destination : destinations) {
        destination = this.placeholderHelper.replacePlaceholders(destination, varResolver);
        this.messagingTemplate.convertAndSend(destination, returnValue, createHeaders(sessionId, returnType, correlationId));
      }
    }
  }

  private String getCorrelationId(Message<?> message) {
    SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(message);
    return headerAccessor.getFirstNativeHeader(CORRELATION_ID_HEADER);
  }

  private MessageHeaders createHeaders(String sessionId, MethodParameter returnType, String correlationId) {
    SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
    if (getHeaderInitializer() != null) {
      getHeaderInitializer().initHeaders(headerAccessor);
    }
    if (sessionId != null) {
      headerAccessor.setSessionId(sessionId);
    }
    headerAccessor.setHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER, returnType);
    headerAccessor.setLeaveMutable(true);
    headerAccessor.addNativeHeader(CORRELATION_ID_HEADER, correlationId);
    return headerAccessor.getMessageHeaders();
  }

  private Object findAnnotation(MethodParameter returnType) {
    Annotation[] anns = new Annotation[4];
    anns[0] = AnnotatedElementUtils.findMergedAnnotation(returnType.getMethod(), SendToUser.class);
    anns[1] = AnnotatedElementUtils.findMergedAnnotation(returnType.getMethod(), SendTo.class);
    anns[2] = AnnotatedElementUtils.findMergedAnnotation(returnType.getDeclaringClass(), SendToUser.class);
    anns[3] = AnnotatedElementUtils.findMergedAnnotation(returnType.getDeclaringClass(), SendTo.class);

    if (anns[0] != null && !ObjectUtils.isEmpty(((SendToUser) anns[0]).value())) {
      return anns[0];
    }
    if (anns[1] != null && !ObjectUtils.isEmpty(((SendTo) anns[1]).value())) {
      return anns[1];
    }
    if (anns[2] != null && !ObjectUtils.isEmpty(((SendToUser) anns[2]).value())) {
      return anns[2];
    }
    if (anns[3] != null && !ObjectUtils.isEmpty(((SendTo) anns[3]).value())) {
      return anns[3];
    }

    for (int i=0; i < 4; i++) {
      if (anns[i] != null) {
        return anns[i];
      }
    }

    return null;
  }

  private PropertyPlaceholderHelper.PlaceholderResolver initVarResolver(MessageHeaders headers) {
    String name = DestinationVariableMethodArgumentResolver.DESTINATION_TEMPLATE_VARIABLES_HEADER;
    Map<String, String> vars = (Map<String, String>) headers.get(name);
    return new DestinationVariablePlaceholderResolver(vars);
  }

  @Override
  public String toString() {
    return "AmbariSendToMethodReturnValueHandler [annotationRequired=" + annotationRequired + "]";
  }

  private static class DestinationVariablePlaceholderResolver implements PropertyPlaceholderHelper.PlaceholderResolver {

    private final Map<String, String> vars;

    public DestinationVariablePlaceholderResolver(Map<String, String> vars) {
      this.vars = vars;
    }

    @Override
    public String resolvePlaceholder(String placeholderName) {
      return (this.vars != null ? this.vars.get(placeholderName) : null);
    }
  }
}
