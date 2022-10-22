package cloud.makeronbean.gmall.list.service;

import cloud.makeronbean.gmall.model.list.SearchParam;
import cloud.makeronbean.gmall.model.list.SearchResponseVo;

/**
 * @author makeronbean
 */
public interface SearchService {

    /**
     * 商品上架
     */
    void upperGoods(Long skuId);

    /**
     * 商品下架
     */
    void lowerGoods(Long skuId);

    /**
     * 更改商品热度排名
     */
    void incrHotScore(Long skuId);

    /**
     * 商品搜索
     */
    SearchResponseVo search(SearchParam searchParam);
}
