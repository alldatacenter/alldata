/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.events;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.HostNotRegisteredException;
import org.apache.ambari.server.agent.AgentSessionManager;
import org.apache.ambari.server.agent.stomp.dto.AckReport;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.utils.ScheduledExecutorCompletionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Is used to define a strategy for emitting message to subscribers.
 */
public abstract class MessageEmitter {
  protected static final AtomicLong MESSAGE_ID = new AtomicLong(0);
  private final static Logger LOG = LoggerFactory.getLogger(MessageEmitter.class);
  public final int retryCount;
  public final int retryInterval;
  protected final AgentSessionManager agentSessionManager;
  protected final SimpMessagingTemplate simpMessagingTemplate;
  protected final ScheduledExecutorService emitExecutor = Executors.newScheduledThreadPool(10,
      new ThreadFactoryBuilder().setNameFormat("agent-message-emitter-%d").build());
  protected final ExecutorService monitorExecutor = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("agent-message-monitor-%d").build());
  protected final ExecutorService retryExecutor = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("agent-message-retry-%d").build());
  protected final ScheduledExecutorCompletionService<EmitTaskWrapper> emitCompletionService =
      new ScheduledExecutorCompletionService(emitExecutor, new LinkedBlockingQueue<>());
  protected ConcurrentHashMap<Long, EmitTaskWrapper> unconfirmedMessages = new ConcurrentHashMap<>();
  protected ConcurrentHashMap<Long, BlockingQueue<EmitTaskWrapper>> messagesToEmit = new ConcurrentHashMap<>();
  private AmbariEventPublisher ambariEventPublisher;

  public MessageEmitter(AgentSessionManager agentSessionManager, SimpMessagingTemplate simpMessagingTemplate,
                        AmbariEventPublisher ambariEventPublisher, int retryCount, int retryInterval) {
    this.agentSessionManager = agentSessionManager;
    this.simpMessagingTemplate = simpMessagingTemplate;
    this.ambariEventPublisher = ambariEventPublisher;
    this.retryCount = retryCount;
    this.retryInterval = retryInterval;
    ambariEventPublisher.register(this);
    monitorExecutor.execute(new MessagesToEmitMonitor());
    retryExecutor.execute(new MessagesToRetryMonitor());
  }

  /**
   * Determines destinations and emits message.
   *
   * @param event message should to be emitted.
   * @throws AmbariException
   */
  abstract void emitMessage(STOMPEvent event) throws AmbariException;

  public void emitMessageRetriable(ExecutionCommandEvent event) {
    // set message identifier used to recognize NACK/ACK agent response
    EmitTaskWrapper wrapper = new EmitTaskWrapper(0, MESSAGE_ID.getAndIncrement(), event);

    Long hostId = event.getHostId();
    messagesToEmit.compute(hostId, (id, hostMessages) -> {
      if (hostMessages == null) {
        LOG.error("Trying to emit message to unregistered host with id {}", hostId);
        return null;
      } else {
        hostMessages.add(wrapper);
        return hostMessages;
      }
    });
  }

  public void processReceiveReport(Long hostId, AckReport ackReport) {
    Long messageId = ackReport.getMessageId();
    if (AckReport.AckStatus.OK.equals(ackReport.getStatus())) {
      unconfirmedMessages.compute(hostId, (id, commandInUse) -> {
        if (commandInUse != null && commandInUse.getMessageId().equals(ackReport.getMessageId())) {
          return null;
        } else {
          LOG.warn("OK agent report was received again for already complete command with message id {}", messageId);
        }
        return commandInUse;
      });
    } else {
      LOG.error("Received {} agent report for execution command with messageId {} with following reason: {}",
          ackReport.getStatus(), messageId, ackReport.getReason());
    }
  }

  protected abstract String getDestination(STOMPEvent stompEvent);

  /**
   * Creates STOMP message header.
   *
   * @param sessionId
   * @return message header.
   */
  protected MessageHeaders createHeaders(String sessionId) {
    return createHeaders(sessionId, null);
  }

  /**
   * Creates STOMP message header.
   *
   * @param sessionId
   * @return message header.
   */
  protected MessageHeaders createHeaders(String sessionId, Long messageId) {
    SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
    headerAccessor.setSessionId(sessionId);
    headerAccessor.setLeaveMutable(true);
    if (messageId != null) {
      headerAccessor.setNativeHeader("messageId", Long.toString(messageId));
    }
    return headerAccessor.getMessageHeaders();
  }

  /**
   * Emits message to all subscribers.
   *
   * @param event message should to be emitted.
   */
  protected void emitMessageToAll(STOMPEvent event) {
    LOG.debug("Received status update event {}", event);
    simpMessagingTemplate.convertAndSend(getDestination(event), event);
  }

  /**
   * Emit message to specified host only.
   *
   * @param event message should to be emitted.
   * @throws HostNotRegisteredException in case host is not registered.
   */
  protected void emitMessageToHost(STOMPHostEvent event) throws HostNotRegisteredException {
    Long hostId = event.getHostId();
    String sessionId = agentSessionManager.getSessionId(hostId);
    LOG.debug("Received status update event {} for host {} registered with session ID {}", event, hostId, sessionId);
    MessageHeaders headers = createHeaders(sessionId);
    simpMessagingTemplate.convertAndSendToUser(sessionId, getDestination(event), event, headers);
  }

  /**
   * Emit execution command to specified host only.
   *
   * @param eventWrapper message should to be emitted.
   * @throws HostNotRegisteredException in case host is not registered.
   */
  protected void emitExecutionCommandToHost(EmitTaskWrapper eventWrapper) throws HostNotRegisteredException {
    ExecutionCommandEvent event = eventWrapper.getExecutionCommandEvent();
    Long hostId = event.getHostId();
    Long messageId = eventWrapper.getMessageId();
    String sessionId = agentSessionManager.getSessionId(hostId);
    LOG.debug("Received status update event {} for host {} registered with session ID {}", event, hostId, sessionId);
    MessageHeaders headers = createHeaders(sessionId, messageId);
    simpMessagingTemplate.convertAndSendToUser(sessionId, getDestination(event), event, headers);
  }

  @Subscribe
  public void onHostRegister(HostRegisteredEvent hostRegisteredEvent) {
    Long hostId = hostRegisteredEvent.getHostId();
    messagesToEmit.computeIfAbsent(hostId, id -> new LinkedBlockingQueue<>());
  }

  /**
   * Is used for first emit of arrived messages. There is a single command in process per host at each time.
   * Host will be released on agent ACK response receiving or {@link MessageNotDelivered} event firing.
   * In case no new operations were emitted thread will sleep for a small time.
   */
  private class MessagesToEmitMonitor implements Runnable {

    /**
     * Is used to check any message was emitted over available hosts.
     */
    private boolean anyActionPerformed;

    @Override
    public void run() {
      while (true) {
        anyActionPerformed = false;
        for (Long hostId : messagesToEmit.keySet()) {
          unconfirmedMessages.computeIfAbsent(hostId, id -> {
            EmitTaskWrapper event = messagesToEmit.get(hostId).poll();
            if (event != null) {
              LOG.info("Schedule execution command emitting, retry: {}, messageId: {}",
                  event.getRetryCounter(), event.getMessageId());
              emitCompletionService.submit(new EmitMessageTask(event, false));
              anyActionPerformed = true;
            }
            return event;
          });
        }
        if (!anyActionPerformed) {
          try {
            Thread.sleep(200);
          } catch (InterruptedException e) {
            LOG.error("Exception during sleep", e);
          }
        }
      }
    }
  }

  /**
   * After first emit completion message will be scheduled to re-emit with delay.
   * Re-emit task also should check was message already delivered.
   * After {@link MessageEmitter#retryCount} retries limit exceeded {@link MessageNotDelivered} event will be fired.
   */
  private class MessagesToRetryMonitor implements Runnable {

    @Override
    public void run() {
      while (true) {
        try {
          Future<EmitTaskWrapper> future = emitCompletionService.take();
          EmitTaskWrapper result = future.get();
          Long hostId = result.getExecutionCommandEvent().getHostId();
          unconfirmedMessages.compute(hostId, (id, commandInUse) -> {
            if (commandInUse != null && commandInUse.getMessageId().equals(result.getMessageId())) {
              if (result.getRetryCounter() < retryCount) {
                result.retry();
                LOG.warn("Reschedule execution command emitting, retry: {}, messageId: {}",
                    result.getRetryCounter(), result.getMessageId());
                emitCompletionService.schedule(new EmitMessageTask(result, true), retryInterval, TimeUnit.SECONDS);
              } else {
                ExecutionCommandEvent event = result.getExecutionCommandEvent();
                // remove commands queue for host
                messagesToEmit.remove(event.getHostId());

                // generate delivery failed event and cancel emitter
                ambariEventPublisher.publish(new MessageNotDelivered(event.getHostId()));

                return null;
              }
            }
            return commandInUse;
          });
        } catch (InterruptedException e) {
          LOG.error("Retry message emitting monitor was interrupted", e);
        } catch (ExecutionException e) {
          LOG.error("Exception during message emitting retry", e);
        }
      }
    }
  }

  /**
   * Task to emit command.
   */
  private class EmitMessageTask implements Callable<EmitTaskWrapper> {

    /**
     * Wrapped command to emit.
     */
    private final EmitTaskWrapper emitTaskWrapper;

    /**
     * Should {@link #call()} check for command was successfully emitted and ACK already received.
     */
    private final boolean checkRelevance;

    public EmitMessageTask(EmitTaskWrapper emitTaskWrapper, boolean checkRelevance) {
      this.emitTaskWrapper = emitTaskWrapper;
      this.checkRelevance = checkRelevance;
    }

    @Override
    public EmitTaskWrapper call() throws Exception {
      try {
        if (checkRelevance) {
          Long hostId = emitTaskWrapper.getExecutionCommandEvent().getHostId();
          EmitTaskWrapper commandInUse = unconfirmedMessages.get(hostId);

          // check ack was already received
          if (commandInUse != null && commandInUse.getMessageId().equals(emitTaskWrapper.getMessageId())) {
            emitExecutionCommandToHost(emitTaskWrapper);
          }
        } else {
          emitExecutionCommandToHost(emitTaskWrapper);
        }
      } catch (HostNotRegisteredException e) {
        LOG.error("Trying to emit execution command to unregistered host {} on attempt {}",
            emitTaskWrapper.getMessageId(), emitTaskWrapper.getRetryCounter(), e);
      }
      return emitTaskWrapper;
    }
  }

  private class EmitTaskWrapper {
    private final Long messageId;
    private final ExecutionCommandEvent executionCommandEvent;
    private final AtomicInteger retryCounter;

    public EmitTaskWrapper(int retryCounter, Long messageId, ExecutionCommandEvent executionCommandEvent) {
      this.retryCounter = new AtomicInteger(retryCounter);
      this.messageId = messageId;
      this.executionCommandEvent = executionCommandEvent;
    }

    public int getRetryCounter() {
      return retryCounter.get();
    }

    public ExecutionCommandEvent getExecutionCommandEvent() {
      return executionCommandEvent;
    }

    public Long getMessageId() {
      return messageId;
    }

    public void retry() {
      retryCounter.incrementAndGet();
    }
  }
}
