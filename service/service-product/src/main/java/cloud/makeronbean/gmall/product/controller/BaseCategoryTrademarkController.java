package cloud.makeronbean.gmall.product.controller;

import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.model.product.BaseTrademark;
import cloud.makeronbean.gmall.model.product.CategoryTrademarkVo;
import cloud.makeronbean.gmall.product.service.BaseCategoryTrademarkService;
import cloud.makeronbean.gmall.product.service.BaseTrademarkService;
import cloud.makeronbean.gmall.product.service.ManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author makeronbean
 */
@RestController
@RequestMapping("admin/product/baseCategoryTrademark")
public class BaseCategoryTrademarkController {

    @Autowired
    private BaseCategoryTrademarkService baseCategoryTrademarkService;

    /**
     * 保存添加的品牌与三级分类对应关系
     */
    @PostMapping("/save")
    public Result save(@RequestBody CategoryTrademarkVo categoryTrademarkVo){
        baseCategoryTrademarkService.save(categoryTrademarkVo);
        return Result.ok();
    }


    /**
     * 添加品牌时，获取可选择的品牌列表
     */
    @GetMapping("/findCurrentTrademarkList/{category3Id}")
    public Result<List<BaseTrademark>> findCurrentTrademarkList(@PathVariable Long category3Id){
        List<BaseTrademark> list = baseCategoryTrademarkService.findCurrentTrademarkList(category3Id);
        return Result.ok(list);
    }


    /**
     * 删除三级分类和品牌的关联关系
     */
    @DeleteMapping("/remove/{category3Id}/{trademarkId}")
    public Result remove(@PathVariable Long category3Id,
                         @PathVariable Long trademarkId){
        baseCategoryTrademarkService.removeByCategory3IdAndTrademarkId(category3Id,trademarkId);
        return Result.ok();
    }


    /**
     * 根据三级分类id查询所有品牌信息
     */
    @GetMapping("/findTrademarkList/{category3Id}")
    public Result<List<BaseTrademark>> findTrademarkList(@PathVariable Integer category3Id){
        List<BaseTrademark> list = baseCategoryTrademarkService.findTrademarkList(category3Id);
        return Result.ok(list);
    }
}