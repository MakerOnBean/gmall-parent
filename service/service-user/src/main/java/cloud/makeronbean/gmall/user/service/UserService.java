package cloud.makeronbean.gmall.user.service;

import cloud.makeronbean.gmall.model.user.LoginVo;
import cloud.makeronbean.gmall.model.user.UserAddress;
import cloud.makeronbean.gmall.model.user.UserInfo;

import java.util.List;

/**
 * @author makeronbean
 */
public interface UserService {

    /**
     * 登陆
     */
    UserInfo login(LoginVo loginVo);

    /**
     * 查询用户地址列表
     */
    List<UserAddress> findUserAddressByUserId(Long userId);
}
