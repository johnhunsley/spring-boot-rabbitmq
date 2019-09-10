package com.hunsley.rabbitmq;

import com.hunsley.rabbitmq.callbacks.ClientReturnCallback;
import com.hunsley.rabbitmq.props.Client;
import com.hunsley.rabbitmq.props.GDPQueue;
import com.hunsley.rabbitmq.props.RabbitProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;


@Component
public class RabbitClientConfigurationProcessor implements ApplicationContextAware {
  private Logger logger = LogManager.getLogger(RabbitClientConfigurationProcessor.class);

  private static final String MESSAGE_LISTENER_NAME_SUFFIX = "MessageListenerContainer";
  private static final String UNDELIVERABLE = "undeliverable";
  private static final String TEMPLATE_SUFFIX = "RabbitTemplate";

  private final RabbitProperties rabbitProperties;
  private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
  private final SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory;
  private final ConnectionFactory connectionFactory;
  private ApplicationContext applicationContext;



  @Autowired
  public RabbitClientConfigurationProcessor(RabbitProperties rabbitProperties,
      ThreadPoolTaskExecutor threadPoolTaskExecutor,
      SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory,
      ConnectionFactory connectionFactory) {
    this.rabbitProperties = rabbitProperties;
    this.threadPoolTaskExecutor = threadPoolTaskExecutor;
    this.simpleRabbitListenerContainerFactory = simpleRabbitListenerContainerFactory;
    this.connectionFactory = connectionFactory;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @PostConstruct
  public void initClients() {
    logger.info("Initializing Rabbit Clients...............");
    ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
    Map<String, Client> clients = rabbitProperties.getClients();

    for(final String key : clients.keySet()) {
      logger.info("Creating client named - "+key);
      Client client = clients.get(key);

      beanFactory.registerSingleton(key + "Declarables", createGDPDeclarables(client));
      beanFactory.registerSingleton(key + MESSAGE_LISTENER_NAME_SUFFIX,
          createListener(key, client.getQueue() + GDPQueue.MAIN.value));
      beanFactory.registerSingleton(key + TEMPLATE_SUFFIX, createClientTemplate(client.getExchange()));
    }

    beanFactory.registerSingleton(UNDELIVERABLE+"Declarables", createUndeliverableDeliverables());
  }

  private Declarables createUndeliverableDeliverables() {
    List<Declarable> declarables = new ArrayList<>();
    declarables.add(createExchange(UNDELIVERABLE));
    declarables.add(createQueue(UNDELIVERABLE));
    return new Declarables(declarables);
  }

  private Declarables createGDPDeclarables(Client client) {
    List<Declarable> declarables = new ArrayList<>();
    TopicExchange exchange = createExchange(client.getExchange());
    declarables.add(exchange);

    for(GDPQueue gdpQueue : GDPQueue.values()) {
      Queue queue = createQueue(client.getQueue() + gdpQueue.value);
      declarables.add(queue);
      declarables.add(createBinding(queue, exchange, client.getRoutingKey() + gdpQueue.value));
    }

    return new Declarables(declarables);
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

  private RabbitTemplate createClientTemplate(final String exchange) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
      if (!ack) {
        logger.error("NACK received in ConfirmCallback. Message not delivered to Rabbit. Cause: {}\n{}",
            cause, correlationData);
      }
    });

    rabbitTemplate.setReturnCallback(new ClientReturnCallback(connectionFactory, UNDELIVERABLE, threadPoolTaskExecutor));
    rabbitTemplate.setExchange(exchange);
    rabbitTemplate.setMandatory(true);
    return rabbitTemplate;

  }

}


