package cloud.makeronbean.gmall.product.mapper;

import cloud.makeronbean.gmall.model.product.SkuSaleAttrValue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface SkuSaleAttrValueMapper extends BaseMapper<SkuSaleAttrValue> {
    /**
     * 根据spuId获取销售属性值id与skuId组成的数据集
     */

    List<Map<Object,Object>> getSkuValueIdsMap(Long spuId);
}
