package com.ullink.jira.slackit.service;

import com.atlassian.activeobjects.tx.Transactional;
import com.ullink.jira.slackit.model.UserToken;

@Transactional
public interface UserTokenService {

    UserToken add(String userKey, String token);

    UserToken get(String userKey);

    void delete(String userKey);

    String getUserToken(String userKey);

}
