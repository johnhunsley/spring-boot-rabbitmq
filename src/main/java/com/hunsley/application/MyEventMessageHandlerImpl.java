package com.hunsley.application;

import com.hunsley.rabbitmq.handler.MessageHandler;
import com.hunsley.rabbitmq.handler.SupportedRabbitClients;
import org.springframework.amqp.core.Message;

@SupportedRabbitClients({"event"})
public class MyEventMessageHandlerImpl implements MessageHandler {

  /**
   * throw a generic excpetion so this gets retried
   * @param message
   * @throws Exception
   */
  @Override
  public void handleMessage(Message message) throws Exception {
    System.out.println(new String(message.getBody()));
    throw new Exception(message.toString());
  }
}
