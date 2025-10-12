package com.dujiaqi.intelligentBI.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.util.Scanner;

public class DLXDirectProducer {

  private static final String DEAD_EXCHANGE_NAME = "dlx_direct_exchange";

  private static final String WORK_EXCHANGE_NAME = "direct2_exchange";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    try (Connection connection = factory.newConnection();
         Channel channel = connection.createChannel()) {
        // 声明死信交换机
        channel.exchangeDeclare(DEAD_EXCHANGE_NAME, "direct");

        // 创建队列
        String queueName1 = "laoban_dlx_queue";
        channel.queueDeclare(queueName1,true,false,false,null);
        channel.queueBind(queueName1, DEAD_EXCHANGE_NAME,"laoban");

        String queueName2 = "waibao_dlx_queue";
        channel.queueDeclare(queueName2,true,false,false,null);
        channel.queueBind(queueName2, DEAD_EXCHANGE_NAME,"waibao");

        DeliverCallback deliverCallback1 = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [laoban] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [waibao] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };

        channel.basicConsume(queueName1, false, deliverCallback1, consumerTag -> { });
        channel.basicConsume(queueName2, false, deliverCallback2, consumerTag -> { });


        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String userInput = scanner.nextLine();
            String[] strings = userInput.split(" ");
            if (strings.length < 1) {
                continue;
            }
            String message = strings[0];
            String routingKey = strings[1];

            channel.basicPublish(WORK_EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));
            System.out.println(" [x] Sent '" + message + "with routing" + routingKey + "'");
        }
    }
  }
  //..
}