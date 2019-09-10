package com.hunsley.rabbitmq;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;

public class ClientGDPMessageListenerImpl implements MessageListener {
  private final String key;

  public ClientGDPMessageListenerImpl(String key) {
    this.key = key;
  }

  @Override
  public void onMessage(Message message) {
    System.out.println(key + ": " + new String(message.getBody()));

    //todo deal with GDP routing
  }
}
