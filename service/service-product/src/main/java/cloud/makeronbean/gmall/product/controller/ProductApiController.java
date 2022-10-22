package cloud.makeronbean.gmall.product.controller;

import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.model.product.*;
import cloud.makeronbean.gmall.product.service.ManagerService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 远程调用 服务降级
 * @author makeronbean
 */
@RestController
@RequestMapping("/api/product/inner")
public class ProductApiController {

    @Autowired
    private ManagerService managerService;

    /**
     * 根据品牌id获取品牌属性
     */
    @GetMapping("/getTrademark/{tmId}")
    public BaseTrademark getTrademark(@PathVariable Long tmId){
        return managerService.getTrademark(tmId);
    }


    /**
     * 查询所有三级分类id
     * 首页显示使用
     */
    @GetMapping("/getBaseCategoryList")
    public Result<List<JSONObject>> getBaseCategoryList(){
        List<JSONObject> resultList = managerService.getBaseCategoryList();
        return Result.ok(resultList);
    }


    /**
     * 根据skuId查询平台属性
     */
    @GetMapping("/getAttrList/{skuId}")
    public List<BaseAttrInfo> getAttrList(@PathVariable Long skuId){
        return managerService.getAttrList(skuId);
    }


    /**
     * 根据spuId获取海报数据
     */
    @GetMapping("/findSpuPosterBySpuId/{spuId}")
    public List<SpuPoster> findSpuPosterBySpuId(@PathVariable Long spuId){
        return managerService.findSpuPosterBySpuId(spuId);
    }



    /**
     * 根据spuId获取销售属性值id与skuId组成的数据集
     */
    @GetMapping("/getSkuValueIdsMap/{spuId}")
    public Map<Object,Object> getSkuValueIdsMap(@PathVariable Long spuId){
        return managerService.getSkuValueIdsMap(spuId);
    }


    /**
     * 根据spuId，skuId 查询销售属性集合
     */
    @GetMapping("/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId,
                                                         @PathVariable Long spuId){
        return managerService.getSpuSaleAttrListCheckBySku(skuId,spuId);
    }


    /**
     * 根据skuId查询商品价格
     * 价格数据不缓存，实时查询
     */
    @GetMapping("/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId){
        return managerService.getSkuPrice(skuId);
    }


    /**
     * 根据skuId查询带图片列表的SpuInfo信息
     */
    @GetMapping("/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId){
        return managerService.getSkuInfo(skuId);
    }


    /**
     * 根据三级分类id获取分类信息
     */
    @GetMapping("/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id){
        return managerService.getCategoryView(category3Id);
    }
}
