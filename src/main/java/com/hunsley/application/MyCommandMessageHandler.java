package com.hunsley.application;

import com.hunsley.rabbitmq.InvalidMessageException;
import com.hunsley.rabbitmq.handler.MessageHandler;
import com.hunsley.rabbitmq.handler.SupportedRabbitClients;
import org.springframework.amqp.core.Message;

@SupportedRabbitClients({"command"})
public class MyCommandMessageHandler implements MessageHandler {

  /**
   * Throw an InvalidMessageException so this gets dead lettered
   * @param message
   * @throws Exception
   */
  @Override
  public void handleMessage(Message message) throws Exception {
    System.out.println(new String(message.getBody()));
    throw new InvalidMessageException(message.toString());
  }
}
