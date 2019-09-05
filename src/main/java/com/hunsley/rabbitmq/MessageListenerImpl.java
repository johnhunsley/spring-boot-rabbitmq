package com.hunsley.rabbitmq;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.stereotype.Component;

@Component("myMessageListener")
public class MessageListenerImpl implements MessageListener {

  @Override
  public void onMessage(Message message) {
    System.out.println(new String(message.getBody()));
  }
}
