/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.common;

import java.util.LinkedList;

/**
 * Process events serially.<br>
 * <br>
 * Our use of listeners that deliver events directly can sometimes
 * cause problems when events are delivered recursively in the middle of
 * event handling by the same object. This helper class can be used to
 * serialize events in such cases.<br>
 * <br>
 * All events are queued until {@link #start()} is called.
 * The thread that calls {@link #start()} will process all events in the order they
 * were added until the queue is empty. Other threads will just enqueue their events.<br>
 * When the queue is empty, the first thread that adds an event will start processing
 * the queue until it's empty again.
 *
 * @param <T> the event class
 */
public abstract class EventProcessor<T> {
  private final LinkedList<T> queuedEvents = new LinkedList<>();
  private volatile boolean isProcessing = false;
  private volatile boolean started = false;

  /**
   * Send an event to the processor. the event will be queued to be processed after
   * any prior events are processed, once processing actually starts.
   *
   * <p>If an event's processing causes an exception, it will be added to any
   * previous exceptions as a suppressed exception. Once all the currently queued
   * events have been processed, a single exception will be thrown.</p>
   *
   * @param newEvent the new event
   *
   * @throws RuntimeException if any exception is thrown while events are being processed
   */
  public void sendEvent(final T newEvent) {
    synchronized (queuedEvents) {
      queuedEvents.addLast(newEvent);
      if (!started || isProcessing) {
        return;
      }

      isProcessing = true;
    }

    processEvents();
  }

  /**
   * Start processing events as soon as the queue isn't empty.<br>
   * If the queue is not empty, this method will process all events already
   * in the queue and any event that will be added while the queue is being processed.
   *
   * @throws RuntimeException if any exception is thrown while events are being processed
   */
  public void start() {
    synchronized (queuedEvents) {
      if (started) {
        return;
      }

      started = true;
      isProcessing = true;
    }

    processEvents();
  }

  /**
   * Process all events in the queue until it's empty.
   */
  private void processEvents() {
    @SuppressWarnings("resource")
    final DeferredException deferredException = new DeferredException();
    while (true) {
      T event;

      synchronized (queuedEvents) {
        if (queuedEvents.isEmpty()) {
          isProcessing = false;
          break;
        }

        event = queuedEvents.removeFirst();
      }

      try {
        processEvent(event);
      } catch (Exception e) {
        deferredException.addException(e);
      } catch (AssertionError ae) {
        deferredException.addException(new RuntimeException("Caught an assertion", ae));
      }
    }

    try {
      deferredException.close();
    } catch(Exception e) {
      throw new RuntimeException("Exceptions caught during event processing", e);
    }
  }

  /**
   * Process a single event. Derived classes provide the implementation of this
   * to process events.
   *
   * @param event the event to process
   */
  protected abstract void processEvent(T event);
}
