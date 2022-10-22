package cloud.makeronbean.gmall.item.controller;

import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.item.service.ItemApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author makeronbean
 */
@RestController
@RequestMapping("/api/item")
public class ItemApiController {

    @Autowired
    private ItemApiService itemApiService;


    /**
     * 获取商品详细数据
     */
    @GetMapping("/{skuId}")
    public Result<Map<String,Object>> getItem(@PathVariable Long skuId){
        Map<String,Object> map = itemApiService.getItem(skuId);
        return Result.ok(map);
    }

}
