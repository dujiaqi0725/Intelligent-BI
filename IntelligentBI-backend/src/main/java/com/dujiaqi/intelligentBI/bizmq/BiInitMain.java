package com.dujiaqi.intelligentBI.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 *  用于创建智能BI中使用的交换机和队列（只在运行前启动一次）
 */
public class BiInitMain {

    public static void main(String[] args) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            String EXCHANGE_NAME = BiMqConstant.BI_EXCHANGE_NAME;
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");

            String queueName1 = BiMqConstant.BI_QUEUE_NAME;
            channel.queueDeclare(queueName1, true, false, false, null);
            channel.queueBind(queueName1,EXCHANGE_NAME,BiMqConstant.BI_ROUTING_KEY);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
