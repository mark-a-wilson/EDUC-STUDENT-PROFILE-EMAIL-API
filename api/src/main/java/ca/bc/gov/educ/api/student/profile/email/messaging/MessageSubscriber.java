package ca.bc.gov.educ.api.student.profile.email.messaging;

import ca.bc.gov.educ.api.student.profile.email.model.Event;
import ca.bc.gov.educ.api.student.profile.email.props.ApplicationProperties;
import ca.bc.gov.educ.api.student.profile.email.service.EventHandlerService;
import ca.bc.gov.educ.api.student.profile.email.utils.JsonUtil;
import io.nats.streaming.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.api.student.profile.email.constants.Topics.PROFILE_REQUEST_EMAIL_API_TOPIC;
import static lombok.AccessLevel.PRIVATE;

/**
 * This listener uses durable queue groups of nats streaming client.
 * A durable queue group allows you to have all members leave but still maintain state. When a member re-joins, it starts at the last position in that group.
 * <b>DO NOT call unsubscribe on the subscription.</b> please see the below for details.
 * Closing the Group
 * The last member calling Unsubscribe will close (that is destroy) the group. So if you want to maintain durability of the group,
 * <b>you should not be calling Unsubscribe.</b>
 * <p>
 * So unlike for non-durable queue subscribers, it is possible to maintain a queue group with no member in the server.
 * When a new member re-joins the durable queue group, it will resume from where the group left of, actually first receiving
 * all unacknowledged messages that may have been left when the last member previously left.
 */
@Component
@Slf4j
@SuppressWarnings("squid:S2142")
public class MessageSubscriber implements Closeable {

  @Getter(PRIVATE)
  private final EventHandlerService eventHandlerService;
  private StreamingConnection connection;
  private final StreamingConnectionFactory connectionFactory;

  @Autowired
  public MessageSubscriber(final ApplicationProperties applicationProperties, final EventHandlerService eventHandlerService) throws IOException, InterruptedException {
    this.eventHandlerService = eventHandlerService;
    Options options = new Options.Builder()
        .natsUrl(applicationProperties.getNatsUrl())
        .clusterId(applicationProperties.getNatsClusterId())
        .clientId("student-profile-email-api-subscriber" + UUID.randomUUID().toString())
        .connectionLostHandler(this::connectionLostHandler).build();
    connectionFactory = new StreamingConnectionFactory(options);
    connection = connectionFactory.createConnection();
  }

  @PostConstruct
  public void subscribe() throws InterruptedException, TimeoutException, IOException {
    SubscriptionOptions options = new SubscriptionOptions.Builder().durableName("student-profile-email-api-consumer").build();
    connection.subscribe(PROFILE_REQUEST_EMAIL_API_TOPIC.toString(), "student-profile-email", this::onPenReqEmailTopicMessage, options);
  }

  /**
   * This method will process the event message pushed into the queue.
   *
   * @param message the string representation of {@link Event} if it not type of event then it will throw exception and will be ignored.
   */
  private void onPenReqEmailTopicMessage(Message message) {
    if (message != null) {
      try {
        String eventString = new String(message.getData());
        Event event = JsonUtil.getJsonObjectFromString(Event.class, eventString);
        log.info("received event for event type :: {} and saga ID :: {}", event.getEventType(), event.getSagaId());
        getEventHandlerService().handleEvent(event);
      } catch (final Exception ex) {
        log.error("Exception ", ex);
      }
    }
  }


  /**
   * This method will keep retrying for a connection.
   */

  private void connectionLostHandler(StreamingConnection streamingConnection, Exception e) {
    if (e != null) {
      int numOfRetries = 1;
      numOfRetries = retryConnection(numOfRetries);
      retrySubscription(numOfRetries);
    }
  }

  private void retrySubscription(int numOfRetries) {
    while (true) {
      try {
        log.trace("retrying subscription as connection was lost :: retrying ::" + numOfRetries++);
        this.subscribe();
        log.info("successfully resubscribed after {} attempts", numOfRetries);
        break;
      } catch (InterruptedException | TimeoutException | IOException exception) {
        log.error("exception occurred while retrying subscription", exception);
        try {
          double sleepTime = (2 * numOfRetries);
          TimeUnit.SECONDS.sleep((long) sleepTime);
        } catch (InterruptedException exc) {
          log.error("InterruptedException occurred while retrying subscription", exc);
        }
      }
    }
  }

  private int retryConnection(int numOfRetries) {
    while (true) {
      try {
        log.trace("retrying connection as connection was lost :: retrying ::" + numOfRetries++);
        connection = connectionFactory.createConnection();
        log.info("successfully reconnected after {} attempts", numOfRetries);
        break;
      } catch (IOException | InterruptedException ex) {
        log.error("exception occurred", ex);
        try {
          double sleepTime = (2 * numOfRetries);
          TimeUnit.SECONDS.sleep((long) sleepTime);
        } catch (InterruptedException exc) {
          log.error("exception occurred", exc);
        }
      }
    }
    return numOfRetries;
  }

  @Override
  public void close() {
    if(connection != null){
      log.info("closing nats connection in the subscriber...");
      try {
        connection.close();
      } catch (IOException | TimeoutException | InterruptedException e) {
        log.error("error while closing nats connection in the subscriber...", e);
      }
      log.info("nats connection closed in the subscriber...");
    }
  }
}
