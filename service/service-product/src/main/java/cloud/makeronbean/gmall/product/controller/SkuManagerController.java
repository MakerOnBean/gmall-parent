package cloud.makeronbean.gmall.product.controller;

import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.model.product.SkuInfo;
import cloud.makeronbean.gmall.model.product.SpuImage;
import cloud.makeronbean.gmall.model.product.SpuSaleAttr;
import cloud.makeronbean.gmall.product.service.ManagerService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author makeronbean
 */
@RestController
@RequestMapping("/admin/product")
public class SkuManagerController {


    @Autowired
    private ManagerService managerService;

    /**
     * 商品下架
     */
    @GetMapping("/cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId){
        managerService.cancelSale(skuId);
        return Result.ok();
    }


    /**
     * 商品上架
     */
    @GetMapping("/onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId){
        managerService.onSale(skuId);
        return Result.ok();
    }


    /**
     * 分页查询sku
     */
    @GetMapping("/list/{page}/{limit}")
    public Result getSkuInfoPage(@PathVariable("page") Integer pageNum,
                                 @PathVariable Integer limit){

        Page<SkuInfo> page = new Page<>(pageNum,limit);
        page = managerService.getSkuInfoPage(page);
        return Result.ok(page);
    }


    /**
     * 保存新增的SpuInfo信息
     */
    @PostMapping("/saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo){
        managerService.saveSkuInfo(skuInfo);
        return Result.ok();
    }


    /**
     * 根据spuId查询SpuImage集合
     */
    @GetMapping("/spuImageList/{spuId}")
    public Result<List<SpuImage>> getSpuImageList(@PathVariable Long spuId){
        List<SpuImage> list = managerService.getSpuImageList(spuId);
        return Result.ok(list);
    }


    /**
     * 根据spuId查询销售属性
     */
    @GetMapping("/spuSaleAttrList/{spuId}")
    public Result<List<SpuSaleAttr>> getSpuSaleAttrList(@PathVariable Long spuId){
        List<SpuSaleAttr> list = managerService.getSpuSaleAttrList(spuId);
        return Result.ok(list);
    }
}
