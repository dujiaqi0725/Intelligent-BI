package com.dujiaqi.intelligentBI.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 *  用于创建测试过程中使用的交换机和队列（只在运行测试前启动一次）
 */
public class MqInitMain {

    public static void main(String[] args) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            String EXCHANGE_NAME = "code_exchange";
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");

            String queueName1 = "code_queue";
            channel.queueDeclare(queueName1, true, false, false, null);
            channel.queueBind(queueName1,EXCHANGE_NAME,"my_routingKey");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
