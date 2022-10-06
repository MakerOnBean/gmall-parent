package cloud.makeronbean.gmall.product.controller;

import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.model.product.*;
import cloud.makeronbean.gmall.product.service.ManagerService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品基础 controller
 * @author makeronbean
 */
@RestController
@RequestMapping("/admin/product")
@Api("商品基础接口")
public class BaseManagerController {

    @Autowired
    private ManagerService managerService;

    /**
     * 根据平台属性id查询平台属性值集合
     */
    @GetMapping("/getAttrValueList/{attrId}")
    public Result<List<BaseAttrValue>> getAttrValueList(@PathVariable Long attrId){
        BaseAttrInfo baseAttrInfo = managerService.getAttrInfo(attrId);
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        return Result.ok(attrValueList);
    }

    /**
     * 保存或修改平台属性
     */
    @PostMapping("/saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        managerService.saveAttrInfo(baseAttrInfo);
        return Result.ok();
    }


    /**
     * 根据分类查询平台属性
     * 查询一级、二级、三级分类下的数据，关系为or
     */
    @GetMapping("/attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result<List<BaseAttrInfo>> attrInfoList(@PathVariable(value = "category1Id")Long category1Id,
                                                   @PathVariable(value = "category2Id")Long category2Id,
                                                   @PathVariable(value = "category3Id")Long category3Id
    ){
        List<BaseAttrInfo> list = managerService.attrInfoList(category1Id,category2Id,category3Id);
        return Result.ok(list);
    }


    /**
     * 获取所有一级分类
     */
    @GetMapping("/getCategory1")
    public Result<List<BaseCategory1>> getCategory1(){
        List<BaseCategory1> list = managerService.getCategory1();
        return Result.ok(list);
    }


    /**
     * 获取指定一级分类下所有的二级分类
     */
    @GetMapping("/getCategory2/{category1Id}")
    public Result<List<BaseCategory2>> getCategory2(@PathVariable("category1Id") Long category1Id){
        List<BaseCategory2> list = managerService.getCategory2(category1Id);
        return Result.ok(list);
    }

    /**
     * 获取指定二级分类下所有的三级分类
     */
    @GetMapping("/getCategory3/{category2Id}")
    public Result<List<BaseCategory3>> getCategory3(@PathVariable("category2Id") Long category2Id){
        List<BaseCategory3> list = managerService.getCategory3(category2Id);
        return Result.ok(list);
    }


}
