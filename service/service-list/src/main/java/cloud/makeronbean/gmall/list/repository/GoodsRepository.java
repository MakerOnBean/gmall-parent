package cloud.makeronbean.gmall.list.repository;

import cloud.makeronbean.gmall.model.list.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * @author makeronbean
 */
@Repository
public interface GoodsRepository extends ElasticsearchRepository<Goods,Long> {
}
