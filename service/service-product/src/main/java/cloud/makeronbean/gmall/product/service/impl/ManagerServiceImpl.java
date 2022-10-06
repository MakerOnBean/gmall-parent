package cloud.makeronbean.gmall.product.service.impl;

import cloud.makeronbean.gmall.model.product.*;
import cloud.makeronbean.gmall.product.mapper.*;
import cloud.makeronbean.gmall.product.service.ManagerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

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
        return  list;
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
        if (!CollectionUtils.isEmpty(spuPosterList)){
            spuPosterList.forEach(item -> {
                item.setSpuId(spuInfo.getId());
                spuPosterMapper.insert(item);
            });
        }

        // 向销售属性表和销售属性值表中添加记录
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (!CollectionUtils.isEmpty(spuSaleAttrList)){
            spuSaleAttrList.forEach(spuSaleAttr -> {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (!CollectionUtils.isEmpty(spuSaleAttrValueList)){
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
        wrapper.eq(SpuImage::getSpuId,spuId);
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
        if (!CollectionUtils.isEmpty(skuImageList)){
            skuImageList.forEach(item -> {
                item.setSkuId(skuInfo.getId());
                skuImageMapper.insert(item);
            });
        }

        // 保存带skuId的skuAttrValueList集合
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)){
            skuAttrValueList.forEach(item -> {
                item.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(item);
            });
        }

        // 保存带spuId和skuId的skuSaleAttrValueList集合
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)){
            skuSaleAttrValueList.forEach(item -> {
                item.setSkuId(skuInfo.getId());
                item.setSpuId(skuInfo.getSpuId());
                skuSaleAttrValueMapper.insert(item);
            });
        }
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
    }


    /**
     * 根据属性id查询属性值集合
     */
    private List<BaseAttrValue> getAttrValue(Long attrId) {
        LambdaQueryWrapper<BaseAttrValue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseAttrValue::getAttrId, attrId);
        return baseAttrValueMapper.selectList(wrapper);
    }
}
