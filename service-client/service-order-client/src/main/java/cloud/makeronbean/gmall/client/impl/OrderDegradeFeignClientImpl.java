package cloud.makeronbean.gmall.client.impl;

import cloud.makeronbean.gmall.client.OrderFeignClient;
import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.model.order.OrderInfo;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * @author makeronbean
 */
@Component
public class OrderDegradeFeignClientImpl implements OrderFeignClient {
    @Override
    public Long submitOrderSeckill(OrderInfo orderInfo) {
        return null;
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        return null;
    }

    @Override
    public Result trade() {
        return Result.fail();
    }
}
