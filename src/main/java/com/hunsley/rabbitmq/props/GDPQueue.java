package com.hunsley.rabbitmq.props;

public enum GDPQueue {
  MAIN(".main"),
  DEAD_LETTER(".deadletter"),
  RETRY(".retry"),
  INVALID(".invalidMessage");

  public final String value;

  GDPQueue(final String value) {
    this.value = value;
  }


}
