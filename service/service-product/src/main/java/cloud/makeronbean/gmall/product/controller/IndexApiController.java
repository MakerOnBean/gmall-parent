package cloud.makeronbean.gmall.product.controller;

import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.product.service.ManagerService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 移动端调用
 * @author makeronbean
 */
@RestController
@RequestMapping("/api/product")
public class IndexApiController {

    @Autowired
    private ManagerService managerService;

    /**
     * 查询所有三级分类id
     * 首页显示使用
     */
    @GetMapping("/getBaseCategoryList")
    public Result<List<JSONObject>> getBaseCategoryList(){
        List<JSONObject> resultList = managerService.getBaseCategoryList();
        return Result.ok(resultList);
    }
}
