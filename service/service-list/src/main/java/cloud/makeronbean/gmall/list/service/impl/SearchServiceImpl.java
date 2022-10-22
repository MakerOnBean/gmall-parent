package cloud.makeronbean.gmall.list.service.impl;

import cloud.makeronbean.gmall.client.ProductFeignClient;
import cloud.makeronbean.gmall.common.constant.RedisConst;
import cloud.makeronbean.gmall.common.execption.GmallException;
import cloud.makeronbean.gmall.common.result.ResultCodeEnum;
import cloud.makeronbean.gmall.list.repository.GoodsRepository;
import cloud.makeronbean.gmall.list.service.SearchService;
import cloud.makeronbean.gmall.model.list.*;
import cloud.makeronbean.gmall.model.product.BaseAttrInfo;
import cloud.makeronbean.gmall.model.product.BaseCategoryView;
import cloud.makeronbean.gmall.model.product.BaseTrademark;
import cloud.makeronbean.gmall.model.product.SkuInfo;
import com.alibaba.fastjson.JSONObject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.script.AggregationScript;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author makeronbean
 */
@Service
@Slf4j
public class SearchServiceImpl implements SearchService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * es 高级客户端
     */
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /**
     * 商品上架
     */
    @Override
    public void upperGoods(Long skuId) {
        // 数据查询
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);

        // 封装数据
        Goods goods = new Goods();
        if (skuInfo != null) {
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            BeanUtils.copyProperties(categoryView, goods);

            goods.setId(skuId);
            goods.setDefaultImg(skuInfo.getSkuDefaultImg());
            goods.setTitle(skuInfo.getSkuName());
            goods.setPrice(skuInfo.getPrice().doubleValue());
            goods.setCreateTime(new Date());
            BaseTrademark trademark = productFeignClient.getTrademark(skuInfo.getTmId());
            goods.setTmId(skuInfo.getTmId());
            goods.setTmName(trademark.getTmName());
            goods.setTmLogoUrl(trademark.getLogoUrl());


            List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
/*            attrList.forEach(item -> {
                SearchAttr searchAttr = new SearchAttr();
                searchAttr.setAttrId(item.getId());
                searchAttr.setAttrName(item.getAttrName());
                searchAttr.setAttrValue(item.getAttrValueList().get(0).getValueName());
                searchAttrList.add(searchAttr);
            });*/
            List<SearchAttr> searchAttrList = attrList.stream().map(baseAttrInfo -> {
                SearchAttr searchAttr = new SearchAttr();
                searchAttr.setAttrId(baseAttrInfo.getId());
                searchAttr.setAttrName(baseAttrInfo.getAttrName());
                searchAttr.setAttrValue(baseAttrInfo.getAttrValueList().get(0).getValueName());
                return searchAttr;
            }).collect(Collectors.toList());
            goods.setAttrs(searchAttrList);

            // 向es中保存数据
            goodsRepository.save(goods);
        } else {
            throw new GmallException(ResultCodeEnum.FAIL);
        }
    }


    /**
     * 商品下架
     */
    @Override
    public void lowerGoods(Long skuId) {
        goodsRepository.deleteById(skuId);
    }


    /**
     * 更改商品热度排名
     * 保存到redis中的类型：sortedSet
     * key: hostScore
     * value: 'skuId':skuId
     * score: 具体热点数
     */
    @Override
    public void incrHotScore(Long skuId) {
        Double hotScore = redisTemplate.opsForZSet().incrementScore(RedisConst.HOT_KEY, RedisConst.HOT_VALUE + skuId, 1);

        if (hotScore != null && hotScore % 10 == 0) {
            Optional<Goods> goodsOptional = goodsRepository.findById(skuId);
            if (goodsOptional.isPresent()) {
                Goods goods = goodsOptional.get();
                goods.setHotScore(Math.round(hotScore));
                goodsRepository.save(goods);
            }
        }
    }


    /**
     * 商品搜索
     */
    @Override
    @SneakyThrows
    public SearchResponseVo search(SearchParam searchParam) {
        // 封装请求条件
        SearchRequest searchRequest = this.bulidSearchRequest(searchParam);
        // 执行索引请求
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        // 封装响应对象
        SearchResponseVo searchResponseVo = this.parseSearchResponseVo(searchResponse);
        // 设置响应对象当前页数
        searchResponseVo.setPageNo(searchParam.getPageNo());
        // 设置响应对象每页数据条数
        searchResponseVo.setPageSize(searchParam.getPageSize());
        // 计算总页数
        long totalPages = (searchResponseVo.getTotal() + searchParam.getPageSize() - 1) / searchParam.getPageSize();
        // 设置响应对象总页数
        searchResponseVo.setTotalPages(totalPages);
        return searchResponseVo;
    }


    /**
     * 封装返回结果集
     */
    private SearchResponseVo parseSearchResponseVo(SearchResponse searchResponse) {
        // 创建返回对象
        SearchResponseVo searchResponseVo = new SearchResponseVo();

        // 封装
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();

        // 封装品牌
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) aggregationMap.get("tmIdAgg");
        List<? extends Terms.Bucket> trademarkBuckets = tmIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(trademarkBuckets)) {
            List<SearchResponseTmVo> trademarkList = trademarkBuckets.stream().map(bucket -> {
                SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();
                Aggregations aggregations = bucket.getAggregations();
                // 封装id
                searchResponseTmVo.setTmId(bucket.getKeyAsNumber().longValue());
                // 封装tmName
                ParsedStringTerms tmNameAgg = aggregations.get("tmNameAgg");
                String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
                searchResponseTmVo.setTmName(tmName);
                // 封装tmLogoUrl
                ParsedStringTerms tmLogoUrlAgg = aggregations.get("tmLogoUrlAgg");
                String tmLogoUrl = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();
                searchResponseTmVo.setTmLogoUrl(tmLogoUrl);
                return searchResponseTmVo;
            }).collect(Collectors.toList());
            searchResponseVo.setTrademarkList(trademarkList);
        }

        // 封装平台属性
        ParsedNested attrsAgg = (ParsedNested) aggregationMap.get("attrsAgg");
        Map<String, Aggregation> attrsAggMap = attrsAgg.getAggregations().asMap();
        // 通过 attrsAgg 获取 attrIdAgg
        ParsedLongTerms attrIdAgg = (ParsedLongTerms) attrsAggMap.get("attrIdAgg");
        // 获取 attrIdAgg 的 Buckets
        List<? extends Terms.Bucket> attrIdBuckets = attrIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(attrIdBuckets)) {
            List<SearchResponseAttrVo> attrsList = attrIdBuckets.stream().map(bucket -> {
                Aggregations aggregations = bucket.getAggregations();
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                // 封装平台属性Id
                searchResponseAttrVo.setAttrId(bucket.getKeyAsNumber().longValue());
                // 封装属性名称
                ParsedStringTerms attrNameAgg = aggregations.get("attrNameAgg");
                searchResponseAttrVo.setAttrName(attrNameAgg.getBuckets().get(0).getKeyAsString());
                // 封装属性值
                ParsedStringTerms attrValueAgg = aggregations.get("attrValueAgg");
                List<String> attrValueList = attrValueAgg.getBuckets()
                        .stream()
                        .map(MultiBucketsAggregation.Bucket::getKeyAsString)
                        .collect(Collectors.toList());
                searchResponseAttrVo.setAttrValueList(attrValueList);

                return searchResponseAttrVo;
            }).collect(Collectors.toList());
            searchResponseVo.setAttrsList(attrsList);
        }

        // 封装查询出来的具体商品信息
        SearchHit[] hits = searchResponse.getHits().getHits();
        if (hits != null && hits.length > 0) {
            List<Goods> goodsList = Arrays
                    .stream(hits)
                    .map(hit -> {
                        Goods goods = JSONObject.parseObject(hit.getSourceAsString(), Goods.class);
                        Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                        if (!CollectionUtils.isEmpty(highlightFields)) {
                            goods.setTitle(highlightFields.get("title").getFragments()[0].toString());
                        }
                        return goods;
                    })
                    .collect(Collectors.toList());
            searchResponseVo.setGoodsList(goodsList);
        }


        // 封装 total 总记录数
        searchResponseVo.setTotal(searchResponse.getHits().getTotalHits().value);

        return searchResponseVo;
    }


    /**
     * 封装es查询条件
     */
    private SearchRequest bulidSearchRequest(SearchParam searchParam) {
        // 创建查询请求对象
        SearchRequest searchRequest = new SearchRequest("goods");
        // 创建查询条件对象
        SearchSourceBuilder builder = new SearchSourceBuilder();
        // 创建对条件对象
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 封装关键字keyword查询条件
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            MatchQueryBuilder title = QueryBuilders.matchQuery("title", searchParam.getKeyword()).operator(Operator.AND);
            boolQueryBuilder.must(title);
        }

        // 封装品牌条件
        String trademark = searchParam.getTrademark();
        if (!StringUtils.isEmpty(trademark)) {
            String[] split = trademark.split(":");
            if (split != null && split.length == 2) {
                TermQueryBuilder tmIdTQB = QueryBuilders.termQuery("tmId", split[0]);
                boolQueryBuilder.filter(tmIdTQB);
            }
        }

        // 封装分类条件
        Long category1Id = searchParam.getCategory1Id();
        if (category1Id != null) {
            TermQueryBuilder category1IdTQB = QueryBuilders.termQuery("category1Id", category1Id);
            boolQueryBuilder.filter(category1IdTQB);
        }
        Long category2Id = searchParam.getCategory2Id();
        if (category2Id != null) {
            TermQueryBuilder category2IdTQB = QueryBuilders.termQuery("category2Id", category2Id);
            boolQueryBuilder.filter(category2IdTQB);
        }
        Long category3Id = searchParam.getCategory3Id();
        if (category3Id != null) {
            TermQueryBuilder category3IdTQB = QueryBuilders.termQuery("category3Id", category3Id);
            boolQueryBuilder.filter(category3IdTQB);
        }

        // 封装平台属性
        // props=23:4G:运行内存
        // 平台属性Id 平台属性值名称 平台属性名
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0) {
            Arrays.stream(props).forEach(itemProp -> {
                String[] split = StringUtils.split(itemProp, ":");
                if (split != null && split.length == 3) {
                    // 创建多条件对象
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    // 创建子条件对象
                    BoolQueryBuilder subBoolQuery = QueryBuilders.boolQuery();
                    subBoolQuery.filter(QueryBuilders.termQuery("attrs.attrValue", split[1]));
                    subBoolQuery.filter(QueryBuilders.termQuery("attrs.attrId", split[0]));

                    // nested
                    boolQuery.must(QueryBuilders.nestedQuery("attrs", subBoolQuery, ScoreMode.None));
                    // 添加条件到最外层
                    boolQueryBuilder.filter(boolQuery);
                }
            });
        }


        // 添加条件到条件条件对象中
        builder.query(boolQueryBuilder);

        // 分页
        builder.from((searchParam.getPageNo() - 1) * searchParam.getPageSize());
        builder.size(searchParam.getPageSize());
        // 排序
        String order = searchParam.getOrder();
        if (!StringUtils.isEmpty(order)) {
            String[] split = StringUtils.split(order, ":");
            String field = null;
            assert split != null;
            if ("2".equals(split[0])) {
                field = "price";
            } else {
                field = "hotScore";
            }
            builder.sort(field, "asc".equals(split[1]) ? SortOrder.ASC : SortOrder.DESC);
        } else {
            builder.sort("hotScore", SortOrder.DESC);
        }

        // 聚合-品牌
        // 最外层聚合
        TermsAggregationBuilder tmIdAgg = AggregationBuilders.terms("tmIdAgg").field("tmId");
        // 子聚合
        tmIdAgg.subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"));
        tmIdAgg.subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl"));

        builder.aggregation(tmIdAgg);

        //聚合-平台属性
        // 父聚合
        NestedAggregationBuilder attrsAgg = AggregationBuilders.nested("attrsAgg", "attrs");
        // 一级子聚合
        TermsAggregationBuilder attrIdAgg = AggregationBuilders.terms("attrIdAgg").field("attrs.attrId");
        // 二级子聚合
        attrIdAgg.subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"));
        attrIdAgg.subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"));
        // 添加到父聚合
        attrsAgg.subAggregation(attrIdAgg);

        builder.aggregation(attrsAgg);

        // 高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.postTags("</span>");

        builder.highlighter(highlightBuilder);

        // 过滤
        builder.fetchSource(new String[]{"id", "title","price","defaultImg"}, null);

        // 将条件构造对象添加到查询请求对象中
        searchRequest.source(builder);

        log.info("dsl------>"+builder);
        return searchRequest;
    }
}
