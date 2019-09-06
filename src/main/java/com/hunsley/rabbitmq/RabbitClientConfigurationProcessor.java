package com.hunsley.rabbitmq;

import com.hunsley.rabbitmq.props.Client;
import com.hunsley.rabbitmq.props.RabbitProperties;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class RabbitClientConfigurationProcessor implements ApplicationContextAware {
  private static final String MESSAGE_LISTENER_NAME_SUFFIX = "MessageListenerContainer";
  private static final String BINDING_NAME_SUFFIX = "Binding";

  private final RabbitProperties rabbitProperties;
  private final SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory;
  private ApplicationContext applicationContext;

  @Autowired
  public RabbitClientConfigurationProcessor(RabbitProperties rabbitProperties,
      SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory) {
    this.rabbitProperties = rabbitProperties;
    this.simpleRabbitListenerContainerFactory = simpleRabbitListenerContainerFactory;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @PostConstruct
  public void initListeners() {
    ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
    Map<String, Client> clients = rabbitProperties.getClients();

    for(final String key : clients.keySet()) {
      Client client = clients.get(key);
      TopicExchange exchange = createExchange(client.getExchange());
      Queue queue = createQueue(client.getQueue());

      beanFactory.registerSingleton(client.getExchange(), exchange);
      beanFactory.registerSingleton(client.getQueue(), queue);
      beanFactory.registerSingleton(key+BINDING_NAME_SUFFIX, createBinding(queue, exchange, client.getRoutingKey()));
      beanFactory.registerSingleton(key+MESSAGE_LISTENER_NAME_SUFFIX, createListener(key, client.getQueue()));
    }
  }

  private TopicExchange createExchange(final String name) {
    return new TopicExchange(name);
  }

  private SimpleMessageListenerContainer createListener(final String key, final String queueName) {
    SimpleMessageListenerContainer container = simpleRabbitListenerContainerFactory.createListenerContainer();
    container.setQueueNames(queueName);
    container.setMessageListener(new MessageListenerImpl(key));
    return container;
  }

  private Queue createQueue(final String name) {
    return new Queue(name);
  }

  private Binding createBinding(Queue queue, TopicExchange exchange, final String routingKey) {
    return BindingBuilder.bind(queue).to(exchange).with(routingKey);
  }
}
