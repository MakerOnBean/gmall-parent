package cloud.makeronbean.gmall.order.service;

import cloud.makeronbean.gmall.model.enums.ProcessStatus;
import cloud.makeronbean.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * @author makeronbean
 */
public interface OrderService extends IService<OrderInfo> {

    /**
     * 下订单
     */
    Long submitOrder(OrderInfo orderInfo);

    /**
     * 生成流水号，并存储到redis中
     */
    String getTradeNo(String userId);

    /**
     * 校验流水号
     */
    boolean checkTradeNo(String userId, String tradeNoCode);

    /**
     * 删除流水号
     */
    void deleteTradeNo(String userId);

    /**
     * 验证库存
     */
    boolean checkStock(String skuId, String skuNum);

    /**
     * 进入我的订单
     */
    IPage<OrderInfo> getOrderPage(Page<OrderInfo> page, Integer pageNum, Integer limit, Long userId, String orderStatus);

    /**
     * 处理过期订单
     */
    void execExpiredOrder(Long orderId, String flag);

    /**
     * 根据订单Id 修改订单的状态
     */
    void updateOrderStatus(Long orderId, ProcessStatus processStatus);

    /**
     * 根据订单Id查询订单
     */
    OrderInfo getOrderInfo(Long orderId);

    /**
     * 发送消息给第三方库存，扣减库存
     */
    void sendOrderStatus(Long orderId);

    /**
     * 构建用于返回给库存系统的map集合
     */
    Map<String, Object> getInitWareParamMap(OrderInfo orderInfo);

    /**
     * 拆单实现
     */
    List<OrderInfo> orderSplit(String orderIdStr, String wareSkuListStr);
}
