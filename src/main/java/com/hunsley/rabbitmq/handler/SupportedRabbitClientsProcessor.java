package com.hunsley.rabbitmq.handler;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class SupportedRabbitClientsProcessor implements BeanPostProcessor {

  @Override
  public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
    return bean;
  }

  /**
   * only add beans which are subtypes of {@link MessageHandler} which are annotated with {@link SupportedRabbitClients}
   */
  @Override
  public Object postProcessAfterInitialization(final Object bean, final String beanName) {

    if(bean instanceof MessageHandler) {
      final MessageHandler handler = (MessageHandler)bean;
      SupportedRabbitClients supportedClients = bean.getClass().getAnnotation(SupportedRabbitClients.class);

      if (supportedClients != null) {
        for(String client : supportedClients.value()) {

        }
      }
    }

    return bean;
  }
}

