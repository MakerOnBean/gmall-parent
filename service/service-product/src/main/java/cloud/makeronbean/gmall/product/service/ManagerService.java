package cloud.makeronbean.gmall.product.service;

import cloud.makeronbean.gmall.model.product.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

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
}
