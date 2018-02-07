package com.nextyu.rabbitmq.producer;

import cn.hutool.core.lang.Console;
import com.nextyu.rabbitmq.RabbitMQConfig;
import com.nextyu.rabbitmq.RedisConfig;
import com.nextyu.rabbitmq.util.DefaultKeyGenerator;
import com.nextyu.rabbitmq.util.RabbitMetaMessage;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProducerController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DefaultKeyGenerator keyGenerator;

    @GetMapping("/sendMessage")
    public Object sendMessage() {
        new Thread(() -> {
            HashOperations hashOperations = redisTemplate.opsForHash();
            for (int i = 0; i < 1; i++) {
                String id = keyGenerator.generateKey() + "";
                String value = "message " + i;
                RabbitMetaMessage rabbitMetaMessage = new RabbitMetaMessage(value);

                // 先把消息存储到 redis
                hashOperations.put(RedisConfig.RETRY_KEY, id, rabbitMetaMessage);

                Console.log("send message = {}", value);

                // 再发送到 rabbitmq
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, value, new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        message.getMessageProperties().setMessageId(id);
                        return message;
                    }
                }, new CorrelationData(id));
            }
        }).start();


        return "ok";
    }

}