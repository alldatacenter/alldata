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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.inlong.audit.channel;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.flume.Channel;
import org.apache.flume.ChannelException;
import org.apache.flume.ChannelSelector;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.FlumeException;
import org.apache.flume.Transaction;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.interceptor.Interceptor;
import org.apache.flume.interceptor.InterceptorBuilderFactory;
import org.apache.flume.interceptor.InterceptorChain;
import org.apache.inlong.audit.utils.LogCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FailoverChannelProcessor
        extends ChannelProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(FailoverChannelProcessor.class);

    public static final String MSG_COUNTER_KEY = "msgcnt";

    private static final LogCounter logPrinter = new LogCounter(10, 10000, 60 * 1000);

    private final ChannelSelector selector;
    private final InterceptorChain interceptorChain;

    public FailoverChannelProcessor(ChannelSelector selector) {
        super(selector);
        this.selector = selector;
        this.interceptorChain = new InterceptorChain();
    }

    public void initialize() {
        interceptorChain.initialize();
    }

    public void close() {
        interceptorChain.close();
    }

    @Override
    public void configure(Context context) {
        configureInterceptors(context);
    }

    private void configureInterceptors(Context context) {

        List<Interceptor> interceptors = Lists.newLinkedList();

        String interceptorListStr = context.getString("interceptors", "");
        if (interceptorListStr.isEmpty()) {
            return;
        }
        String[] interceptorNames = interceptorListStr.split("\\s+");

        Context interceptorContexts = new Context(context.getSubProperties("interceptors."));

        // run through and instantiate all the interceptors specified in the Context
        InterceptorBuilderFactory factory = new InterceptorBuilderFactory();
        for (String interceptorName : interceptorNames) {
            Context interceptorContext =
                    new Context(interceptorContexts.getSubProperties(interceptorName + "."));
            String type = interceptorContext.getString("type");
            if (type == null) {
                LOG.error("Type not specified for interceptor " + interceptorName);
                throw new FlumeException("Interceptor.Type not specified for " + interceptorName);
            }
            try {
                Interceptor.Builder builder = factory.newInstance(type);
                builder.configure(interceptorContext);
                interceptors.add(builder.build());
            } catch (ClassNotFoundException e) {
                LOG.error("Builder class not found. Exception follows.", e);
                throw new FlumeException("Interceptor.Builder not found.", e);
            } catch (InstantiationException e) {
                LOG.error("Could not instantiate Builder. Exception follows.", e);
                throw new FlumeException("Interceptor.Builder not constructable.", e);
            } catch (IllegalAccessException e) {
                LOG.error("Unable to access Builder. Exception follows.", e);
                throw new FlumeException("Unable to access Interceptor.Builder.", e);
            }
        }

        interceptorChain.setInterceptors(interceptors);
    }

    public ChannelSelector getSelector() {
        return selector;
    }

    public void processEventBatch(List<Event> events) {
        Preconditions.checkNotNull(events, "Event list must not be null");
        events = interceptorChain.intercept(events);
        Map<Channel, List<Event>> reqChannelQueue = new LinkedHashMap<Channel, List<Event>>();
        Map<Channel, List<Event>> optChannelQueue = new LinkedHashMap<Channel, List<Event>>();

        long tMsgCounterL = 1L;
        for (Event event : events) {
            if (event.getHeaders().containsKey(MSG_COUNTER_KEY)) {
                tMsgCounterL += Long.parseLong(event.getHeaders().get(MSG_COUNTER_KEY));
            } else {
                tMsgCounterL += 1;
            }

            List<Channel> reqChannels = selector.getRequiredChannels(event);

            for (Channel ch : reqChannels) {
                List<Event> eventQueue = reqChannelQueue
                        .computeIfAbsent(ch, k -> new ArrayList<Event>());//reqChannelQueue
                eventQueue.add(event);
            }

            List<Channel> optChannels = selector.getOptionalChannels(event);

            for (Channel ch : optChannels) {
                List<Event> eventQueue = optChannelQueue
                        .computeIfAbsent(ch, k -> new ArrayList<Event>());//optChannelQueue

                eventQueue.add(event);
            }
        }

        boolean success = true;
        // Process memory channels
        for (Map.Entry<Channel, List<Event>> entry : reqChannelQueue.entrySet()) {
            Channel reqChannel = entry.getKey();
            Transaction tx = reqChannel.getTransaction();
            Preconditions.checkNotNull(tx, "Transaction object must not be null");
            try {
                tx.begin();
                List<Event> batch = entry.getValue();
                for (Event event : batch) {
                    reqChannel.put(event);
                }
                tx.commit();
            } catch (Throwable t) {
                success = false;
                tx.rollback();
                if (!(t instanceof ChannelException)) {
                    LOG.error("Unable to put batch on required " + "channel: " + reqChannel, t);
                    if (t instanceof Error) {
                        throw (Error) t;
                    }
                }
                break;
            } finally {
                tx.close();
            }
        }

        if (!success) {
            // Process file channels
            for (Map.Entry<Channel, List<Event>> entry : optChannelQueue.entrySet()) {
                Channel optChannel = entry.getKey();
                Transaction tx = optChannel.getTransaction();
                Preconditions.checkNotNull(tx, "Transaction object must not be null");
                try {
                    tx.begin();

                    List<Event> batch = entry.getValue();

                    for (Event event : batch) {
                        optChannel.put(event);
                    }

                    tx.commit();

                } catch (Throwable t) {
                    tx.rollback();
                    if (t instanceof Error) {
                        LOG.error("Error while writing to optChannel channel: " + optChannel, t);
                        throw (Error) t;
                    } else {
                        throw new ChannelException(
                                "Unable to put batch on optChannel " + "channel: " + optChannel, t);
                    }
                } finally {
                    tx.close();
                }
            }
        }
    }

    public void processEvent(Event event) {
        event = interceptorChain.intercept(event);
        if (event == null) {
            return;
        }

        boolean success = true;
        // Process required channels
        List<Channel> requiredChannels = selector.getRequiredChannels(event);

        for (Channel reqChannel : requiredChannels) {
            Transaction tx = reqChannel.getTransaction();
            Preconditions.checkNotNull(tx, "Transaction object must not be null");
            try {
                tx.begin();

                reqChannel.put(event);

                tx.commit();

            } catch (Throwable t) {
                if (logPrinter.shouldPrint()) {
                    LOG.error("FailoverChannelProcessor Unable to put event on required channel: {}"
                            + ",exception = {}", reqChannel.getName(), t);
                }

                success = false;
                try {
                    tx.rollback();
                } catch (Throwable e) {
                    if (logPrinter.shouldPrint()) {
                        LOG.error("FailoverChannelProcessor Transaction rollback exception", e);
                    }
                }
                break;
            } finally {
                tx.close();
            }
        }

        if (!success) {
            List<Channel> optionalChannels = selector.getOptionalChannels(event);
            for (Channel optChannel : optionalChannels) {
                Transaction tx = null;
                try {
                    tx = optChannel.getTransaction();
                    tx.begin();

                    optChannel.put(event);

                    tx.commit();
                } catch (Throwable t) {
                    if (logPrinter.shouldPrint()) {
                        LOG.error(
                                "FailoverChannelProcessor Unable to put event on optionalChannel:",
                                t);
                    }
                    if (tx != null) {
                        try {
                            tx.rollback();
                        } catch (Throwable e) {
                            if (logPrinter.shouldPrint()) {
                                LOG.error("FailoverChannelProcessor Transaction rollback exception",
                                        e);
                            }
                        }
                    }
                    if (t instanceof Error) {
                        if (logPrinter.shouldPrint()) {
                            LOG.error("FailoverChannelProcessor Error while writing event to "
                                    + "optionalChannels: " + optChannel, t);
                        }
                        throw (Error) t;
                    } else {
                        throw new ChannelException(
                                "FailoverChannelProcessor Unable to put event on "
                                        + "optionalChannels: " + optChannel, t);
                    }
                } finally {
                    if (tx != null) {
                        tx.close();
                    }
                }
            }
        }
    }
}