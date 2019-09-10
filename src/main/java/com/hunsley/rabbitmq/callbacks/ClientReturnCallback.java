package com.hunsley.rabbitmq.callbacks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ReturnCallback;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class ClientReturnCallback implements ReturnCallback {
  private Logger logger = LogManager.getLogger(ClientReturnCallback.class);

  private final RabbitTemplate rabbitTemplate;
  private final String undeliverable;
  private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

  public ClientReturnCallback(ConnectionFactory connectionFactory, String undeliverable,
      ThreadPoolTaskExecutor threadPoolTaskExecutor) {
    this.rabbitTemplate = createTemplate(connectionFactory, undeliverable);
    this.undeliverable = undeliverable;
    this.threadPoolTaskExecutor = threadPoolTaskExecutor;
  }

  private RabbitTemplate createTemplate(ConnectionFactory connectionFactory, String undeliverableExchange) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setExchange(undeliverableExchange);
    rabbitTemplate.setMandatory(true);
    rabbitTemplate.setConfirmCallback(new ClientConfirmCallBack());
    rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> logger.error("Failed sending message to undeliverable queue."
            + " Message has been returned by the exchange. Details:"
            + "\n Reply Code: {} "
            + "\n Reply Text: {} ",
        replyCode,
        replyText));
    return rabbitTemplate;
  }

  @Override
  public void returnedMessage(Message message, int replyCode, String replyText, String exchange,
      String routingKey) {

    logger.info(
        "Message has been returned by the rabbitmq exchange, so send it to the undeliverable queue."
            + " Message Details:"
            + "\n Reply Code: {} "
            + "\n Reply Text: {} ",
        replyCode,
        replyText);

    threadPoolTaskExecutor.execute(() ->
        rabbitTemplate.convertAndSend(
            undeliverable,
            message)
    );
  }

}

