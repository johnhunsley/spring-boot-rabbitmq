package com.hunsley.application;

import com.hunsley.rabbitmq.handler.MessageHandler;
import com.hunsley.rabbitmq.handler.SupportedRabbitClients;
import org.springframework.amqp.core.Message;

@SupportedRabbitClients({"command", "event"})
public class MyMessageHandlerImpl implements MessageHandler {

  @Override
  public void handleMessage(Message message) {
    System.out.println(new String(message.getBody()));
  }
}
