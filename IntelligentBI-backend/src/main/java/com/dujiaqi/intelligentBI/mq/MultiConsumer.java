package com.dujiaqi.intelligentBI.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class MultiConsumer {

    private static final String TASK_QUEUE_NAME = "multi_queue";

    public static void main(String[] argv) throws Exception {
        // 建立连接
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        final Connection connection = factory.newConnection();
        for (int i = 0; i < 2; i++) {
            final Channel channel = connection.createChannel();

            channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

            // 控制单个消费者的处理任务积压数
            channel.basicQos(1);

            // 定义了如何处理消息
            int finalI = i;
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                try {
                    // 处理工作
                    System.out.println(" [x] Received '" + "编号:" + finalI  + "消息:" + message + "'");
                    // 停 20 秒，模拟机器处理能力有限
                    Thread.sleep(20000);
                    // 参数 multiple 批量确认：是指是否要一次性确认所有的历史消息直到当前这条
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag() , false);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    // 参数 requeue 表示是否重新入队，可用于重试
                    channel.basicReject(delivery.getEnvelope().getDeliveryTag() ,false);
                } finally {
                    System.out.println(" [x] Done");
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }
            };
            // 开启消费者监听
            // autoAck 参数 为了保证消息成功被消费
            channel.basicConsume(TASK_QUEUE_NAME, false, deliverCallback, consumerTag -> {
            });
        }
    }
}
