package com.hunsley.rabbitmq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@EnableConfigurationProperties(RabbitProperties.class)
public class Application {

//  static final String topicExchangeName = "spring-boot-exchange";
//
//  static final String queueName = "spring-boot";
//
//  @Bean
//  Queue queue() {
//    return new Queue(queueName, false);
//  }
//
//  @Bean
//  TopicExchange exchange() {
//    return new TopicExchange(topicExchangeName);
//  }
//
//  @Bean
//  Binding binding(Queue queue, TopicExchange exchange) {
//    return BindingBuilder.bind(queue).to(exchange).with("foo.bar.#");
//  }
//
//  @Bean
//  SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
//      @Qualifier("myMessageListener") MessageListener messageListener) {
//    SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
//    container.setConnectionFactory(connectionFactory);
//    container.setQueueNames(queueName);
//    container.setMessageListener(messageListener);
//    return container;
//  }


  public static void main(String[] args) throws InterruptedException {
    SpringApplication.run(Application.class, args);
  }

}