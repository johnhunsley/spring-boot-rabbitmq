package com.hunsley.rabbitmq;

import static com.hunsley.rabbitmq.RabbitConfig.ORIGINAL_ROUTING_KEY;

import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class MessagePublisher {
  private Logger logger = LogManager.getLogger(MessagePublisher.class);

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
            logger.error("Failed sending message to queue {}",
                routingKey, ex);
          }
          return confirmed.get();
        },
        (tag, multiple) -> ack.set(true),
        (tag, multiple) -> ack.set(false)
    );

    if (!confirmed.get()) {
      logger.error("Failed to get confirmation that message was delivered to Rabbit.\n{}",
          message.toString());
    } else if (!ack.get()) {
      logger.error("NACK received in ConfirmCallback. Message not delivered to Rabbit.\n{}",
          message.toString());
    } else {
      logger.error("Confirm received from RabbitMQ.");
    }
  }

}
