package com.hunsley.rabbitmq;

import com.hunsley.rabbitmq.props.Client;
import com.hunsley.rabbitmq.props.RabbitProperties;
import java.util.Map;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

@Component
public class RabbitClientConfigurationProcessor implements BeanFactoryPostProcessor {
  private static final String MESSAGE_LISTENER_NAME_SUFFIX = "MessageListenerContainer";

  @Autowired
  private RabbitProperties rabbitProperties;

  @Autowired
  private SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory;

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
      throws BeansException {
    Map<String, Client> clients = rabbitProperties.getClients();

    for(final String key : clients.keySet()) {
      Client client = clients.get(key);
      beanFactory.registerSingleton(client.getExchange(), createExchange(client.getExchange()));
      beanFactory.registerSingleton(key+MESSAGE_LISTENER_NAME_SUFFIX, createListener(client.getQueue()));
    }
  }

  private TopicExchange createExchange(final String name) {
    return new TopicExchange(name);
  }

  private SimpleMessageListenerContainer createListener(final String queueName) {
    SimpleMessageListenerContainer container = simpleRabbitListenerContainerFactory.createListenerContainer();
    container.setQueueNames(queueName);
    container.setMessageListener(message -> System.out.println(new String(message.getBody())));
    return container;
  }
}
