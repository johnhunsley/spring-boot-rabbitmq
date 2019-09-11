package com.hunsley.rabbitmq.props;

public class Client {
  private String id;
  private String exchange;
  private String queue;
  private int maxRetries;
  private String routingKey;
  private String[] additionalBindings;

  public String getExchange() {
    return exchange;
  }

  public void setExchange(String exchange) {
    this.exchange = exchange;
  }

  public String getQueue() {
    return queue;
  }

  public void setQueue(String queue) {
    this.queue = queue;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  public String getRoutingKey() {
    return routingKey;
  }

  public void setRoutingKey(String routingKey) {
    this.routingKey = routingKey;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String[] getAdditionalBindings() {
    return additionalBindings;
  }

  public void setAdditionalBindings(String[] additionalBindings) {
    this.additionalBindings = additionalBindings;
  }
}
