package cloud.makeronbean.gmall.item.service.impl;

import cloud.makeronbean.gmall.client.ListFeignClient;
import cloud.makeronbean.gmall.client.ProductFeignClient;
import cloud.makeronbean.gmall.common.constant.RedisConst;
import cloud.makeronbean.gmall.item.service.ItemApiService;
import cloud.makeronbean.gmall.model.product.*;
import com.alibaba.fastjson.JSON;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author makeronbean
 */
@Service
public class ItemApiServiceImpl implements ItemApiService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private ListFeignClient listFeignClient;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ExecutorService executor;

    /**
     * 获取商品详细数据
     */
    @Override
    public Map<String, Object> getItem(Long skuId) {

        Map<String, Object> map = new HashMap<>();

        // 先判断布隆过滤器中是否存在该数据的映射，如果不存在直接返回
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
        if (!bloomFilter.contains(skuId)) {
            return map;
        }

        // skuInfo 携带图片信息的sku信息
        CompletableFuture<SkuInfo> skuInfoFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            map.put("skuInfo", skuInfo);
            return skuInfo;
        }, executor);

        // price 实时显示价格
        CompletableFuture<Void> priceFuture = CompletableFuture.runAsync(() -> {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            map.put("price", skuPrice);
        }, executor);

        // categoryView 左上角的三级分类
        CompletableFuture<Void> categoryViewFuture = skuInfoFuture.thenAcceptAsync(skuInfo -> {
            if (skuInfo != null) {
                BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
                map.put("categoryView", categoryView);
            }
        }, executor);


        // spuPosterList 商品海报
        CompletableFuture<Void> spuPosterListFuture = skuInfoFuture.thenAcceptAsync(skuInfo -> {
            if (skuInfo != null) {
                List<SpuPoster> spuPosterList = productFeignClient.findSpuPosterBySpuId(skuInfo.getSpuId());
                map.put("spuPosterList", spuPosterList);
            }
        }, executor);


        // spuSaleAttrList 销售属性和选择情况
        CompletableFuture<Void> spuSaleAttrListFuture = skuInfoFuture.thenAcceptAsync(skuInfo -> {
            if (skuInfo != null) {
                List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
                map.put("spuSaleAttrList", spuSaleAttrList);
            }
        }, executor);


        // 商品切换数据
        CompletableFuture<Void> skuValueIdsMapFuture = skuInfoFuture.thenAcceptAsync(skuInfo -> {
            if (skuInfo != null) {
                Map<Object, Object> skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
                map.put("valuesSkuJson", JSON.toJSONString(skuValueIdsMap));
            }
        }, executor);

        // skuAttrList 海报位置的平台属性与平台属性值
        CompletableFuture<Void> attrListFuture = CompletableFuture.runAsync(() -> {
            List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
            // 处理结果集
            List<Map<String, Object>> skuAttrList = attrList.stream().map(baseAttrInfo -> {
                Map<String, Object> map1 = new HashMap<>();
                map1.put("attrName", baseAttrInfo.getAttrName());
                map1.put("attrValue", baseAttrInfo.getAttrValueList().get(0).getValueName());
                return map1;
            }).collect(Collectors.toList());
            map.put("skuAttrList", skuAttrList);
        }, executor);

        // 被查询的商品热度 +1 ，通过调用 service-list 实现
        CompletableFuture<Void> incrHotScoreFuture = CompletableFuture.runAsync(() -> {
            listFeignClient.incrHotScore(skuId);
        }, executor);

        // 等待所有异步编排对象执行完毕后再返回
        CompletableFuture.allOf(skuInfoFuture, priceFuture,
                categoryViewFuture, spuPosterListFuture,
                spuSaleAttrListFuture, skuValueIdsMapFuture,
                attrListFuture, incrHotScoreFuture).join();

        return map;
    }
}
