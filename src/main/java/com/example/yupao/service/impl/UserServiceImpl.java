package com.example.yupao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yupao.common.ErrorCode;
import com.example.yupao.constant.UserConstant;
import com.example.yupao.exception.BusinessException;
import com.example.yupao.model.domain.User;
import com.example.yupao.service.UserService;

import java.util.*;

import com.example.yupao.mapper.UserMapper;
import com.example.yupao.utils.AlgorithmUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.example.yupao.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author 张博洋
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2025-11-23 18:16:53
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Resource
    private UserMapper userMapper;


    private static final String SALT = "yupi";

    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        //  1. 账号、密码、校验密码 非空
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        //  2. 账户不少于4位
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度不足4位");
        }
        //  3. 密码不少于8位
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不足8位");
        }
        //  4. 账号不含特殊字符
        String illegalChars = "[-`~!@#$%^&*()+=|{}':;',\\[\\].<>/?！@#￥%……&*（）——+【】‘；：”“’。，、？\\s\\\\]";
        Pattern pattern = Pattern.compile(illegalChars);
        Matcher matcher = pattern.matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能包含特殊字符");
        }
        //  5. 密码和校验密码相同
        if (!Objects.equals(userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "校验密码与密码不同");
        }
        //  6. 账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }
        // 对密码加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 存入User并返回id
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
        }
        return user.getId();
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验用户账户和密码是否合法
        // 账户长度不小于4位
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度不足4位");
        }
        // 密码长度不小于8位
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不足8位");
        }
        // 账户不包含特殊字符
        String illegalChars = "[-`~!@#$%^&*()+=|{}':;',\\[\\].<>/?！@#￥%……&*（）——+【】‘；：”“’。，、？\\s\\\\]";
        Pattern pattern = Pattern.compile(illegalChars);
        Matcher matcher = pattern.matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能包含特殊字符");
        }
        // 2. 校验密码是否输入正确，要和数据库中的密文密码对比
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户或密码错误");
        }
        // 3. 返回用户信息（脱敏）隐藏敏感信息，防止数据库中的字段泄露
        User safetyUser = getSafetyUser(user);
        // 4. 要记录用户的登录状态（session）将其存到服务器上（用后端SpringBoot框架封装的服务器tomcat去记录）cookie
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
        // 5. 返回脱敏后的用户信息
        return safetyUser;
    }

    @Override
    public User getSafetyUser(User originalUser) {
        User safetyUser = new User();
        safetyUser.setId(originalUser.getId());
        safetyUser.setUsername(originalUser.getUsername());
        safetyUser.setUserAccount(originalUser.getUserAccount());
        safetyUser.setAvatarUrl(originalUser.getAvatarUrl());
        safetyUser.setProfile(originalUser.getProfile());
        safetyUser.setGender(originalUser.getGender());
        safetyUser.setPhone(originalUser.getPhone());
        safetyUser.setEmail(originalUser.getEmail());
        safetyUser.setUserStatus(originalUser.getUserStatus());
        safetyUser.setUserRole(originalUser.getUserRole());
        safetyUser.setCreateTime(originalUser.getCreateTime());
        safetyUser.setTags(originalUser.getTags());
        return safetyUser;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(USER_LOGIN_STATE) != null) {
            session.invalidate();
            return true;
        }
        return false;
    }

    @Override
    public boolean isAdmin(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (!(userObj instanceof User user)) {
            return false;
        }
        return user.getUserRole() == UserConstant.ADMIN_ROLE;
    }

    @Override
    public boolean isAdmin(User loginUser) {
        return loginUser != null && loginUser.getUserRole() == UserConstant.ADMIN_ROLE;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        return (User) userObj;
    }

    @Override
    public int updateUser(User user, User loginUser) {
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = user.getId();
        if (id < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (!isAdmin(loginUser) && !Objects.equals(user.getId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        User oldUser = userMapper.selectById(user.getId());
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "被修改用户不存在或已删除");
        }
        return userMapper.updateById(user);
    }

    @Override
    public List<User> searchUsersByTags(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String targetTagsJson = new Gson().toJson(tagNameList);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.apply("JSON_CONTAINS(tags,{0})", targetTagsJson);
        List<User> userList = this.list(queryWrapper);
        return userList.stream().map(this::getSafetyUser).toList();
    }

    @Override
    public Page<User> recommendUsers(long pageNum, long pageSize, User loginUser) {
        String redisKey = String.format("yupao:user:recommend:%s", loginUser.getId());
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        // 有缓存，查询缓存
        Object cached = valueOperations.get(redisKey);
        if (cached instanceof Page) {
            @SuppressWarnings("unchecked")
            Page<User> userPage = (Page<User>) cached;
            return userPage;
        }
        // 无缓存，查询数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .eq("isDelete", 0)
                .eq("userStatus", 0)
                .ne("id", loginUser.getId())
                .orderByDesc("createTime");
        Page<User> userPage = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        // 写缓存
        try {
            valueOperations.set(redisKey, userPage, 30000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }
        return userPage;
    }

    @Override
    public List<User> matchUsers(long num, User loginUser) {
        // 1.拿到当前登录用户的标签字符串
        String tags = loginUser.getTags();
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {}.getType());
        // 2.从数据库查出所有“有标签”的用户（只查 id 和 tags）
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "tags");
        queryWrapper.isNotNull("tags");
        List<User> userList = this.list(queryWrapper);
        // 3.遍历所有用户，计算和当前用户的“标签距离”
        List<Pair<User, Long>> list = new ArrayList<>();
        for (User user : userList) {
            String userTags = user.getTags();
            if (StringUtils.isBlank(userTags) || Objects.equals(loginUser.getId(), user.getId())) {
                continue;
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {}.getType());
            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
            list.add(Pair.of(user, distance));
        }
        // 4.按升序排序+取前几名
        PriorityQueue<Pair<User, Long>> priorityQueue = new PriorityQueue<>(
                (int) num,
                Comparator.<Pair<User, Long>, Long>comparing(Pair::getRight).reversed()
        );
        for (Pair<User, Long> pair : list) {
            if (priorityQueue.size() < num) {
                priorityQueue.offer(pair);
            } else if (pair.getRight() < priorityQueue.peek().getRight()) {
                priorityQueue.poll();
                priorityQueue.offer(pair);
            }
        }
        List<Pair<User, Long>> topSimilarUser = priorityQueue.stream()
                .sorted(Comparator.comparing(Pair::getRight))
                .toList();
        List<Long> topSimilarUserId = topSimilarUser.stream()
                .map(Pair::getLeft)
                .map(User::getId)
                .toList();
        // 5.根据 id 批量查这些用户的完整信息
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", topSimilarUserId);
        List<User> fullUsers = this.list(userQueryWrapper);
        Map<Long, User> userIdUserMap = fullUsers.stream()
                .map(this::getSafetyUser)
                .collect(Collectors.toMap(User::getId, user -> user));
        // 6.按原来的顺序（相似度顺序）组装最终结果
        List<User> finallUserList = new ArrayList<>();
        for (Long userId : topSimilarUserId) {
            finallUserList.add(
                    userIdUserMap.get(userId)
            );
        }
        return finallUserList;
    }
}




