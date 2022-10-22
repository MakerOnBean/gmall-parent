package cloud.makeronbean.gmall.product.service;

import cloud.makeronbean.gmall.model.product.*;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author makeronbean
 */
public interface ManagerService {

    /**
     * 获取所有一级分类
     */
    List<BaseCategory1> getCategory1();

    /**
     * 获取指定一级分类下所有的二级分类
     */
    List<BaseCategory2> getCategory2(Long category1Id);

    /**
     * 获取指定二级分类下所有的三级分类
     */
    List<BaseCategory3> getCategory3(Long category2Id);

    /**
     * 根据分类查询平台属性
     * 查询一级、二级、三级分类下的数据，关系为or
     */
    List<BaseAttrInfo> attrInfoList(Long category1Id, Long category2Id, Long category3Id);

    /**
     * 保存或修改平台属性
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     * 根据平台属性id查询平台属性值集合
     */
    BaseAttrInfo getAttrInfo(Long attrId);


    /**
     * 根据三级分类id进行分页查询spu
     */
    Page<SpuInfo> getSpuInfoPage(Page<SpuInfo> page, Long category3Id);

    /**
     * 获取销售属性集合
     */
    List<BaseSaleAttr> getBaseSaleAttrList();

    /**
     * 保存新增的spu信息
     */
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 根据spuid查询销售属性
     */
    List<SpuSaleAttr> getSpuSaleAttrList(Long spuId);

    /**
     * 根据spuId查询SpuImage集合
     */
    List<SpuImage> getSpuImageList(Long spuId);

    /**
     * 保存新增的SpuInfo信息
     */
    void saveSkuInfo(SkuInfo skuInfo);

    /**
     * 分页查询sku
     */
    Page<SkuInfo> getSkuInfoPage(Page<SkuInfo> page);

    /**
     * 商品上架
     */
    void onSale(Long skuId);

    /**
     * 商品下架
     */
    void cancelSale(Long skuId);

    /**
     * 根据skuId查询带图片列表的SpuInfo信息
     */
    SkuInfo getSkuInfo(Long skuId);

    /**
     * 根据三级分类id获取分类信息
     */
    BaseCategoryView getCategoryView(Long category3Id);


    /**
     * 根据skuId查询商品价格
     */
    BigDecimal getSkuPrice(Long skuId);

    /**
     * 根据spuId，skuId 查询销售属性集合
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId);

    /**
     * 根据spuId获取销售属性值id与skuId组成的数据集
     */
    Map<Object, Object> getSkuValueIdsMap(Long spuId);

    /**
     * 根据spuId获取海报数据
     */
    List<SpuPoster> findSpuPosterBySpuId(Long spuId);

    /**
     * 根据skuId查询平台属性
     */
    List<BaseAttrInfo> getAttrList(Long skuId);

    /**
     * 重制布隆过滤器
     */
    void remakeBloomFilter();

    /**
     * 查询所有三级分类id
     * 首页显示使用
     */
    List<JSONObject> getBaseCategoryList();

    /**
     * 根据品牌id获取品牌属性
     */
    BaseTrademark getTrademark(Long tmId);
}
