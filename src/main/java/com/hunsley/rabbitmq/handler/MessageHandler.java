package com.hunsley.rabbitmq.handler;

import org.springframework.amqp.core.Message;

public interface MessageHandler {

  void handleMessage(Message message);

}
