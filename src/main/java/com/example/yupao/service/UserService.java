package com.example.yupao.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.yupao.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
* @author 张博洋
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2025-11-23 18:16:53
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return 新用户id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount
     * @param userPassword
     * @param request
     * @return 脱敏用户
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户脱敏
     * @param originalUser
     * @return
     */
    User getSafetyUser(User originalUser);

    /**
     * 退出登录
     *
     * @param request
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param request
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param loginUser
     */
    boolean isAdmin(User loginUser);

    /**
     * 获取当前登录用户
     *
     * @param request
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 更新用户
     *
     * @param user 被修改用户
     * @param loginUser 当前登录用户
     */
    int updateUser(User user, User loginUser);

    /**
     * 根据标签搜索用户
     *
     * @param tagNameList 标签列表
     * @return 用户列表
     */
    List<User> searchUsersByTags(List<String> tagNameList);

    /**
     * 推荐用户
     *
     * @param loginUser 当前登录用户
     * @param pageNum  页数
     * @param pageSize 每页大小
     * @return 分页结果
     */
    Page<User> recommendUsers(long pageNum, long pageSize, User loginUser);

    /**
     * 匹配用户
     *
     * @param loginUser 当前登录用户
     * @param num 匹配用户数
     * @return 用户列表
     */
    List<User> matchUsers(long num, User loginUser);
}
