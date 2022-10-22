package cloud.makeronbean.gmall.item.service;


import java.util.Map;

/**
 * @author makeronbean
 */
public interface ItemApiService {

    /**
     * 获取商品详细数据
     */
    Map<String,Object> getItem(Long skuId);
}
