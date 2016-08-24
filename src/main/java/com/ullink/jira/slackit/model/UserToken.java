package com.ullink.jira.slackit.model;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Table;

@Preload
@Table("SlackUsersToken")
public interface UserToken extends Entity {

    String getUserKey();

    void setUserKey(String userKey);

    String getToken();

    void setToken(String token);

}
