package com.hunsley.rabbitmq;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@ComponentScan
public class RabbitConfig {
  public static final String ORIGINAL_ROUTING_KEY = "original-routing-key";
  public static final String RETRIES_HEADER_KEY = "x-retries";
  public static final String DEAD_LETTER_EXCHANGE_HEADER = "x-dead-letter-exchange";
  public static final String DEAD_LETTER_ROUTING_KEY_HEADER = "x-dead-letter-routing-key";
  public static final String RETRY_TTL_HEADER = "x-message-ttl";

  @Bean
  public ThreadPoolTaskExecutor threadPoolTaskExecutor(
      @Value("${rabbit.threadPool.corePoolSize:5}") int corePoolSize,
      @Value("${rabbit.threadPool.maxPoolSize:10}") int maxPoolSize) {
    ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
    threadPoolTaskExecutor.setCorePoolSize(corePoolSize);
    threadPoolTaskExecutor.setMaxPoolSize(maxPoolSize);
    threadPoolTaskExecutor.initialize();
    return threadPoolTaskExecutor;
  }
}
