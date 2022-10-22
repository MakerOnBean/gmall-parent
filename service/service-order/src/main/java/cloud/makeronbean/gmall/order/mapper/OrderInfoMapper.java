package cloud.makeronbean.gmall.order.mapper;

import cloud.makeronbean.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author makeronbean
 */
@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    /**
     * 订单分页查询
     */
    IPage<OrderInfo> getOrderPage(IPage<OrderInfo> page,
                                  @Param("userId") Long userId,@Param("orderStatus") String orderStatus);


    /**
     * 根据订单Id查询订单
     */
    OrderInfo getOrderInfo(Long orderId);
}
