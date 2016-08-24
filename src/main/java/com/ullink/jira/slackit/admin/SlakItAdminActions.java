package com.ullink.jira.slackit.admin;

import org.apache.log4j.Logger;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.ullink.jira.slackit.managers.SlackItConfigurationHolder;

public class SlakItAdminActions extends JiraWebActionSupport {

    private static final long serialVersionUID = -5950096754381886768L;
    private static final Logger log = Logger.getLogger(SlackItConfigurationHolder.class);
    private SlackItConfigurationHolder slackItConfigurationHolder;
    
    public SlakItAdminActions(SlackItConfigurationHolder slackItConfigurationHolder){
        this.slackItConfigurationHolder = slackItConfigurationHolder;
    }
    
    public SlackItConfigurationHolder getSlackItConfigurationHolder() {
        return slackItConfigurationHolder;
    }

    public String doDefault() throws Exception {
        log.debug("doDefault");
        return INPUT;
    }

    public String doConfigFileReload() throws Exception {
        log.debug("Reloading configuration requested by user");
        slackItConfigurationHolder.forceReload();
        return returnMsgToUser("SlackitSetup!default.jspa", "Reload successful", MessageType.SUCCESS , false, null);
    }

}
