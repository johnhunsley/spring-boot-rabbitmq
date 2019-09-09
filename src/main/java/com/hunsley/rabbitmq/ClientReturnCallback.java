package com.hunsley.rabbitmq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ReturnCallback;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class ClientReturnCallback implements ReturnCallback {
  private Logger logger = LogManager.getLogger(ClientReturnCallback.class);

  private final RabbitTemplate rabbitTemplate;
  private final String undeliverableQueue;
  private final String undeliverableExchange;
  private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

  public ClientReturnCallback(RabbitTemplate rabbitTemplate, String undeliverableQueue,
      String undeliverableExchange,
      ThreadPoolTaskExecutor threadPoolTaskExecutor) {
    this.rabbitTemplate = rabbitTemplate;
    this.undeliverableQueue = undeliverableQueue;
    this.undeliverableExchange = undeliverableExchange;
    this.threadPoolTaskExecutor = threadPoolTaskExecutor;
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
        send(rabbitTemplate, undeliverableExchange, undeliverableQueue, message)
    );
  }

}

