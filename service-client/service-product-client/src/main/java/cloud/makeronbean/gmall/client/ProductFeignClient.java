package cloud.makeronbean.gmall.client;

import cloud.makeronbean.gmall.client.impl.ProductDegradeFeignClientImpl;
import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.model.product.*;
import com.alibaba.fastjson.JSONObject;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author makeronbean
 */
@FeignClient(value = "service-product",fallback = ProductDegradeFeignClientImpl.class)
@Component
public interface ProductFeignClient {

    /**
     * 根据品牌id获取品牌属性
     */
    @GetMapping("/api/product/inner/getTrademark/{tmId}")
    BaseTrademark getTrademark(@PathVariable Long tmId);


    /**
     * 查询所有三级分类id
     * 首页显示使用
     */
    @GetMapping("/api/product/inner/getBaseCategoryList")
    Result<List<JSONObject>> getBaseCategoryList();

    /**
     * 根据skuId查询平台属性
     */
    @GetMapping("/api/product/inner/getAttrList/{skuId}")
    List<BaseAttrInfo> getAttrList(@PathVariable Long skuId);


    /**
     * 根据spuId获取海报数据
     */
    @GetMapping("/api/product/inner/findSpuPosterBySpuId/{spuId}")
    List<SpuPoster> findSpuPosterBySpuId(@PathVariable Long spuId);


    /**
     * 根据spuId获取销售属性值id与skuId组成的数据集
     */
    @GetMapping("/api/product/inner/getSkuValueIdsMap/{spuId}")
    Map<Object,Object> getSkuValueIdsMap(@PathVariable Long spuId);


    /**
     * 根据spuId，skuId 查询销售属性集合
     */
    @GetMapping("/api/product/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId, @PathVariable Long spuId);


    /**
     * 根据skuId查询商品价格
     * 价格数据不缓存，实时查询
     */
    @GetMapping("/api/product/inner/getSkuPrice/{skuId}")
    BigDecimal getSkuPrice(@PathVariable Long skuId);


    /**
     * 根据skuId查询带图片列表的SpuInfo信息
     */
    @GetMapping("/api/product/inner/getSkuInfo/{skuId}")
    SkuInfo getSkuInfo(@PathVariable Long skuId);


    /**
     * 根据三级分类id获取分类信息
     */
    @GetMapping("/api/product/inner/getCategoryView/{category3Id}")
    BaseCategoryView getCategoryView(@PathVariable Long category3Id);
}
