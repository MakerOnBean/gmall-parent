package cloud.makeronbean.gmall.client;

import cloud.makeronbean.gmall.client.impl.OrderDegradeFeignClientImpl;
import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.model.order.OrderInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author makeronbean
 */
@Component
@FeignClient(value = "service-order",fallback = OrderDegradeFeignClientImpl.class)
public interface OrderFeignClient {

    /**
     * 秒杀商品的下单
     */
    @PostMapping("/api/order/auth/submitOrderSeckill")
    Long submitOrderSeckill(@RequestBody OrderInfo orderInfo);


    /**
     * 根据订单Id查询订单
     */
    @GetMapping("/api/order/inner/getOrderInfo/{orderId}")
    OrderInfo getOrderInfo(@PathVariable Long orderId);

    /**
     * 去结算
     */
    @GetMapping("/api/order/auth/trade")
    Result trade();
}
