package org.jeecg.generator.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.generator.entity.UserInfos;
import org.jeecg.generator.mapper.UserInfosMapper;
import org.jeecg.generator.service.UserInfosService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author system
 * @since 2021-08-04
 */
@Service
//public class UserInfosServiceImpl extends ServiceImpl<UserInfosMapper, UserInfos> implements UserInfosService {
public class UserInfosServiceImpl implements UserInfosService {

    @Autowired
    private UserInfosMapper userInfosMapper;

    @Override
    public boolean updateById(UserInfos entity) {
        return userInfosMapper.updateById(entity) == 1;
    }

    @Override
    public boolean save(UserInfos entity) {
        return userInfosMapper.insert(entity) == 1;
    }

    @Override
    public boolean removeByIds(List<Long> ids) {
        return userInfosMapper.deleteBatchIds(ids) >= 1;
    }

    @Override
    public UserInfos getById(Long id) {
        return userInfosMapper.selectById(id);
    }

    @Override
    public IPage<UserInfos> page(Page<UserInfos> page) {
        return userInfosMapper.selectPage(page, null);
    }

}
