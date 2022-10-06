package cloud.makeronbean.gmall.product.controller;

import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.model.product.BaseTrademark;
import cloud.makeronbean.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author makeronbean
 */
@RestController
@RequestMapping("/admin/product/baseTrademark")
public class BaseTrademarkController {

    @Autowired
    private BaseTrademarkService baseTrademarkService;


    /**
     * 根据id删除品牌
     */
    @DeleteMapping("/remove/{id}")
    public Result remove(@PathVariable Long id){
        baseTrademarkService.removeById(id);
        return Result.ok();
    }


    /**
     * 根据id查询品牌
     */
    @GetMapping("/get/{id}")
    public Result get(@PathVariable Long id){
        BaseTrademark baseTrademark = baseTrademarkService.getById(id);
        return Result.ok(baseTrademark);
    }


    /**
     * 修改品牌信息
     */
    @PutMapping("/update")
    public Result update(@RequestBody BaseTrademark baseTrademark){
        baseTrademarkService.updateById(baseTrademark);
        return Result.ok();
    }


    /**
     * 新增品牌信息
     */
    @PostMapping("/save")
    public Result save(@RequestBody BaseTrademark baseTrademark){
        baseTrademarkService.save(baseTrademark);
        return Result.ok();
    }


    /**
     * 分页查询品牌列表
     */
    @GetMapping("/{page}/{limit}")
    public Result<Page<BaseTrademark>> getBaseTrademarkPage(@PathVariable("page")Integer pageNum,
                                       @PathVariable Integer limit){
        Page<BaseTrademark> page = new Page<>(pageNum,limit);
        page = baseTrademarkService.getBaseTrademarkPage(page);
        return Result.ok(page);
    }

}
