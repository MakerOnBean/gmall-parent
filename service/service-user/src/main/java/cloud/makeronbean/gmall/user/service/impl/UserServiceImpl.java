package cloud.makeronbean.gmall.user.service.impl;

import cloud.makeronbean.gmall.model.user.LoginVo;
import cloud.makeronbean.gmall.model.user.UserAddress;
import cloud.makeronbean.gmall.model.user.UserInfo;
import cloud.makeronbean.gmall.user.mapper.UserAddressMapper;
import cloud.makeronbean.gmall.user.mapper.UserInfoMapper;
import cloud.makeronbean.gmall.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author makeronbean
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;


    /**
     * 登陆
     */
    @Override
    public UserInfo login(LoginVo loginVo) {
        String loginName = null;
        String password = null;
        // 判断是移动端登录还是pc端登录
        if (!StringUtils.isEmpty(loginVo.getPhone()) && !StringUtils.isEmpty(loginVo.getPassword())) {
            loginName = loginVo.getPhone();
            password = loginVo.getPassword();
        } else if (!StringUtils.isEmpty(loginVo.getLoginName()) && !StringUtils.isEmpty(loginVo.getPasswd())){
            loginName = loginVo.getLoginName();
            password = loginVo.getPasswd();
        } else {
            return null;
        }

        //登录验证
        String newPasswd = DigestUtils.md5DigestAsHex(password.getBytes());
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getLoginName,loginName);
        wrapper.eq(UserInfo::getPasswd,newPasswd);
        return userInfoMapper.selectOne(wrapper);

    }


    /**
     * 查询用户地址列表
     */
    @Override
    public List<UserAddress> findUserAddressByUserId(Long userId) {
        LambdaQueryWrapper<UserAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAddress::getUserId,userId);
        return userAddressMapper.selectList(wrapper);
    }
}
