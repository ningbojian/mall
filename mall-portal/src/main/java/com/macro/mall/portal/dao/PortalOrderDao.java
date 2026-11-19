package com.macro.mall.portal.dao;

import com.macro.mall.model.OmsOrderItem;
import com.macro.mall.portal.domain.OmsOrderDetail;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 前台订单管理自定义Dao
 * Created by macro on 2018/9/4.
 */
public interface PortalOrderDao {
    /**
     * 获取订单及下单商品详情
     */
    OmsOrderDetail getDetail(@Param("orderId") Long orderId);

    /**
     * 修改 pms_sku_stock表的锁定库存及真实库存
     */
    int updateSkuStock(@Param("itemList") List<OmsOrderItem> orderItemList);

    /**
     * 获取超时订单
     * @param minute 超时时间（分）
     */
    List<OmsOrderDetail> getTimeOutOrders(@Param("minute") Integer minute);

    /**
     * 批量修改订单状态
     */
    int updateOrderStatus(@Param("ids") List<Long> ids,@Param("status") Integer status);

    /**
     * 解除取消订单的库存锁定
     */
    int releaseSkuStockLock(@Param("itemList") List<OmsOrderItem> orderItemList);

    /**
     * 原子锁定库存：仅当可售库存(stock - lock_stock)充足时才增加锁定库存。
     * 判断与写入在一条 UPDATE 中由数据库行锁保证原子性，避免超卖。
     *
     * @return 影响行数，0 表示库存不足（锁定失败）
     */
    int lockStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    /**
     * 原子抢占支付处理权：仅当订单仍为待付款(status=0)时才更新为已支付。
     * 判断与写入在一条 UPDATE 中由数据库行锁保证原子性，用于支付回调幂等。
     *
     * @return 影响行数，1 表示抢到处理权，0 表示订单已被处理（重复回调）
     */
    int paySuccess(@Param("orderId") Long orderId, @Param("payType") Integer payType);

    /**
     * 原子抢占单笔取消处理权：仅当订单仍为待付款(status=0且未删除)时才改为取消(status=4)。
     * 判断与写入在一条 UPDATE 中由数据库行锁保证原子性，供 MQ 消费者与定时器共用同一道幂等门。
     *
     * @return 影响行数，1 表示抢到处理权，0 表示订单已被取消/支付（重复消息）
     */
    int cancelOrder(@Param("orderId") Long orderId);

}
