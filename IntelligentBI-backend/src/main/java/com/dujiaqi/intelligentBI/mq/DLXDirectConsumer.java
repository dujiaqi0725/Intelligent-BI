package com.dujiaqi.intelligentBI.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.util.HashMap;
import java.util.Map;

public class DLXDirectConsumer {

  private static final String EXCHANGE_NAME = "direct2_exchange";

  private static final String DEAD_EXCHANGE_NAME = "dlx_direct_exchange";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    channel.exchangeDeclare(EXCHANGE_NAME, "direct");

    // 指定死信队列
    Map<String,Object> args1 = new HashMap<>();
    // 要绑定到哪个交换机
    args1.put("x-dead-letter-exchange",DEAD_EXCHANGE_NAME);
    // 指定死信发到哪一个死信队列
    args1.put("x-dead-letter-routing-key","waibao");

    String queueName1 = "xiaodog_queue";
    channel.queueDeclare(queueName1, true, false, false, args1);
    channel.queueBind(queueName1,EXCHANGE_NAME,"xiaodog");

    Map<String,Object> args2 = new HashMap<>();
    args2.put("x-dead-letter-exchange",DEAD_EXCHANGE_NAME);
    args2.put("x-dead-letter-routing-key","laoban");

    String queueName2 = "xiaocat_queue";
    channel.queueDeclare(queueName2, true, false, false, args2);
    channel.queueBind(queueName2,EXCHANGE_NAME,"xiaocat");

    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

    DeliverCallback deliverCallback1 = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        // 拒接消息去触发死信，将消息放入死信队列
        channel.basicNack(delivery.getEnvelope().getDeliveryTag() , false,false);
        System.out.println(" [xiaodog] Received '" +
            delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
    };
      DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
          String message = new String(delivery.getBody(), "UTF-8");
          channel.basicNack(delivery.getEnvelope().getDeliveryTag() , false,false);
          System.out.println(" [xiaocat] Received '" +
                  delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
      };
    channel.basicConsume(queueName1, false, deliverCallback1, consumerTag -> { });
    channel.basicConsume(queueName2, false, deliverCallback2, consumerTag -> { });
  }
}