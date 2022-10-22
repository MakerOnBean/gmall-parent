package cloud.makeronbean.gmall.client.impl;

import cloud.makeronbean.gmall.client.UserFeignClient;
import cloud.makeronbean.gmall.model.user.UserAddress;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author makeronbean
 */
@Component
public class UserDegradeFeignClientImpl implements UserFeignClient {
    @Override
    public List<UserAddress> findUserAddressByUserId(Long userId) {
        return null;
    }
}
