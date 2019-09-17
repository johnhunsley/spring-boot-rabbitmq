package com.hunsley.rabbitmq;

import static com.hunsley.rabbitmq.RabbitConfig.RETRIES_HEADER_KEY;
import static org.springframework.amqp.support.AmqpHeaders.CORRELATION_ID;

import com.hunsley.rabbitmq.handler.MessageHandler;
import com.hunsley.rabbitmq.props.Client;
import com.hunsley.rabbitmq.props.GDPQueue;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class ClientGDPMessageListenerImpl implements MessageListener {
  private Logger logger = LogManager.getLogger(ClientGDPMessageListenerImpl.class);
  private final int maxRetries;
  private final RabbitTemplate rabbitTemplate;
  private final Client client;
  private List<MessageHandler> messageHandlers;

  public ClientGDPMessageListenerImpl(final int maxRetries,
      RabbitTemplate rabbitTemplate,
      List<MessageHandler> messageHandlers,
      Client client) {
    this.maxRetries = maxRetries;
    this.rabbitTemplate = rabbitTemplate;
    this.messageHandlers = messageHandlers;
    this.client = client;
  }

  /**
   * Logs that an event was received along with the correlation ID if present
   *
   * @param message the message to log
   */
  private void logMessage(Message message) {
    Map<String, Object> headers = message.getMessageProperties().getHeaders();
    logger.info("Received event with correlation ID: {}",
        headers.getOrDefault(CORRELATION_ID, "N/A"));
  }

  @Override
  public void onMessage(Message message) {
    logMessage(message);

    try {
      handleMessage(message);

    } catch (InvalidMessageException e) {
      logger.error("Invalid message received", e);
      throw e;
    } catch (Exception e) {

      if (maxRetriesExceeded(message)) {
        logger.error("retries exceeded for message {}, reason {}", message, e);
        publish(client.getRoutingKey() + GDPQueue.DEAD_LETTER.value, message);

      } else {
        logger.info("retry message {}, reason {}", message, e);
        publish(client.getRoutingKey() + GDPQueue.RETRY.value, message);
      }
    }
  }

  private void handleMessage(Message message) throws Exception {

    if(messageHandlers != null && !messageHandlers.isEmpty()) {

      for(MessageHandler messageHandler : messageHandlers) {
        messageHandler.handleMessage(message);
      }

    } else {
      //todo - do we want to throw this here or handle a checked ex?
      throw new Exception("No message handler implementations");
    }
  }

  /**
   * Inspects message headers to see how many times they've already been retried. If this
   * exceeds the configured limit then true is returned. If retries is null then false is returned
   * and the x-retries header is added.
   *
   * @param message the message to inspect
   * @return true = retries exceeded, false = retries not exceeded or null x-retries
   */
  private boolean maxRetriesExceeded(Message message) {
    MessageProperties messageProperties = message.getMessageProperties();
    Map<String, Object> headers = messageProperties.getHeaders();

    long retries =
        (headers.get(RETRIES_HEADER_KEY) == null) ? 0L : (long) headers.get(RETRIES_HEADER_KEY) + 1;

    headers.put(RETRIES_HEADER_KEY, retries);

    return (retries >= maxRetries);
  }

  private void publish(final String routingKey, final Message message) {
    CorrelationData correlationData = new CorrelationData(message.toString());

    rabbitTemplate.convertAndSend(
        routingKey,
        message,
        correlationData
    );
  }


}
