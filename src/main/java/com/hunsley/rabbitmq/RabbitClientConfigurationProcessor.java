package com.hunsley.rabbitmq;

import static com.hunsley.rabbitmq.RabbitConfig.DEAD_LETTER_EXCHANGE_HEADER;
import static com.hunsley.rabbitmq.RabbitConfig.DEAD_LETTER_ROUTING_KEY_HEADER;
import static com.hunsley.rabbitmq.RabbitConfig.RETRY_TTL_HEADER;

import com.hunsley.rabbitmq.callbacks.ClientReturnCallback;
import com.hunsley.rabbitmq.handler.SupportedRabbitClientsProcessor;
import com.hunsley.rabbitmq.props.Client;
import com.hunsley.rabbitmq.props.GDPQueue;
import com.hunsley.rabbitmq.props.RabbitProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
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
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;


@Component
@DependsOn("supportedRabbitClientsProcessor")
public class RabbitClientConfigurationProcessor implements ApplicationContextAware {
  private Logger logger = LogManager.getLogger(RabbitClientConfigurationProcessor.class);

  private static final String MESSAGE_LISTENER_NAME_SUFFIX = "MessageListenerContainer";
  private static final String UNDELIVERABLE = "undeliverable";
  private static final String TEMPLATE_SUFFIX = "RabbitTemplate";

  private final RabbitProperties rabbitProperties;
  private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
  private final SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory;
  private final ConnectionFactory connectionFactory;
  private final SupportedRabbitClientsProcessor supportedRabbitClientsProcessor;
  private ApplicationContext applicationContext;

  @Autowired
  public RabbitClientConfigurationProcessor(RabbitProperties rabbitProperties,
      ThreadPoolTaskExecutor threadPoolTaskExecutor,
      SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory,
      ConnectionFactory connectionFactory,
      SupportedRabbitClientsProcessor supportedRabbitClientsProcessor) {
    this.rabbitProperties = rabbitProperties;
    this.threadPoolTaskExecutor = threadPoolTaskExecutor;
    this.simpleRabbitListenerContainerFactory = simpleRabbitListenerContainerFactory;
    this.connectionFactory = connectionFactory;
    this.supportedRabbitClientsProcessor = supportedRabbitClientsProcessor;
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
      Client client = clients.get(key);
      final String id = client.getId();
      logger.info("Creating client named - "+id);

      beanFactory.registerSingleton(id + "Declarables", createGDPDeclarables(client));
      RabbitTemplate template = createClientTemplate(client.getExchange());
      beanFactory.registerSingleton(id + TEMPLATE_SUFFIX, template);
      beanFactory.registerSingleton(id + MESSAGE_LISTENER_NAME_SUFFIX,
          createListener(client, client.getQueue() + GDPQueue.MAIN.value, template));
    }

    beanFactory.registerSingleton(UNDELIVERABLE+"Declarables", createUndeliverableDeliverables());
  }

  private Declarables createUndeliverableDeliverables() {
    List<Declarable> declarables = new ArrayList<>();
    declarables.add(createExchange(UNDELIVERABLE));
    declarables.add(QueueBuilder.durable(UNDELIVERABLE).build());
    return new Declarables(declarables);
  }

  private Declarables createGDPDeclarables(Client client) {
    List<Declarable> declarables = new ArrayList<>();
    TopicExchange exchange = createExchange(client.getExchange());
    declarables.add(exchange);

    //create the main queue and binding
    Queue mainQueue = QueueBuilder.durable(client.getQueue() + GDPQueue.MAIN.value)
        .withArgument(DEAD_LETTER_EXCHANGE_HEADER, client.getExchange())
        .withArgument(DEAD_LETTER_ROUTING_KEY_HEADER, client.getRoutingKey() + GDPQueue.DEAD_LETTER.value).build();
    declarables.add(
        BindingBuilder.bind(mainQueue).to(exchange).with(client.getRoutingKey() + GDPQueue.MAIN.value));

    for(String extraBinding : client.getAdditionalBindings()) {
      declarables.add(BindingBuilder.bind(mainQueue).to(exchange).with(extraBinding));
    }

    declarables.add(mainQueue);

    //create the retry queue
    Queue retryQueue = QueueBuilder.durable(client.getQueue() + GDPQueue.RETRY.value)
        .withArgument(DEAD_LETTER_EXCHANGE_HEADER, client.getExchange())
        .withArgument(DEAD_LETTER_ROUTING_KEY_HEADER, client.getRoutingKey() + GDPQueue.MAIN.value)
        .withArgument(RETRY_TTL_HEADER, client.getRetryTtl()).build();
    declarables.add(
        BindingBuilder.bind(retryQueue).to(exchange).with(client.getRoutingKey() + GDPQueue.RETRY.value));
    declarables.add(retryQueue);

    //create the dead letter queue
    Queue deadLetterQueue = QueueBuilder.durable(client.getQueue() + GDPQueue.DEAD_LETTER.value).build();
    declarables.add(
        BindingBuilder.bind(deadLetterQueue).to(exchange).with(client.getRoutingKey() + GDPQueue.DEAD_LETTER.value));
    declarables.add(deadLetterQueue);

    //create the invalid msg queue
    Queue invalidMsgQueue = QueueBuilder.durable(client.getQueue() + GDPQueue.INVALID.value).build();
    declarables.add(
        BindingBuilder.bind(invalidMsgQueue).to(exchange).with(client.getRoutingKey() + GDPQueue.INVALID.value));
    declarables.add(invalidMsgQueue);

    return new Declarables(declarables);
  }

  private TopicExchange createExchange(final String name) {
    return new TopicExchange(name);
  }

  private SimpleMessageListenerContainer createListener(Client client, final String queueName, RabbitTemplate rabbitTemplate) {
    SimpleMessageListenerContainer container = simpleRabbitListenerContainerFactory.createListenerContainer();
    container.setQueueNames(queueName);
    container.setDefaultRequeueRejected(false);
    container.setMessageListener(
        new ClientGDPMessageListenerImpl(
            client.getMaxRetries(),
            rabbitTemplate,
            supportedRabbitClientsProcessor.getClientMessageHandlers(client.getId()),
            client));
    return container;
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


