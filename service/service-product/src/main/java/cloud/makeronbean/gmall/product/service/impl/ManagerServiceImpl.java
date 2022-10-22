package cloud.makeronbean.gmall.product.service.impl;

import cloud.makeronbean.gmall.common.cache.GmallCache;
import cloud.makeronbean.gmall.common.constant.RedisConst;
import cloud.makeronbean.gmall.constant.MqConst;
import cloud.makeronbean.gmall.model.product.*;
import cloud.makeronbean.gmall.product.mapper.*;
import cloud.makeronbean.gmall.product.service.ManagerService;
import cloud.makeronbean.gmall.service.RabbitService;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author makeronbean
 */
@Service
public class ManagerServiceImpl implements ManagerService {

    /**
     * 一级分类表
     */
    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    /**
     * 二级分类表
     */
    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    /**
     * 三级分类表
     */
    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    /**
     * 分类属性表
     */
    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    /**
     * 分类属性值表
     */
    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    /**
     * spuInfo表
     */
    @Autowired
    private SpuInfoMapper spuInfoMapper;

    /**
     * 销售属性表
     */
    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    /**
     * spu图片表
     */
    @Autowired
    private SpuImageMapper spuImageMapper;

    /**
     * spu海报表
     */
    @Autowired
    private SpuPosterMapper spuPosterMapper;

    /**
     * 销售属性表
     */
    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    /**
     * 销售属性值表
     */
    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    /**
     * skuInfo表
     */
    @Autowired
    private SkuInfoMapper skuInfoMapper;

    /**
     * skuImage表
     */
    @Autowired
    private SkuImageMapper skuImageMapper;

    /**
     * skuAttrValue sku销售属性表
     */
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    /**
     * skuSaleAttrValue sku平台属性表
     */
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    /**
     * base_category_view视图
     */
    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    /**
     * redis操作对象
     */
    @Autowired
    private RedisTemplate<Object,Object> redisTemplate;

    /**
     * redisson分布式锁
     */
    @Autowired
    private RedissonClient redissonClient;

    /**
     * 品牌表操作对象
     */
    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    /**
     * 发送消息对象
     */
    @Autowired
    private RabbitService rabbitService;


    /**
     * 获取所有一级分类
     */
    @Override
    public List<BaseCategory1> getCategory1() {
        return baseCategory1Mapper.selectList(null);
    }


    /**
     * 获取指定一级分类下所有的二级分类
     */
    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        LambdaQueryWrapper<BaseCategory2> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BaseCategory2::getCategory1Id, category1Id);
        return baseCategory2Mapper.selectList(queryWrapper);
    }


    /**
     * 获取指定二级分类下所有的三级分类
     */
    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        LambdaQueryWrapper<BaseCategory3> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BaseCategory3::getCategory2Id, category2Id);
        return baseCategory3Mapper.selectList(queryWrapper);
    }


    /**
     * 根据分类查询平台属性
     * 查询一级、二级、三级分类下的数据，关系为or
     */
    @Override
    public List<BaseAttrInfo> attrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        List<BaseAttrInfo> list = baseAttrInfoMapper.selectAttrInfoList(category1Id, category2Id, category3Id);
        return list;
    }


    /**
     * 保存或修改平台属性
     * <p>
     * Transactional如果不设置rollbackFor属性，那么默认回滚RuntimeException，需要通过该属性指定遇到哪些异常进行回滚
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (StringUtils.isEmpty(baseAttrInfo.getId())) {
            // 新增平台属性
            baseAttrInfoMapper.insert(baseAttrInfo);
        } else {
            // 修改平台属性
            baseAttrInfoMapper.updateById(baseAttrInfo);
            // 删除原来的数据
            LambdaQueryWrapper<BaseAttrValue> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BaseAttrValue::getAttrId, baseAttrInfo.getId());
            // 使用api是逻辑删除
            baseAttrValueMapper.delete(wrapper);
        }
        // 先为平台属性值 attr_id 字段赋值，再添加到平台属性值表
        if (!CollectionUtils.isEmpty(attrValueList)) {
            attrValueList.forEach(item -> {
                item.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insert(item);
            });
        }
    }


    /**
     * 根据平台属性id查询平台属性值集合
     */
    @Override
    public BaseAttrInfo getAttrInfo(Long attrId) {
        // 查询平台属性
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);

        // 将平台属性值查询出来并封装
        List<BaseAttrValue> list = getAttrValue(attrId);
        baseAttrInfo.setAttrValueList(list);

        return baseAttrInfo;
    }


    /**
     * 根据三级分类id进行分页查询spu
     */
    @Override
    public Page<SpuInfo> getSpuInfoPage(Page<SpuInfo> page, Long category3Id) {
        LambdaQueryWrapper<SpuInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpuInfo::getCategory3Id, category3Id);
        return spuInfoMapper.selectPage(page, wrapper);
    }


    /**
     * 获取销售属性集合
     */
    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectList(null);
    }


    /**
     * 保存新增的spu信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSpuInfo(SpuInfo spuInfo) {
        // 向spu_info表中添加记录
        spuInfoMapper.insert(spuInfo);

        // 向spu_image表中添加带spu_id字段值的list
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (!CollectionUtils.isEmpty(spuImageList)) {
            spuImageList.forEach(item -> {
                item.setSpuId(spuInfo.getId());
                spuImageMapper.insert(item);
            });
        }

        //向spu_poster表中添加带spu_id字段值的list
        List<SpuPoster> spuPosterList = spuInfo.getSpuPosterList();
        if (!CollectionUtils.isEmpty(spuPosterList)) {
            spuPosterList.forEach(item -> {
                item.setSpuId(spuInfo.getId());
                spuPosterMapper.insert(item);
            });
        }

        // 向销售属性表和销售属性值表中添加记录
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (!CollectionUtils.isEmpty(spuSaleAttrList)) {
            spuSaleAttrList.forEach(spuSaleAttr -> {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (!CollectionUtils.isEmpty(spuSaleAttrValueList)) {
                    spuSaleAttrValueList.forEach(spuSaleAttrValue -> {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    });
                }
            });
        }
    }


    /**
     * 根据spuid查询销售属性
     */
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(Long spuId) {
        return spuSaleAttrMapper.getSpuSaleAttrList(spuId);
    }


    /**
     * 根据spuId查询SpuImage集合
     */
    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
        LambdaQueryWrapper<SpuImage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpuImage::getSpuId, spuId);
        return spuImageMapper.selectList(wrapper);
    }


    /**
     * 保存新增的SpuInfo信息
     */
    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        // 保存skuInfo信息
        skuInfoMapper.insert(skuInfo);

        // 保存带skuId和isDefault数据的 skuImage 集合信息
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!CollectionUtils.isEmpty(skuImageList)) {
            skuImageList.forEach(item -> {
                item.setSkuId(skuInfo.getId());
                skuImageMapper.insert(item);
            });
        }

        // 保存带skuId的skuAttrValueList集合
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)) {
            skuAttrValueList.forEach(item -> {
                item.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(item);
            });
        }

        // 保存带spuId和skuId的skuSaleAttrValueList集合
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)) {
            skuSaleAttrValueList.forEach(item -> {
                item.setSkuId(skuInfo.getId());
                item.setSpuId(skuInfo.getSpuId());
                skuSaleAttrValueMapper.insert(item);
            });
        }

        // 向布隆过滤器中保存添加的字段key映射
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
        bloomFilter.add(skuInfo.getId());
    }


    /**
     * 分页查询sku
     */
    @Override
    public Page<SkuInfo> getSkuInfoPage(Page<SkuInfo> page) {
        LambdaQueryWrapper<SkuInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SkuInfo::getId);
        return skuInfoMapper.selectPage(page, wrapper);
    }


    /**
     * 商品上架
     */
    @Override
    public void onSale(Long skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setIsSale(1);
        skuInfo.setId(skuId);
        skuInfoMapper.updateById(skuInfo);
        // 发送消息
        rabbitService.send(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_UPPER,skuId);
    }


    /**
     * 商品下架
     */
    @Override
    public void cancelSale(Long skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setIsSale(0);
        skuInfo.setId(skuId);
        skuInfoMapper.updateById(skuInfo);
        // 发送消息
        rabbitService.send(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_LOWER,skuId);
    }


    /**
     * 根据skuId查询带图片列表的SpuInfo信息
     */
    @Override
    @GmallCache(prefix = RedisConst.SKUKEY_PREFIX)
    public SkuInfo getSkuInfo(Long skuId) {
        return getInfoDB(skuId);
    }


    /**
     * 查询数据库获取skuInfo信息
     */
    private SkuInfo getInfoDB(Long skuId) {
        // 查询skuInfo
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);

        if (skuInfo != null) {
            // 封装skuImage集合
            LambdaQueryWrapper<SkuImage> skuImageWrapper = new LambdaQueryWrapper<>();
            skuImageWrapper.eq(SkuImage::getSkuId, skuId);
            List<SkuImage> skuImageList = skuImageMapper.selectList(skuImageWrapper);
            skuInfo.setSkuImageList(skuImageList);
        }

        return skuInfo;
    }


    /**
     * 根据三级分类id获取分类信息
     */
    @Override
    @GmallCache(prefix = "categoryView:")
    public BaseCategoryView getCategoryView(Long category3Id) {
        return baseCategoryViewMapper.selectById(category3Id);
    }


    /**
     * 根据skuId查询商品价格
     * 不添加缓存，使用分布式锁实现流控
     */
    @Override
    public BigDecimal getSkuPrice(Long skuId) {
        try {
            RLock lock = redissonClient.getLock("price:" + skuId + RedisConst.SKULOCK_SUFFIX);
            boolean flag = lock.tryLock(RedisConst.SKUKEY_TIMEOUT, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
            if (flag) {
                try {
                    SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
                    if (skuInfo != null) {
                        return skuInfo.getPrice();
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                Thread.sleep(100);
                return getSkuPrice(skuId);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new BigDecimal("0");
    }


    /**
     * 根据spuId，skuId 查询销售属性集合
     */
    @Override
    @GmallCache(prefix = "spuSaleAttrListCheck:")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        return spuSaleAttrMapper.getSpuSaleAttrListCheckBySku(skuId,spuId);
    }


    /**
     * 根据spuId获取销售属性值id与skuId组成的数据集
     */
    @Override
    @GmallCache(prefix = "skuValueIdsMap:")
    public Map<Object, Object> getSkuValueIdsMap(Long spuId) {
        // 查询对应关系的集合
        List<Map<Object, Object>> resultList = skuSaleAttrValueMapper.getSkuValueIdsMap(spuId);
        Map<Object,Object> map = new HashMap<>();
        // 整合返回的数据
        if (!CollectionUtils.isEmpty(resultList)) {
            resultList.forEach(resultMap -> map.put(resultMap.get("value_ids"),resultMap.get("sku_id")));
        }
        return map;
    }


    /**
     * 根据spuId获取海报数据
     */
    @Override
    @GmallCache(prefix = "spuPoster:")
    public List<SpuPoster> findSpuPosterBySpuId(Long spuId) {
        LambdaQueryWrapper<SpuPoster> spuPosterWrapper = new LambdaQueryWrapper<>();
        spuPosterWrapper.eq(SpuPoster::getSpuId,spuId);
        return spuPosterMapper.selectList(spuPosterWrapper);
    }


    /**
     * 根据skuId查询平台属性
     */
    @Override
    @GmallCache(prefix = "attrList:")
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        return baseAttrInfoMapper.getAttrList(skuId);
    }

    /**
     * 重制布隆过滤器
     */
    @Override
    public void remakeBloomFilter() {
        redisTemplate.delete(RedisConst.SKU_BLOOM_FILTER);
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
        bloomFilter.tryInit(1000L,0.01);
        List<Long> idList = skuInfoMapper.selectIdList();
        idList.forEach(bloomFilter::add);
    }


    /**
     * 查询所有三级分类id
     * 首页显示使用
     */
    @Override
    @GmallCache(prefix = "baseCategoryList")
    public List<JSONObject> getBaseCategoryList() {
        List<JSONObject> resultList = new ArrayList<>();
        List<BaseCategoryView> list = baseCategoryViewMapper.selectList(null);
        // 分组    key 一级分类id  value 一级分类对应的所有数据
        Map<Long, List<BaseCategoryView>> groupBy1Map = list.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        // 自增
        int index = 1;
        // 分组处理
        for (Map.Entry<Long, List<BaseCategoryView>> entry : groupBy1Map.entrySet()) {
            // 获取 category1Id
            Long category1Id = entry.getKey();
            // 获取 category1Name
            List<BaseCategoryView> category2List = entry.getValue();
            String category1Name = category2List.get(0).getCategory1Name();
            // 分组    key 二级分类id  value 二级分类对应的所有数据
            Map<Long, List<BaseCategoryView>> groupBy2Map = category2List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));

            List<JSONObject> categoryChild2 = new ArrayList<>();
            // 分组处理
            for (Map.Entry<Long, List<BaseCategoryView>> entry2 : groupBy2Map.entrySet()) {
                // 获取 category2Id
                Long category2Id = entry2.getKey();
                // 获取 category3Id
                List<BaseCategoryView> category3List = entry2.getValue();
                String category2Name = category3List.get(0).getCategory2Name();

                List<JSONObject> categoryChild3 = new ArrayList<>();

                category3List.forEach(item -> {
                    // 封装第三级数据
                    JSONObject category3Json = new JSONObject();
                    category3Json.put("categoryId",item.getCategory3Id());
                    category3Json.put("categoryName",item.getCategory3Name());
                    categoryChild3.add(category3Json);
                });
                // 封装第二级数据
                JSONObject category2Json = new JSONObject();
                category2Json.put("categoryId",category2Id);
                category2Json.put("categoryName",category2Name);
                category2Json.put("categoryChild",categoryChild3);
                categoryChild2.add(category2Json);
            }


            // category1JSONObject
            JSONObject category1Json = new JSONObject();
            // 封装第一级数据
            category1Json.put("index",index++);
            category1Json.put("categoryId",category1Id);
            category1Json.put("categoryName",category1Name);
            category1Json.put("categoryChild",categoryChild2);
            resultList.add(category1Json);
        }

        return resultList;
    }


    /**
     * 根据品牌id获取品牌属性
     */
    @Override
    public BaseTrademark getTrademark(Long tmId) {
        return baseTrademarkMapper.selectById(tmId);
    }


    /**
     * 根据属性id查询属性值集合
     */
    private List<BaseAttrValue> getAttrValue(Long attrId) {
        LambdaQueryWrapper<BaseAttrValue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseAttrValue::getAttrId, attrId);
        return baseAttrValueMapper.selectList(wrapper);
    }


    /*
        以下方法通过aop实现了
     */
    /**
     * 查询redis缓存中的skuInfo信息，分布式锁使用redis实现
     * 步骤：
     *      1 定义存储skuInfo的key
     *      2 根据skuKey从缓存中查询skuInfo
     *      3 判断
     *          3.1 如果有 直接返回
     *          3.2 如果没有 定义锁key，尝试获取锁
     *              3.2.1 获取锁成功则直接查询数据库
     *                  3.2.1.1 如果查询有值，放入缓存中
     *                  3.2.1.2 如果查询无值，也放入缓存中，时间设置不同
     *              3.2.2 获取锁失败，自旋尝试获取锁
     *          3.3 释放锁
     */
    public SkuInfo getInfoRedis(Long skuId){
        try {
            // 1 定义存储skuInfo的key
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            // 2 根据skuKey从缓存中查询skuInfo
            Object skuInfoObj = redisTemplate.opsForValue().get(skuKey);
            // 3 判断
            if (skuInfoObj != null) {
                // 3.1 如果有 直接返回
                return (SkuInfo) skuInfoObj;
            } else {
                // 3.2 如果没有 定义锁key，尝试获取锁
                String skuLock = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                String value = UUID.randomUUID().toString().replaceAll("-", "");
                Boolean flag = redisTemplate.opsForValue().setIfAbsent(skuLock, value);
                if (flag) {
                    SkuInfo skuInfo;
                    // 3.2.1 获取锁成功则直接查询数据库
                    skuInfo = getInfoDB(skuId);
                    if (skuInfo != null) {
                        // 3.2.1.1 如果查询有值，放入缓存中
                        redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                    } else {
                        // 3.2.1.2 如果查询无值，也放入缓存中，时间设置不同
                        skuInfo = new SkuInfo();
                        redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                    }

                    // 3.3 释放锁
                    String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                    redisScript.setResultType(Long.class);
                    redisScript.setScriptText(script);
                    redisTemplate.execute(redisScript, Arrays.asList(skuLock),value);
                    return skuInfo;
                } else {
                    // 3.2.2 获取锁失败，睡眠并自旋尝试获取锁
                    Thread.sleep(100);
                    return getInfoRedis(skuId);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 兜底方案 如果redis查询出现异常，则前往mysql中查询
        return getInfoDB(skuId);
    }


    /**
     * 查询redis缓存中的skuInfo信息，分布式锁使用redisson实现
     * 步骤：
     *      1 定义存储skuInfo的key
     *      2 根据skuKey从缓存中查询skuInfo
     *      3 判断
     *          3.1 如果有 直接返回
     *          3.2 如果没有 定义锁key，尝试获取锁
     *              3.2.1 获取锁成功则直接查询数据库
     *                  3.2.1.1 如果查询有值，放入缓存中
     *                  3.2.1.2 如果查询无值，也放入缓存中，时间设置不同
     *              3.2.2 获取锁失败，自旋尝试获取锁
     *          3.3 释放锁
     */
    public SkuInfo getInfoRedisson(Long skuId) {
        try {
            // 1 定义存储skuInfo的key
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            // 2 根据skuKey从缓存中查询skuInfo
            Object skuInfoObj = redisTemplate.opsForValue().get(skuKey);
            // 3 判断
            if (skuInfoObj != null) {
                // 3.1 如果有 直接返回
                return (SkuInfo) skuInfoObj;
            } else {
                SkuInfo skuInfo;
                // 3.2 如果没有 定义锁key，尝试获取锁
                String skuLock = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                RLock lock = redissonClient.getLock(skuLock);
                if (lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS)) {
                    try {
                        // 3.2.1 获取锁成功则直接查询数据库
                        skuInfo = getInfoDB(skuId);
                        if (skuInfo != null) {
                            // 3.2.1.1 如果查询有值，放入缓存中
                            redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                        } else {
                            // 3.2.1.2 如果查询无值，也放入缓存中，时间设置不同
                            skuInfo = new SkuInfo();
                            redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                        }
                        return skuInfo;
                    } finally {
                        lock.unlock();
                    }
                } else {
                    // 3.2.2 获取锁失败，睡眠并自旋尝试获取锁
                    Thread.sleep(500);
                    return getInfoRedisson(skuId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getInfoDB(skuId);
    }
}