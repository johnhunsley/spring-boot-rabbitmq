package com.hunsley.rabbitmq;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessagePublisher {

  public static final int WAIT_FOR_CONFIRMS_TIMEOUT = 10000;

  public void send(RabbitTemplate rabbitTemplate, String exchangeName, String routingKey, Message message) {
    AtomicBoolean confirmed = new AtomicBoolean(false);
    AtomicBoolean ack = new AtomicBoolean();
    rabbitTemplate.invoke(template -> {
          try {
            message.getMessageProperties().setHeader(ORIGINAL_ROUTING_KEY, routingKey);
            template.send(exchangeName,
                routingKey,
                message);
            confirmed.set(template.waitForConfirms(WAIT_FOR_CONFIRMS_TIMEOUT));
          } catch (Exception ex) {
            log.error("Failed sending message to queue {}",
                routingKey, ex);
          }
          return confirmed.get();
        },
        (tag, multiple) -> ack.set(true),
        (tag, multiple) -> ack.set(false)
    );

    if (!confirmed.get()) {
      log.error("Failed to get confirmation that message was delivered to Rabbit.\n{}",
          MessageFormatter.forLogging(message));
    } else if (!ack.get()) {
      log.error("NACK received in ConfirmCallback. Message not delivered to Rabbit.\n{}",
          MessageFormatter.forLogging(message));
    } else {
      log.debug("Confirm received from RabbitMQ.");
    }
  }
}
