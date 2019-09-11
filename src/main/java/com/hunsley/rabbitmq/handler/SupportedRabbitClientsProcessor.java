package com.hunsley.rabbitmq.handler;

import java.util.List;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Component
public final class SupportedRabbitClientsProcessor implements BeanPostProcessor {

  private final MultiValueMap<String, MessageHandler> messageHandlers;

  public SupportedRabbitClientsProcessor() {
    this.messageHandlers = new LinkedMultiValueMap<>();
  }

  public List<MessageHandler> getClientMessageHandlers(final String client) {
    return messageHandlers.get(client);
  }

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
          messageHandlers.add(client, handler);
        }
      }
    }

    return bean;
  }
}

