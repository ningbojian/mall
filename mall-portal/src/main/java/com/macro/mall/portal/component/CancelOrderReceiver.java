package com.macro.mall.portal.component;

import com.macro.mall.portal.service.OmsPortalOrderService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 *  取消订单消息的接收者
 * Created by macro on 2018/9/14.
 */
@Component
@RabbitListener(queues = "mall.order.cancel")
public class CancelOrderReceiver {
    private static final Logger LOGGER = LoggerFactory.getLogger(CancelOrderReceiver.class);
    @Autowired
    private OmsPortalOrderService portalOrderService;

    @RabbitHandler
    public void handle(Long orderId, Channel channel, Message message) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            portalOrderService.cancelOrder(orderId);
            // 业务处理成功才确认，消息从队列删除
            channel.basicAck(deliveryTag, false);
            LOGGER.info("process orderId:{}", orderId);
        } catch (Exception e) {
            LOGGER.error("process orderId:{} failed", orderId, e);
            // 处理失败：不重投（requeue=false），避免毒消息无限循环。
            // 订单最终一致性由 OrderTimeOutCancelTask 定时扫表兜底保证。
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
