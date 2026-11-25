package com.macro.mall.portal.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 可靠性配置：生产者确认 + 无法投递退回。
 *
 * 解决"消息丢失"的前两个环节：
 * 1. confirm 回调：消息是否到达交换机。nack 时记录日志，便于告警/补偿。
 * 2. return 回调：消息到达交换机但无队列匹配（mandatory=true 时退回），
 *    弥补 confirm 无法发现"静默丢弃"的盲区。
 *
 * 注意：交换机/队列/消息的持久化由 RabbitMqConfig 的 durable 配置保证，
 * 消费者手动 ack 由 application.yml 的 acknowledge-mode: manual 保证。
 */
@Configuration
public class RabbitReliabilityConfig {

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);

        // ① 生产者确认：消息到交换机 ack，没到 nack
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                // 生产环境此处应记录日志并触发告警/重发补偿；
                // 本项目依赖定时扫表兜底，故仅记录原因。
                System.err.println("[MQ confirm] 消息未到达交换机, cause=" + cause
                        + ", correlationData=" + correlationData);
            }
        });

        // ② 退回回调：到了交换机但路由不到队列时触发（需 mandatory=true）
        template.setReturnsCallback(returned -> {
            System.err.println("[MQ return] 消息无法投递到队列, exchange="
                    + returned.getExchange() + ", routingKey=" + returned.getRoutingKey()
                    + ", replyCode=" + returned.getReplyCode());
        });
        template.setMandatory(true);

        return template;
    }
}
