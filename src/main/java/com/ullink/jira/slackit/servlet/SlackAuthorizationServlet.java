package com.ullink.jira.slackit.servlet;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.base.Joiner;
import com.ullink.jira.slackit.Utils;
import com.ullink.jira.slackit.fields.SlackChannelCustomField;
import com.ullink.jira.slackit.managers.SlackItConfigurationHolder;
import com.ullink.jira.slackit.service.UserTokenService;

public class SlackAuthorizationServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(SlackAuthorizationServlet.class.getName());

    private static final long serialVersionUID = 1L;

    private final UserTokenService userTokenService;
    private final SlackItConfigurationHolder slackItConfigurationHolder;

    public SlackAuthorizationServlet(UserTokenService userTokenService, SlackItConfigurationHolder slackItConfigurationHolder) {
        this.userTokenService = checkNotNull(userTokenService);
        this.slackItConfigurationHolder = checkNotNull(slackItConfigurationHolder);
    }

    @Override
    protected void doGet(final HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final PrintWriter w = resp.getWriter();
        final ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

        String tokenParam = req.getParameter("token");
        String requestTokenParam = req.getParameter("requestToken");
        String actionParam = req.getParameter("action");

        if ("deleteToken".equals(actionParam)) {
            // delete slack token for current user
            userTokenService.delete(user.getKey());
            LOG.info("Deleted token for userKey=" + user.getKey());
            resp.sendRedirect(req.getHeader("referer"));
        } else if ("setChannelId".equals(actionParam)) {
            String channelId = req.getParameter("channelId");
            String channelName = req.getParameter("channelName");
            String issueKey = req.getParameter("issueKey");
            if (channelId != null && issueKey != null && channelName != null) {
                CustomField slackChannelCustomField = slackItConfigurationHolder.getSlackChannelCustomField();
                MutableIssue issue = ComponentAccessor.getIssueManager().getIssueByKeyIgnoreCase(issueKey);
                if (issue != null) {
                    String customFieldValue = Joiner.on(SlackChannelCustomField.SEPARATOR).join(Arrays.asList(channelId, channelName));
                    Utils.updateCustomField(issue, slackChannelCustomField, customFieldValue);
                    ComponentAccessor.getCommentManager().create(issue, user, 
                            "Slack channel [#" + channelName + "|" + slackItConfigurationHolder.getSlackBaseUrl() + "/messages/" + channelName + "] associated with this issue (id=" + channelId + ")", 
                            slackItConfigurationHolder.getCommentGroupID(), 
                            slackItConfigurationHolder.getCommentRoleID(), 
                            false);
                    LOG.info("Slack channel " + channelName + " with id: " + channelId + " saved for issue : " + issue.getKey());
                } else {
                    LOG.warn("Cannot update Slack channel. Issue : " + issueKey + " not found");
                }
            }
        } else if (tokenParam != null) {
            userTokenService.add(user.getKey(), tokenParam);
            String stateParam = req.getParameter("state");
            resp.sendRedirect(stateParam);
        } else if (requestTokenParam != null) {
            String redirectUri = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL) + SlackItConfigurationHolder.OAUTH_REDIRECT_URL;
            String scope = "channels%3Awrite+channels%3Aread+channels%3Ahistory+users%3Aread";
            resp.sendRedirect("https://slack.com/oauth/authorize?client_id=" + slackItConfigurationHolder.getSlackAppClientId() + "&scope=" + scope + "&state="
                    + requestTokenParam + "&redirect_uri=" + redirectUri);
        } else {
            String token = userTokenService.getUserToken(user.getKey());
            if (token != null) {
                w.print("Token for " + user.getKey() + " : " + token);
            } else {
                w.write("No token for : " + user.getKey() + "</br>");
                w.write("Click <a href=\"" + req.getContextPath() + SlackItConfigurationHolder.OAUTH_AUTHORIZE_URI + "?requestToken=true"
                        + "\">here</a> to generate a token");
            }
        }
    }
}
