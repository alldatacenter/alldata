/*
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
package org.apache.ambari.server.state.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ambari.server.AmbariService;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.events.AlertEvent;
import org.apache.ambari.server.notifications.DispatchCallback;
import org.apache.ambari.server.notifications.DispatchCredentials;
import org.apache.ambari.server.notifications.DispatchFactory;
import org.apache.ambari.server.notifications.DispatchRunnable;
import org.apache.ambari.server.notifications.Notification;
import org.apache.ambari.server.notifications.NotificationDispatcher;
import org.apache.ambari.server.notifications.Recipient;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.NotificationState;
import org.apache.ambari.server.state.alert.AlertNotification;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * The {@link AlertNoticeDispatchService} is used to scan the database for
 * {@link AlertNoticeEntity} that are in the {@link NotificationState#PENDING}
 * state. It will then process them through the dispatch system.
 * <p/>
 * The dispatch system will then make a callback to
 * {@link AlertNoticeDispatchCallback} so that the {@link NotificationState} can
 * be updated to its final value.
 * <p/>
 * This class uses the templates that are defined via
 * {@link Configuration#getAlertTemplateFile()} or the fallback internal
 * template {@code alert-templates.xml}. These files are parsed during
 * {@link #startUp()}. If there is a problem parsing them, the service will
 * still startup normally, producing an error in logs. It will fall back to
 * simple string concatenation for {@link Notification} content in this case.
 */
@AmbariService
public class AlertNoticeDispatchService extends AbstractScheduledService {
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(AlertNoticeDispatchService.class);

  /**
   * The log tag to pass to Apache Velocity during rendering.
   */
  private static final String VELOCITY_LOG_TAG = "ambari-alerts";

  /**
   * The internal Ambari templates that ship.
   */
  private static final String AMBARI_ALERT_TEMPLATES = "alert-templates.xml";

  /**
   * The property containing the dispatch authentication username.
   */
  public static final String AMBARI_DISPATCH_CREDENTIAL_USERNAME = "ambari.dispatch.credential.username";

  /**
   * The property containing the dispatch authentication password.
   */
  public static final String AMBARI_DISPATCH_CREDENTIAL_PASSWORD = "ambari.dispatch.credential.password";

  /**
   * The property containing the dispatch recipients
   */
  public static final String AMBARI_DISPATCH_RECIPIENTS = "ambari.dispatch.recipients";

  /**
   * The context key for Ambari information to be passed to Velocity.
   */
  private static final String VELOCITY_AMBARI_KEY = "ambari";

  /**
   * The context key for alert summary information to be passed to Velocity.
   */
  private static final String VELOCITY_SUMMARY_KEY = "summary";

  /**
   * The context key for a single alert's information to be passed to Velocity.
   */
  private static final String VELOCITY_ALERT_KEY = "alert";

  /**
   * The context key for dispatch target information to be passed to Velocity.
   */
  private static final String VELOCITY_DISPATCH_KEY = "dispatch";

  /**
   * Gson used to convert JSON properties to a map.
   */
  private final Gson m_gson;

  /**
   * Dispatch DAO to query pending {@link AlertNoticeEntity} instances from.
   */
  @Inject
  private AlertDispatchDAO m_dao;

  /**
   * The factory used to get an {@link NotificationDispatcher} instance to
   * submit to the {@link #m_executor}.
   */
  @Inject
  private DispatchFactory m_dispatchFactory;

  /**
   * The alert templates to use when rendering content for a
   * {@link Notification}.
   */
  private AlertTemplates m_alertTemplates;

  /**
   * The configuration instance to get Ambari properties.
   */
  @Inject
  private Configuration m_configuration;

  /**
   * Ambari meta information used fro alert {@link Notification}s.
   */
  @Inject
  private Provider<AmbariMetaInfo> m_metaInfo;

  /**
   * The executor responsible for dispatching.
   */
  private Executor m_executor;

  /**
   * Constructor.
   */
  public AlertNoticeDispatchService() {
    m_executor = new ThreadPoolExecutor(0, 2, 5L, TimeUnit.MINUTES,
      new LinkedBlockingQueue<>(), new AlertDispatchThreadFactory(),
        new ThreadPoolExecutor.CallerRunsPolicy());

    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.registerTypeAdapter(AlertTargetProperties.class,
        new AlertTargetPropertyDeserializer());

    m_gson = gsonBuilder.create();
  }

  /**
   * {@inheritDoc}
   * <p/>
   * Parse the XML template for {@link Notification} content. If there is a
   * problem parsing the content, the service will still startup normally but
   * the {@link Notification} content will fallback to plaintext.
   */
  @Override
  protected void startUp() throws Exception {
    super.startUp();

    InputStream inputStream = null;
    String alertTemplatesFile = null;

    try {
      alertTemplatesFile = m_configuration.getAlertTemplateFile();
      if (null != alertTemplatesFile) {
        File file = new File(alertTemplatesFile);
        inputStream = new FileInputStream(file);
      }
    } catch (Exception exception) {
      LOG.warn("Unable to load alert template file {}", alertTemplatesFile,
          exception);
    }

    try {
      JAXBContext context = JAXBContext.newInstance(AlertTemplates.class);
      Unmarshaller unmarshaller = context.createUnmarshaller();

      // if the file provided via the configuration is not available, use
      // the internal one
      if (null == inputStream) {
        inputStream = ClassLoader.getSystemResourceAsStream(AMBARI_ALERT_TEMPLATES);
      }

      m_alertTemplates = (AlertTemplates) unmarshaller.unmarshal(inputStream);
    } catch (Exception exception) {
      LOG.error(
          "Unable to load alert template file {}, outbound notifications will not be formatted",
          AMBARI_ALERT_TEMPLATES, exception);
    } finally {
      if (null != inputStream) {
        IOUtils.closeQuietly(inputStream);
      }
    }
  }

  /**
   * Sets the {@link Executor} to use when dispatching {@link Notification}s.
   * This should only be used by unit tests to provide a mock executor.
   *
   * @param executor
   *          the executor to use (not {@code null).

   */
  protected void setExecutor(Executor executor) {
    m_executor = executor;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void runOneIteration() throws Exception {
    List<AlertNoticeEntity> pending = m_dao.findPendingNotices();
    if (pending.size() == 0) {
      return;
    }

    LOG.info("There are {} pending alert notices about to be dispatched...",
        pending.size());

    Map<AlertTargetEntity, List<AlertNoticeEntity>> aggregateMap =
      new HashMap<>(pending.size());

    // combine all histories by target
    for (AlertNoticeEntity notice : pending) {
      AlertTargetEntity target = notice.getAlertTarget();

      List<AlertNoticeEntity> notices = aggregateMap.get(target);
      if (null == notices) {
        notices = new ArrayList<>();
        aggregateMap.put(target, notices);
      }

      // at this point, notices have been processed but not yet delivered
      notice.setNotifyState(NotificationState.DISPATCHED);
      notice = m_dao.merge(notice);

      notices.add(notice);
    }

    // now that all of the notices are grouped by target, dispatch them
    Set<AlertTargetEntity> targets = aggregateMap.keySet();
    for (AlertTargetEntity target : targets) {
      List<AlertNoticeEntity> notices = aggregateMap.get(target);
      if (null == notices || notices.size() == 0) {
        continue;
      }
      try {
        String targetType = target.getNotificationType();
        NotificationDispatcher dispatcher = m_dispatchFactory.getDispatcher(targetType);

        // create a single digest notification if supported
        if (dispatcher.isDigestSupported()) {
          AlertNotification notification = buildNotificationFromTarget(target);
          notification.CallbackIds = new ArrayList<>(notices.size());
          List<AlertHistoryEntity> histories = new ArrayList<>(
                  notices.size());

          // add callback IDs so that the notices can be marked as DELIVERED or
          // FAILED, and create a list of just the alert histories
          for (AlertNoticeEntity notice : notices) {
            AlertHistoryEntity history = notice.getAlertHistory();
            histories.add(history);

            notification.CallbackIds.add(notice.getUuid());
          }


          // populate the subject and body fields; if there is a problem
          // generating the content, then mark the notices as FAILED
          try {
            renderDigestNotificationContent(dispatcher, notification, histories, target);

            // dispatch
            DispatchRunnable runnable = new DispatchRunnable(dispatcher, notification);
            m_executor.execute(runnable);
          } catch (Exception exception) {
            LOG.error("Unable to create notification for alerts", exception);

            // there was a problem generating content for the target; mark all
            // notices as FAILED and skip this target
            // mark these as failed
            notification.Callback.onFailure(notification.CallbackIds);
          }
        } else {
          // the dispatcher does not support digest, each notice must have a 1:1
          // notification created for it
          for (AlertNoticeEntity notice : notices) {
            AlertNotification notification = buildNotificationFromTarget(target);
            AlertHistoryEntity history = notice.getAlertHistory();
            notification.CallbackIds = Collections.singletonList(notice.getUuid());

            // populate the subject and body fields; if there is a problem
            // generating the content, then mark the notices as FAILED
            try {
              renderNotificationContent(dispatcher, notification, history, target);

              // dispatch
              DispatchRunnable runnable = new DispatchRunnable(dispatcher, notification);
              m_executor.execute(runnable);
            } catch (Exception exception) {
              LOG.error("Unable to create notification for alert", exception);

              // mark these as failed
              notification.Callback.onFailure(notification.CallbackIds);
            }
          }
        }
      } catch (Exception e) {
        LOG.error("Caught exception during Alert Notice dispatching.", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   * <p/>
   * Returns a schedule that starts after 2 minute and runs every 2 minutes
   * after {@link #runOneIteration()} completes.
   */
  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedDelaySchedule(2, 2, TimeUnit.MINUTES);
  }

  /**
   * Initializes a {@link Notification} instance from an
   * {@link AlertTargetEntity}. This method does most of the boilerplate work to
   * get a {@link Notification} that is almost ready to send.
   * <p/>
   * The {@link Notification} will not have any of the callback IDs or content
   * set.
   *
   * @param target
   *          the alert target
   * @return the initialized notification
   */
  private AlertNotification buildNotificationFromTarget(AlertTargetEntity target) {
    String propertiesJson = target.getProperties();

    AlertTargetProperties targetProperties = m_gson.fromJson(propertiesJson,
        AlertTargetProperties.class);

    Map<String, String> properties = targetProperties.Properties;

    // create an initialize the notification
    AlertNotification notification = new AlertNotification();
    notification.Callback = new AlertNoticeDispatchCallback();
    notification.DispatchProperties = properties;

    // set dispatch credentials
    if (properties.containsKey(AMBARI_DISPATCH_CREDENTIAL_USERNAME)
        && properties.containsKey(AMBARI_DISPATCH_CREDENTIAL_PASSWORD)) {
      DispatchCredentials credentials = new DispatchCredentials();
      credentials.UserName = properties.get(AMBARI_DISPATCH_CREDENTIAL_USERNAME);
      credentials.Password = properties.get(AMBARI_DISPATCH_CREDENTIAL_PASSWORD);
      notification.Credentials = credentials;
    }

    // create recipients
    if (null != targetProperties.Recipients) {
      List<Recipient> recipients = new ArrayList<>(
        targetProperties.Recipients.size());

      for (String stringRecipient : targetProperties.Recipients) {
        Recipient recipient = new Recipient();
        recipient.Identifier = stringRecipient;
        recipients.add(recipient);
      }

      notification.Recipients = recipients;
    }

    return notification;
  }

  /**
   * Generates digest content for the {@link Notification} using the
   * {@link #m_alertTemplates} and the list of alerts passed in. If there is a
   * problem with the templates, this will fallback to non-formatted content.
   *
   * @param dispatcher
   *          the dispatcher for this notification type (not {@code null}).
   * @param notification
   *          the notification (not {@code null}).
   * @param histories
   *          the alerts to generate the content from (not {@code null}.
   * @param target
   *          the target of the {@link Notification}.
   */
  private void renderDigestNotificationContent(NotificationDispatcher dispatcher,
      AlertNotification notification, List<AlertHistoryEntity> histories, AlertTargetEntity target)
      throws IOException {
    String targetType = target.getNotificationType();

    // build the velocity objects for template rendering
    AmbariInfo ambari = new AmbariInfo(m_metaInfo.get(), m_configuration);
    AlertSummaryInfo summary = new AlertSummaryInfo(histories);
    DispatchInfo dispatch = new DispatchInfo(target);

    // get the template for this target type
    final Writer subjectWriter = new StringWriter();
    final Writer bodyWriter = new StringWriter();
    final AlertTemplate template = m_alertTemplates.getTemplate(targetType);

    if (dispatcher.isNotificationContentGenerationRequired()) {
      if (null != template) {
        // create the velocity context for template rendering
        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put(VELOCITY_AMBARI_KEY, ambari);
        velocityContext.put(VELOCITY_SUMMARY_KEY, summary);
        velocityContext.put(VELOCITY_DISPATCH_KEY, dispatch);

        // render the template and assign the content to the notification
        String subjectTemplate = template.getSubject();
        String bodyTemplate = template.getBody();

        // render the subject
        Velocity.evaluate(velocityContext, subjectWriter, VELOCITY_LOG_TAG, subjectTemplate);

        // render the body
        Velocity.evaluate(velocityContext, bodyWriter, VELOCITY_LOG_TAG, bodyTemplate);
      } else {
        // a null template is possible from parsing incorrectly or not
        // having the correct type defined for the target
        for (AlertHistoryEntity alert : histories) {
          subjectWriter.write("Apache Ambari Alert Summary");
          bodyWriter.write(alert.getAlertState().name());
          bodyWriter.write(" ");
          bodyWriter.write(alert.getAlertDefinition().getLabel());
          bodyWriter.write(" ");
          bodyWriter.write(alert.getAlertText());
          bodyWriter.write("\n");
        }
      }
    }

    notification.Subject = subjectWriter.toString();
    notification.Body = bodyWriter.toString();
  }

  /**
   * Generates the content for the {@link Notification} using the
   * {@link #m_alertTemplates} and the single alert passed in. If there is a
   * problem with the templates, this will fallback to non-formatted content.
   *
   * @param dispatcher
   *          the dispatcher for this notification type (not {@code null}).
   * @param notification
   *          the notification (not {@code null}).
   * @param history
   *          the alert to generate the content from (not {@code null}.
   * @param target
   *          the target of the {@link Notification}.
   */
  private void renderNotificationContent(NotificationDispatcher dispatcher,
      AlertNotification notification, AlertHistoryEntity history, AlertTargetEntity target)
      throws IOException {
    String targetType = target.getNotificationType();

    // build the velocity objects for template rendering
    AmbariInfo ambari = new AmbariInfo(m_metaInfo.get(), m_configuration);
    AlertInfo alert = new AlertInfo(history);
    DispatchInfo dispatch = new DispatchInfo(target);

    // set the alert info on the notification subclass so that dispatchers
    // can use it directly
    notification.setAlertInfo(alert);

    // get the template for this target type
    final Writer subjectWriter = new StringWriter();
    final Writer bodyWriter = new StringWriter();
    final AlertTemplate template = m_alertTemplates.getTemplate(targetType);

    if (dispatcher.isNotificationContentGenerationRequired()) {
      if (null != template) {
        // create the velocity context for template rendering
        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put(VELOCITY_AMBARI_KEY, ambari);
        velocityContext.put(VELOCITY_ALERT_KEY, alert);
        velocityContext.put(VELOCITY_DISPATCH_KEY, dispatch);

        // render the template and assign the content to the notification
        String subjectTemplate = template.getSubject();
        String bodyTemplate = template.getBody();

        // render the subject
        Velocity.evaluate(velocityContext, subjectWriter, VELOCITY_LOG_TAG, subjectTemplate);

        // render the body
        Velocity.evaluate(velocityContext, bodyWriter, VELOCITY_LOG_TAG, bodyTemplate);
      } else {
        // a null template is possible from parsing incorrectly or not
        // having the correct type defined for the target
        subjectWriter.write(alert.getAlertState().name());
        subjectWriter.write(" ");
        subjectWriter.write(alert.getAlertName());

        bodyWriter.write(alert.getAlertState().name());
        bodyWriter.write(" ");
        bodyWriter.write(alert.getAlertName());
        bodyWriter.write(" ");
        bodyWriter.write(alert.getAlertText());
        if (alert.hasHostName()) {
          bodyWriter.write(" ");
          bodyWriter.append(alert.getHostName());
        }
        bodyWriter.write("\n");
      }
    }

    notification.Subject = subjectWriter.toString();
    notification.Body = bodyWriter.toString();
  }

  /**
   * The {@link AlertTargetProperties} separates out the dispatcher properties
   * from the list of recipients which is a JSON array and not a String.
   */
  private static final class AlertTargetProperties {
    /**
     * The properties to pass to the concrete dispatcher.
     */
    public Map<String, String> Properties;

    /**
     * The recipients of the notice.
     */
    public List<String> Recipients;
  }

  /**
   * The {@link AlertTargetPropertyDeserializer} is used to dump the majority of
   * JSON serialized properties into a {@link Map} of {@link String} while at
   * the same time, converting
   * {@link AlertNoticeDispatchService#AMBARI_DISPATCH_RECIPIENTS} into a list.
   */
  private static final class AlertTargetPropertyDeserializer implements
      JsonDeserializer<AlertTargetProperties> {

    /**
     * {@inheritDoc}
     */
    @Override
    public AlertTargetProperties deserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) throws JsonParseException {

      AlertTargetProperties properties = new AlertTargetProperties();
      properties.Properties = new HashMap<>();

      final JsonObject jsonObject = json.getAsJsonObject();
      Set<Entry<String, JsonElement>> entrySet = jsonObject.entrySet();

      for (Entry<String, JsonElement> entry : entrySet) {
        String entryKey = entry.getKey();
        JsonElement entryValue = entry.getValue();

        if (entryKey.equals(AMBARI_DISPATCH_RECIPIENTS)) {
          Type listType = new TypeToken<List<String>>() {
          }.getType();
          JsonArray jsonArray = entryValue.getAsJsonArray();
          properties.Recipients = context.deserialize(jsonArray, listType);
        } else {
          properties.Properties.put(entryKey, entryValue.getAsString());
        }
      }

      return properties;
    }
  }

  /**
   * A custom {@link ThreadFactory} for the threads that will handle dispatching
   * {@link AlertNoticeEntity} instances. Threads created will have slightly
   * reduced priority since {@link AlertEvent} instances are not critical to the
   * system.
   */
  private static final class AlertDispatchThreadFactory implements
      ThreadFactory {

    private static final AtomicInteger s_threadIdPool = new AtomicInteger(1);

    /**
     * {@inheritDoc}
     */
    @Override
    public Thread newThread(Runnable r) {
      Thread thread = new Thread(r, "alert-dispatch-"
          + s_threadIdPool.getAndIncrement());

      thread.setDaemon(false);
      thread.setPriority(Thread.NORM_PRIORITY - 1);

      return thread;
    }
  }

  /**
   * The {@link AlertNoticeDispatchCallback} is used to receive a callback from
   * the dispatch framework and then update the {@link AlertNoticeEntity}
   * {@link NotificationState}.
   */
  private final class AlertNoticeDispatchCallback implements DispatchCallback {

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSuccess(List<String> callbackIds) {
      for (String callbackId : callbackIds) {
        updateAlertNotice(callbackId, NotificationState.DELIVERED);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFailure(List<String> callbackIds) {
      for (String callbackId : callbackIds) {
        updateAlertNotice(callbackId, NotificationState.FAILED);
      }
    }

    /**
     * Updates the {@link AlertNoticeEntity} matching the given UUID with the
     * specified state.
     *
     * @param uuid
     * @param state
     */
    private void updateAlertNotice(String uuid, NotificationState state) {
      try {
        AlertNoticeEntity entity = m_dao.findNoticeByUuid(uuid);
        if (null == entity) {
          LOG.warn("Unable to find an alert notice with UUID {}", uuid);
          return;
        }

        entity.setNotifyState(state);
        m_dao.merge(entity);
      } catch (Exception exception) {
        LOG.error(
            "Unable to update the alert notice with UUID {} to {}, notifications will continue to be sent",
            uuid, state, exception);
      }
    }
  }

  /**
   * The {@link AlertInfo} class encapsulates all information about a single
   * alert for a single outbound {@link Notification}.
   */
  public final static class AlertInfo {
    private final AlertHistoryEntity m_history;

    /**
     * Constructor.
     *
     * @param history
     */
    public AlertInfo(AlertHistoryEntity history) {
      m_history = history;
    }

    /**
     * Gets the host name or {@code null} if none.
     *
     * @return
     */
    public String getHostName() {
      return m_history.getHostName();
    }

    /**
     * Gets whether there is a host associated with the alert. Some alerts like
     * aggregate alerts don't have hosts.
     *
     * @return
     */
    public boolean hasHostName() {
      return m_history.getHostName() != null;
    }

    /**
     * Gets the service name or {@code null} if none.
     *
     * @return
     */
    public String getServiceName() {
      return m_history.getServiceName();
    }

    /**
     * Gets the service component name, or {@code null} if none.
     *
     * @return
     */
    public String getComponentName() {
      return m_history.getComponentName();
    }

    /**
     * Gets whether there is a component associated with an alert. Some alerts
     * don't have an associated component.
     *
     * @return
     */
    public boolean hasComponentName() {
      return m_history.getComponentName() != null;
    }

    /**
     *  Gets the time that the alert was received
     * @return
       */
    public long getAlertTimestamp() {
      return m_history.getAlertTimestamp();
    }

    /**
     * Gets the state of the alert.
     *
     * @return
     */
    public AlertState getAlertState() {
      return m_history.getAlertState();
    }

    /**
     * Gets the definition id of the alert.
     *
     * @return
     */
    public Long getAlertDefinitionId() {
      return m_history.getAlertDefinitionId();
    }

    /**
     * Gets the hash of alert definition entity.
     *
     * @return
     */
    public int getAlertDefinitionHash() {
      return m_history.getAlertDefinitionHash();
    }


    /**
     * Gets the descriptive name of the alert.
     *
     * @return
     */
    public String getAlertName() {
      return m_history.getAlertDefinition().getLabel();
    }

    /**
     * Gets the alert definition for this alert.
     *
     * @return the alert definition.
     */
    public AlertDefinitionEntity getAlertDefinition() {
      return m_history.getAlertDefinition();
    }

    /**
     * Gets the text of the alert.
     *
     * @return
     */
    public String getAlertText() {
      return m_history.getAlertText();
    }
  }

  /**
   * The {@link AlertSummaryInfo} class encapsulates all of the alert
   * information for the {@link Notification}. This includes customized
   * structures to better organize information about each of the services,
   * hosts, and alert states.
   */
  public final static class AlertSummaryInfo {
    private int m_okCount = 0;
    private int m_warningCount = 0;
    private int m_criticalCount = 0;
    private int m_unknownCount = 0;

    /**
     * The hosts that have at least 1 alert reported.
     */
    private final Set<String> m_hosts = new HashSet<>();

    /**
     * The services that have at least 1 alert reported.
     */
    private final Set<String> m_services = new HashSet<>();

    /**
     * All of the alerts for the {@link Notification}.
     */
    private final List<AlertHistoryEntity> m_alerts;

    /**
     * A mapping of service to alerts where the alerts are also grouped by state
     * for that service.
     */
    private final Map<String, Map<AlertState, List<AlertHistoryEntity>>> m_alertsByServiceAndState = new HashMap<>();

    /**
     * A mapping of all services by state.
     */
    private final Map<String, Set<String>> m_servicesByState = new HashMap<>();

    /**
     * A mapping of all alerts by the service that owns them.
     */
    private final Map<String, List<AlertHistoryEntity>> m_alertsByService = new HashMap<>();

    /**
     * Constructor.
     *
     * @param histories
     */
    protected AlertSummaryInfo(List<AlertHistoryEntity> histories) {
      m_alerts = histories;

      // group all alerts by their service and severity
      for (AlertHistoryEntity history : m_alerts) {
        AlertState alertState = history.getAlertState();
        String serviceName = history.getServiceName();
        String hostName = history.getHostName();

        if (null != hostName) {
          m_hosts.add(hostName);
        }

        if (null != serviceName) {
          m_services.add(serviceName);
        }

        // group alerts by service name & state
        Map<AlertState, List<AlertHistoryEntity>> service = m_alertsByServiceAndState.get(serviceName);
        if (null == service) {
          service = new HashMap<>();
          m_alertsByServiceAndState.put(serviceName, service);
        }

        List<AlertHistoryEntity> alertList = service.get(alertState);
        if (null == alertList) {
          alertList = new ArrayList<>();
          service.put(alertState, alertList);
        }

        alertList.add(history);

        // group services by alert states
        Set<String> services = m_servicesByState.get(alertState.name());
        if (null == services) {
          services = new HashSet<>();
          m_servicesByState.put(alertState.name(), services);
        }

        services.add(serviceName);

        // group alerts by service
        List<AlertHistoryEntity> alertsByService = m_alertsByService.get(serviceName);
        if (null == alertsByService) {
          alertsByService = new ArrayList<>();
          m_alertsByService.put(serviceName, alertsByService);
        }

        alertsByService.add(history);

        // keep track of totals
        switch (alertState) {
          case CRITICAL:
            m_criticalCount++;
            break;
          case OK:
            m_okCount++;
            break;
          case UNKNOWN:
            m_unknownCount++;
            break;
          case WARNING:
            m_warningCount++;
            break;
          default:
            m_unknownCount++;
            break;
        }
      }
    }

    /**
     * Gets the total number of OK alerts in the {@link Notification}.
     *
     * @return the OK count.
     */
    public int getOkCount() {
      return m_okCount;
    }

    /**
     * Gets the total number of WARNING alerts in the {@link Notification}.
     *
     * @return the WARNING count.
     */
    public int getWarningCount() {
      return m_warningCount;
    }

    /**
     * Gets the total number of CRITICAL alerts in the {@link Notification}.
     *
     * @return the CRITICAL count.
     */
    public int getCriticalCount() {
      return m_criticalCount;
    }

    /**
     * Gets the total number of UNKNOWN alerts in the {@link Notification}.
     *
     * @return the UNKNOWN count.
     */
    public int getUnknownCount() {
      return m_unknownCount;
    }

    /**
     * Gets the total count of all alerts in the {@link Notification}
     *
     * @return the total count of all alerts.
     */
    public int getTotalCount() {
      return m_okCount + m_warningCount + m_criticalCount + m_unknownCount;
    }

    /**
     * Gets all of the services that have alerts being reporting in this
     * notification dispatch.
     *
     * @return the list of services.
     */
    public Set<String> getServices() {
      return m_services;
    }

    /**
     * Gets all of the alerts in the {@link Notification}.
     *
     * @return all of the alerts.
     */
    public List<AlertHistoryEntity> getAlerts() {
      return m_alerts;
    }

    /**
     * Gets all of the alerts in the {@link Notification} by service name.
     *
     * @param serviceName
     *          the service name.
     * @return the alerts for that service, or {@code null} none.
     */
    public List<AlertHistoryEntity> getAlerts(String serviceName) {
      return m_alertsByService.get(serviceName);
    }

    /**
     * Gets all of the alerts for a given service and alert state level.
     *
     * @param serviceName
     *          the name of the service.
     * @param alertState
     *          the alert state level.
     * @return the list of alerts or {@code null} for none.
     */
    public List<AlertHistoryEntity> getAlerts(String serviceName,
        String alertState) {

      Map<AlertState, List<AlertHistoryEntity>> serviceAlerts = m_alertsByServiceAndState.get(serviceName);
      if (null == serviceAlerts) {
        return null;
      }

      AlertState state = AlertState.valueOf(alertState);
      return serviceAlerts.get(state);
    }

    /**
     * Gets a list of services that have an alert being reporting for the given
     * state.
     *
     * @param alertState
     *          the state to get the services for.
     * @return the services or {@code null} if none.
     */
    public Set<String> getServicesByAlertState(String alertState) {
      return m_servicesByState.get(alertState);
    }
  }

  /**
   * The {@link AmbariInfo} class is used to provide the template engine with
   * information about the Ambari installation.
   */
  public final static class AmbariInfo {
    private String m_hostName = null;
    private String m_url = null;
    private String m_version = null;

    /**
     * Constructor.
     *
     * @param metaInfo
     */
    protected AmbariInfo(AmbariMetaInfo metaInfo, Configuration m_configuration) {
      m_url = m_configuration.getAmbariDisplayUrl();
      m_version = metaInfo.getServerVersion();
    }

    /**
     * @return the hostName
     */
    public String getHostName() {
      return m_hostName;
    }

    public boolean hasUrl() {
      return m_url != null;
    }

    /**
     * @return the url
     */
    public String getUrl() {
      return m_url;
    }

    /**
     * Gets the Ambari server version.
     *
     * @return the version
     */
    public String getServerVersion() {
      return m_version;
    }
  }

  /**
   * The {@link DispatchInfo} class is used to provide the template engine with
   * information about the intended target of the notification.
   */
  public static final class DispatchInfo {
    private String m_targetName;
    private String m_targetDescription;

    /**
     * Constructor.
     *
     * @param target
     *          the {@link AlertTargetEntity} receiving the notification.
     */
    protected DispatchInfo(AlertTargetEntity target) {
      m_targetName = target.getTargetName();
      m_targetDescription = target.getDescription();
    }

    /**
     * Gets the name of the notification target.
     *
     * @return the name of the target.
     */
    public String getTargetName() {
      return m_targetName;
    }

    /**
     * Gets the description of the notification target.
     *
     * @return the target description.
     */
    public String getTargetDescription() {
      return m_targetDescription;
    }
  }

  /**
   * The {@link AlertTemplates} class represnts the {@link AlertTemplates} that
   * have been loaded, either by the {@link Configuration} or by the backup
   * {@code alert-templates.xml} file.
   */
  @XmlRootElement(name = "alert-templates")
  private final static class AlertTemplates {
    /**
     * The alert templates defined.
     */
    @XmlElement(name = "alert-template", required = true)
    private List<AlertTemplate> m_templates;

    /**
     * Gets the alert template given the specified template type.
     *
     * @param type
     *          the template type.
     * @return the template, or {@code null} if none.
     * @see AlertTargetEntity#getNotificationType()
     */
    public AlertTemplate getTemplate(String type) {
      for (AlertTemplate template : m_templates) {
        if (type.equals(template.getType())) {
          return template;
        }
      }

      return null;
    }
  }

  /**
   * The {@link AlertTemplate} class represents a template for a specified alert
   * target type that can be used when creating the content for dispatching
   * {@link Notification}s.
   */
  private final static class AlertTemplate {
    /**
     * The type that this template is for.
     *
     * @see AlertTargetEntity#getNotificationType()
     */
    @XmlAttribute(name = "type", required = true)
    private String m_type;

    /**
     * The subject template for the {@link Notification}.
     */
    @XmlElement(name = "subject", required = true)
    private String m_subject;

    /**
     * The body template for the {@link Notification}.
     */
    @XmlElement(name = "body", required = true)
    private String m_body;

    /**
     * Gets the template type.
     *
     * @return the template type.
     */
    public String getType() {
      return m_type;
    }

    /**
     * Gets the subject template.
     *
     * @return the subject template.
     */
    public String getSubject() {
      return m_subject;
    }

    /**
     * Gets the body template.
     *
     * @return the body template.
     */
    public String getBody() {
      return m_body;
    }
  }
}
