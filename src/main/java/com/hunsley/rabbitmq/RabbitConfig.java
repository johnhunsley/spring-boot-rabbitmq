package com.hunsley.rabbitmq;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@ComponentScan
public class RabbitConfig {


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
