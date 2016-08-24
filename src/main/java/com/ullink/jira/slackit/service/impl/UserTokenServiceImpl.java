package com.ullink.jira.slackit.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.ullink.jira.slackit.model.UserToken;
import com.ullink.jira.slackit.service.UserTokenService;

public class UserTokenServiceImpl implements UserTokenService {

    private final ActiveObjects ao;

    public UserTokenServiceImpl(ActiveObjects ao) {
        this.ao = checkNotNull(ao);
    }

    @Override
    public UserToken add(String userKey, String token) {
        UserToken userToken = get(userKey);
        if (userToken == null) {
            userToken = ao.create(UserToken.class);
        }
        userToken.setToken(token);
        userToken.setUserKey(userKey);
        userToken.save();
        return userToken;
    }

    @Override
    public UserToken get(String userKey) {
        UserToken[] userTokens = ao.find(UserToken.class, Query.select().where("USER_KEY = ?", userKey));
        return userTokens != null && userTokens.length > 0 ? userTokens[0] : null;
    }

    @Override
    public String getUserToken(String userKey) {
        UserToken userToken = get(userKey);
        return userToken != null ? userToken.getToken() : null;
    }

    @Override
    public void delete(String userKey) {
        UserToken userToken = get(userKey);
        if (userToken != null) {
            ao.delete(userToken);
        }
    }
}
