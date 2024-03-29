package com.hunsley.application;

import com.hunsley.rabbitmq.EnableRabbitmqClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRabbitmqClients
public class Application {

  public static void main(String[] args) throws InterruptedException {
    SpringApplication.run(Application.class, args);
  }

}