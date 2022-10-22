package cloud.makeronbean.gmall.client.impl;

import cloud.makeronbean.gmall.client.ProductFeignClient;
import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.model.product.*;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 每个方法的降级处理
 * <p/>
 * @author makeronbean
 */
@Component
public class ProductDegradeFeignClientImpl implements ProductFeignClient {


    @Override
    public BaseTrademark getTrademark(Long tmId) {
        return null;
    }

    @Override
    public Result<List<JSONObject>> getBaseCategoryList() {
        return null;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        return null;
    }

    @Override
    public List<SpuPoster> findSpuPosterBySpuId(Long spuId) {
        return null;
    }

    @Override
    public Map<Object, Object> getSkuValueIdsMap(Long spuId) {
        return null;
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        return null;
    }

    @Override
    public BigDecimal getSkuPrice(Long skuId) {
        return null;
    }

    @Override
    public SkuInfo getSkuInfo(Long skuId) {
        return null;
    }

    @Override
    public BaseCategoryView getCategoryView(Long category3Id) {
        return null;
    }
}
