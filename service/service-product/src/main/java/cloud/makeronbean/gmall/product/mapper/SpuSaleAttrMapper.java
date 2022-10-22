package cloud.makeronbean.gmall.product.mapper;

import cloud.makeronbean.gmall.model.product.SpuSaleAttr;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author makeronbean
 */
@Mapper
public interface SpuSaleAttrMapper extends BaseMapper<SpuSaleAttr> {
    List<SpuSaleAttr> getSpuSaleAttrList(Long spuId);

    /**
     * 根据spuId，skuId 查询销售属性集合
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@Param("skuId") Long skuId, @Param("spuId") Long spuId);
}
