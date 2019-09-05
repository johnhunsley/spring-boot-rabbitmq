package com.hunsley.rabbitmq.props;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rabbit")
public class RabbitProperties {
  private Map<String, Client> clients = new HashMap<>();

  public Map<String, Client> getClients() {
    return clients;
  }

  public void setClients(Map<String, Client> clients) {
    this.clients = clients;
  }
}
