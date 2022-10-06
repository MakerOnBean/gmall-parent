package cloud.makeronbean.gmall.product.controller;

import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.model.product.BaseSaleAttr;
import cloud.makeronbean.gmall.model.product.SpuInfo;
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
public class SpuManagerController {

    @Autowired
    private ManagerService managerService;

    /**
     * 保存新增的spu信息
     */
    @PostMapping("/saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo){
        managerService.saveSpuInfo(spuInfo);
        return Result.ok();
    }


    /**
     * 获取销售属性集合
     */
    @GetMapping("/baseSaleAttrList")
    public Result<List<BaseSaleAttr>> getBaseSaleAttrList(){
        List<BaseSaleAttr> list = managerService.getBaseSaleAttrList();
        return Result.ok(list);
    }


    /**
     * 根据三级分类id进行分页查询spu
     */
    @GetMapping("/{page}/{limit}")
    public Result<Page<SpuInfo>> getSpuInfoList(@PathVariable("page") Integer pageNum,
                                                @PathVariable Integer limit,
                                                @RequestParam("category3Id") Long category3Id){
        Page<SpuInfo> page = new Page<>(pageNum,limit);
        page = managerService.getSpuInfoPage(page,category3Id);
        return Result.ok(page);
    }
}
