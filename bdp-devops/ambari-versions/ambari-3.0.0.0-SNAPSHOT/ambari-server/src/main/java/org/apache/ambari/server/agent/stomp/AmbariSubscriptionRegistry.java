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

package org.apache.ambari.server.agent.stomp;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.broker.AbstractSubscriptionRegistry;
import org.springframework.messaging.simp.broker.SubscriptionRegistry;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.PathMatcher;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Implementation of {@link SubscriptionRegistry} that has configurable cache size, optimized working with cache and
 * destinations matching.
 */
public class AmbariSubscriptionRegistry extends AbstractSubscriptionRegistry {
  private static final Logger LOG = LoggerFactory.getLogger(AmbariSubscriptionRegistry.class);

  /** Static evaluation context to reuse. */
  private static final EvaluationContext messageEvalContext =
      SimpleEvaluationContext.forPropertyAccessors(new SimpMessageHeaderPropertyAccessor()).build();

  private PathMatcher pathMatcher = new AntPathMatcher();

  private volatile int cacheLimit;

  private String selectorHeaderName = "selector";

  private volatile boolean selectorHeaderInUse = false;

  private final ExpressionParser expressionParser = new SpelExpressionParser();

  private final DestinationCache destinationCache;

  private final SessionSubscriptionRegistry subscriptionRegistry = new SessionSubscriptionRegistry();

  public AmbariSubscriptionRegistry(int cacheLimit) {
    this.cacheLimit = cacheLimit;
    destinationCache = new DestinationCache();
  }

  /**
   * Specify the {@link PathMatcher} to use.
   */
  public void setPathMatcher(PathMatcher pathMatcher) {
    this.pathMatcher = pathMatcher;
  }

  /**
   * Return the configured {@link PathMatcher}.
   */
  public PathMatcher getPathMatcher() {
    return this.pathMatcher;
  }

  /**
   * Specify the maximum number of entries for the resolved destination cache.
   * Default is 1024.
   */
  public void setCacheLimit(int cacheLimit) {
    this.cacheLimit = cacheLimit;
  }

  /**
   * Return the maximum number of entries for the resolved destination cache.
   */
  public int getCacheLimit() {
    return this.cacheLimit;
  }

  /**
   * Configure the name of a selector header that a subscription message can
   * have in order to filter messages based on their headers. The value of the
   * header can use Spring EL expressions against message headers.
   * <p>For example the following expression expects a header called "foo" to
   * have the value "bar":
   * <pre>
   * headers.foo == 'bar'
   * </pre>
   * <p>By default this is set to "selector".
   * @since 4.2
   */
  public void setSelectorHeaderName(String selectorHeaderName) {
    Assert.notNull(selectorHeaderName, "'selectorHeaderName' must not be null");
    this.selectorHeaderName = selectorHeaderName;
  }

  /**
   * Return the name for the selector header.
   * @since 4.2
   */
  public String getSelectorHeaderName() {
    return this.selectorHeaderName;
  }


  @Override
  protected void addSubscriptionInternal(
      String sessionId, String subsId, String destination, Message<?> message) {

    Expression expression = getSelectorExpression(message.getHeaders());
    this.subscriptionRegistry.addSubscription(sessionId, subsId, destination, expression);
    this.destinationCache.updateAfterNewSubscription(destination, sessionId, subsId);
  }

  @Nullable
  private Expression getSelectorExpression(MessageHeaders headers) {
    Expression expression = null;
    if (getSelectorHeaderName() != null) {
      String selector = SimpMessageHeaderAccessor.getFirstNativeHeader(getSelectorHeaderName(), headers);
      if (selector != null) {
        try {
          expression = this.expressionParser.parseExpression(selector);
          this.selectorHeaderInUse = true;
          if (logger.isTraceEnabled()) {
            logger.trace("Subscription selector: [" + selector + "]");
          }
        }
        catch (Throwable ex) {
          if (logger.isDebugEnabled()) {
            logger.debug("Failed to parse selector: " + selector, ex);
          }
        }
      }
    }
    return expression;
  }

  @Override
  protected void removeSubscriptionInternal(String sessionId, String subsId, Message<?> message) {
    SessionSubscriptionInfo info = this.subscriptionRegistry.getSubscriptions(sessionId);
    if (info != null) {
      String destination = info.removeSubscription(subsId);
      if (destination != null) {
        this.destinationCache.updateAfterRemovedSubscription(sessionId, subsId);
      }
    }
  }

  @Override
  public void unregisterAllSubscriptions(String sessionId) {
    SessionSubscriptionInfo info = this.subscriptionRegistry.removeSubscriptions(sessionId);
    if (info != null) {
      this.destinationCache.updateAfterRemovedSession(info);
    }
  }

  @Override
  protected MultiValueMap<String, String> findSubscriptionsInternal(String destination, Message<?> message) {
    MultiValueMap<String, String> result = this.destinationCache.getSubscriptions(destination, message);
    return filterSubscriptions(result, message);
  }

  private MultiValueMap<String, String> filterSubscriptions(
      MultiValueMap<String, String> allMatches, Message<?> message) {

    if (!this.selectorHeaderInUse) {
      return allMatches;
    }
    MultiValueMap<String, String> result = new LinkedMultiValueMap<>(allMatches.size());
    allMatches.forEach((sessionId, subIds) -> {
      subIds.forEach(subId -> {
        SessionSubscriptionInfo info = this.subscriptionRegistry.getSubscriptions(sessionId);
        if (info == null) {
          return;
        }
        Subscription sub = info.getSubscription(subId);
        if (sub == null) {
          return;
        }
        Expression expression = sub.getSelectorExpression();
        if (expression == null) {
          result.add(sessionId, subId);
          return;
        }
        try {
          if (Boolean.TRUE.equals(expression.getValue(messageEvalContext, message, Boolean.class))) {
            result.add(sessionId, subId);
          }
        }
        catch (SpelEvaluationException ex) {
          if (logger.isDebugEnabled()) {
            logger.debug("Failed to evaluate selector: " + ex.getMessage());
          }
        }
        catch (Throwable ex) {
          logger.debug("Failed to evaluate selector", ex);
        }
      });
    });
    return result;
  }

  @Override
  public String toString() {
    return "DefaultSubscriptionRegistry[" + this.destinationCache + ", " + this.subscriptionRegistry + "]";
  }


  /**
   * A cache for destinations previously resolved via
   * {@link org.springframework.messaging.simp.broker.DefaultSubscriptionRegistry#findSubscriptionsInternal(String, Message)}
   */
  private class DestinationCache {

    /** Map from destination -> <sessionId, subscriptionId> for fast look-ups */
    private final Map<String, LinkedMultiValueMap<String, String>> accessCache =
        new ConcurrentHashMap<>(cacheLimit);

    //TODO optimize usage of this cache on perf cluster
    private final Cache<String, String> notSubscriptionCache =
        CacheBuilder.newBuilder().maximumSize(cacheLimit).build();

    public LinkedMultiValueMap<String, String> getSubscriptions(String destination, Message<?> message) {
      LinkedMultiValueMap<String, String> copiedSubscriptions = new LinkedMultiValueMap<>();
      if (notSubscriptionCache.asMap().keySet().contains(destination)) {
        return copiedSubscriptions;
      }
      this.accessCache.compute(destination, (key, value) -> {
        if (value == null) {
          LinkedMultiValueMap<String, String> result = new LinkedMultiValueMap<>();
          subscriptionRegistry.getAllSubscriptions().forEach((info) -> {
            info.getDestinations().forEach((destinationPattern) -> {
              //TODO temporary changed to more fast-acting check without regex, need move investigation
              if (destinationPattern.equals(destination)) {
                info.getSubscriptions(destinationPattern).forEach((subscription) -> {
                  result.add(info.sessionId, subscription.getId());
                });
              }
            });
          });
          if (!result.isEmpty()) {
            copiedSubscriptions.addAll(result.deepCopy());
            return result;
          } else {
            notSubscriptionCache.put(destination, "");
            return null;
          }
        } else {
          copiedSubscriptions.addAll(value.deepCopy());
          return value;
        }
      });
      return copiedSubscriptions;
    }

    public void updateAfterNewSubscription(String destination, String sessionId, String subsId) {
      LinkedMultiValueMap<String, String> updatedMap = this.accessCache.computeIfPresent(destination, (key, value) -> {
        if (getPathMatcher().match(destination, key)) {
          LinkedMultiValueMap<String, String> subs = value;
          subs.add(sessionId, subsId);
          return subs;
        }
        return value;
      });
      if (updatedMap == null) {
        this.notSubscriptionCache.invalidate(destination);
      }
    }

    public void updateAfterRemovedSubscription(String sessionId, String subsId) {
      for (Iterator<Map.Entry<String, LinkedMultiValueMap<String, String>>> iterator =
           this.accessCache.entrySet().iterator(); iterator.hasNext(); ) {
        Map.Entry<String, LinkedMultiValueMap<String, String>> entry = iterator.next();
        String destination = entry.getKey();
        this.accessCache.compute(destination, (key, value) -> {
          if (value != null) {
            List<String> subscriptions = value.get(sessionId);
            if (subscriptions != null) {
              subscriptions.remove(subsId);
              if (subscriptions.isEmpty()) {
                value.remove(sessionId);
              }
              if (value.isEmpty()) {
                return null;
              } else {
                this.notSubscriptionCache.invalidate(destination);
              }
            }
          }
          return value;
        });
      }
    }

    public void updateAfterRemovedSession(SessionSubscriptionInfo info) {
      for (Iterator<Map.Entry<String, LinkedMultiValueMap<String, String>>> iterator =
           this.accessCache.entrySet().iterator(); iterator.hasNext(); ) {
        Map.Entry<String, LinkedMultiValueMap<String, String>> entry = iterator.next();
        String destination = entry.getKey();
        this.accessCache.compute(destination, (key, value) -> {
          if (value != null) {
            if (value.remove(info.getSessionId()) != null) {
              if (value.isEmpty()) {
                return null;
              } else {
                this.notSubscriptionCache.invalidate(destination);
              }
            }
          }
          return value;
        });
      }
    }

    @Override
    public String toString() {
      return "cache[" + this.accessCache.size() + " destination(s)]";
    }
  }


  /**
   * Provide access to session subscriptions by sessionId.
   */
  private static class SessionSubscriptionRegistry {

    // sessionId -> SessionSubscriptionInfo
    private final ConcurrentMap<String, SessionSubscriptionInfo> sessions = new ConcurrentHashMap<>();

    @Nullable
    public SessionSubscriptionInfo getSubscriptions(String sessionId) {
      return this.sessions.get(sessionId);
    }

    public Collection<SessionSubscriptionInfo> getAllSubscriptions() {
      return this.sessions.values();
    }

    public SessionSubscriptionInfo addSubscription(String sessionId, String subscriptionId,
                                                   String destination, @Nullable Expression selectorExpression) {

      SessionSubscriptionInfo info = this.sessions.get(sessionId);
      if (info == null) {
        info = new SessionSubscriptionInfo(sessionId);
        SessionSubscriptionInfo value = this.sessions.putIfAbsent(sessionId, info);
        if (value != null) {
          info = value;
        }
      }
      info.addSubscription(destination, subscriptionId, selectorExpression);
      return info;
    }

    @Nullable
    public SessionSubscriptionInfo removeSubscriptions(String sessionId) {
      return this.sessions.remove(sessionId);
    }

    @Override
    public String toString() {
      return "registry[" + this.sessions.size() + " sessions]";
    }
  }


  /**
   * Hold subscriptions for a session.
   */
  private static class SessionSubscriptionInfo {

    private final String sessionId;

    // destination -> subscriptions
    private final Map<String, Set<Subscription>> destinationLookup = new ConcurrentHashMap<>(4);

    public SessionSubscriptionInfo(String sessionId) {
      Assert.notNull(sessionId, "'sessionId' must not be null");
      this.sessionId = sessionId;
    }

    public String getSessionId() {
      return this.sessionId;
    }

    public Set<String> getDestinations() {
      return this.destinationLookup.keySet();
    }

    public Set<Subscription> getSubscriptions(String destination) {
      return this.destinationLookup.get(destination);
    }

    @Nullable
    public Subscription getSubscription(String subscriptionId) {
      for (Map.Entry<String, Set<Subscription>> destinationEntry :
          this.destinationLookup.entrySet()) {
        for (Subscription sub : destinationEntry.getValue()) {
          if (sub.getId().equalsIgnoreCase(subscriptionId)) {
            return sub;
          }
        }
      }
      return null;
    }

    public void addSubscription(String destination, String subscriptionId, @Nullable Expression selectorExpression) {
      Set<Subscription> subs = this.destinationLookup.get(destination);
      if (subs == null) {
        synchronized (this.destinationLookup) {
          subs = this.destinationLookup.get(destination);
          if (subs == null) {
            subs = new CopyOnWriteArraySet<>();
            this.destinationLookup.put(destination, subs);
          }
        }
      }
      subs.add(new Subscription(subscriptionId, selectorExpression));
    }

    @Nullable
    public String removeSubscription(String subscriptionId) {
      for (Map.Entry<String, Set<Subscription>> destinationEntry :
          this.destinationLookup.entrySet()) {
        Set<Subscription> subs = destinationEntry.getValue();
        if (subs != null) {
          for (Subscription sub : subs) {
            if (sub.getId().equals(subscriptionId) && subs.remove(sub)) {
              synchronized (this.destinationLookup) {
                if (subs.isEmpty()) {
                  this.destinationLookup.remove(destinationEntry.getKey());
                }
              }
              return destinationEntry.getKey();
            }
          }
        }
      }
      return null;
    }

    @Override
    public String toString() {
      return "[sessionId=" + this.sessionId + ", subscriptions=" + this.destinationLookup + "]";
    }
  }


  private static final class Subscription {

    private final String id;

    @Nullable
    private final Expression selectorExpression;

    public Subscription(String id, @Nullable Expression selector) {
      Assert.notNull(id, "Subscription id must not be null");
      this.id = id;
      this.selectorExpression = selector;
    }

    public String getId() {
      return this.id;
    }

    @Nullable
    public Expression getSelectorExpression() {
      return this.selectorExpression;
    }

    @Override
    public boolean equals(Object other) {
      return (this == other || (other instanceof Subscription && this.id.equals(((Subscription) other).id)));
    }

    @Override
    public int hashCode() {
      return this.id.hashCode();
    }

    @Override
    public String toString() {
      return "subscription(id=" + this.id + ")";
    }
  }


  private static class SimpMessageHeaderPropertyAccessor implements PropertyAccessor {

    @Override
    public Class<?>[] getSpecificTargetClasses() {
      return new Class<?>[]{Message.class, MessageHeaders.class};
    }

    @Override
    public boolean canRead(EvaluationContext context, @Nullable Object target, String name) {
      return true;
    }

    @Override
    public TypedValue read(EvaluationContext context, @Nullable Object target, String name) {
      Object value;
      if (target instanceof Message) {
        value = name.equals("headers") ? ((Message) target).getHeaders() : null;
      } else if (target instanceof MessageHeaders) {
        MessageHeaders headers = (MessageHeaders) target;
        SimpMessageHeaderAccessor accessor =
            MessageHeaderAccessor.getAccessor(headers, SimpMessageHeaderAccessor.class);
        Assert.state(accessor != null, "No SimpMessageHeaderAccessor");
        if ("destination".equalsIgnoreCase(name)) {
          value = accessor.getDestination();
        } else {
          value = accessor.getFirstNativeHeader(name);
          if (value == null) {
            value = headers.get(name);
          }
        }
      } else {
        // Should never happen...
        throw new IllegalStateException("Expected Message or MessageHeaders.");
      }
      return new TypedValue(value);
    }

    @Override
    public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) {
      return false;
    }

    @Override
    public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object value) {
    }
  }

}
