package com.hunsley.rabbitmq.callbacks;

import com.hunsley.rabbitmq.RabbitClientConfigurationProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ConfirmCallback;

public class ClientConfirmCallBack implements ConfirmCallback {
  private Logger logger = LogManager.getLogger(RabbitClientConfigurationProcessor.class);

  @Override
  public void confirm(CorrelationData correlationData, boolean ack, String cause) {
    if (!ack) {
      logger.error("NACK received in ConfirmCallback. Message not delivered to Rabbit. Cause: {}\n{}",
          cause, correlationData);
    }
  }
}
