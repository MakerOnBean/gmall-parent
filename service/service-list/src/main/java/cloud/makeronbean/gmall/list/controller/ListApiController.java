package cloud.makeronbean.gmall.list.controller;

import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.list.service.SearchService;
import cloud.makeronbean.gmall.model.list.Goods;
import cloud.makeronbean.gmall.model.list.SearchParam;
import cloud.makeronbean.gmall.model.list.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * @author makeronbean
 */
@RestController
@RequestMapping("/api/list")
public class ListApiController {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private SearchService searchService;

    /**
     * 商品搜索
     */
    @PostMapping
    public Result list(@RequestBody SearchParam searchParam) {
        SearchResponseVo searchResponseVo = searchService.search(searchParam);
        return Result.ok(searchResponseVo);
    }


    /**
     * 更改商品热度排名
     */
    @GetMapping("/inner/incrHotScore/{skuId}")
    public Result incrHotScore(@PathVariable Long skuId){
        searchService.incrHotScore(skuId);
        return Result.ok();
    }


    /**
     * 商品下架
     */
    @GetMapping("/inner/lowerGoods/{skuId}")
    public Result lowerGoods(@PathVariable Long skuId){
        searchService.lowerGoods(skuId);
        return Result.ok();
    }


    /**
     * 商品上架
     */
    @GetMapping("/inner/upperGoods/{skuId}")
    public Result upperGoods(@PathVariable Long skuId){
        searchService.upperGoods(skuId);
        return Result.ok();
    }


    /**
     * 创建一个索引库
     */
    @GetMapping("/createIndex")
    public Result createIndex(){
        restTemplate.createIndex(Goods.class);
        restTemplate.putMapping(Goods.class);
        return Result.ok();
    }


}
