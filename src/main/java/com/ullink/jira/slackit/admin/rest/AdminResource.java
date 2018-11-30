package com.ullink.jira.slackit.admin.rest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;

import com.atlassian.jira.rest.api.util.ErrorCollection;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.ullink.jira.slackit.managers.SlackItConfigurationHolder;

@Path("/admin")
public class AdminResource {

    private static final Logger LOG = Logger.getLogger(AdminResource.class);
    private final UserManager userManager;
    private SlackItConfigurationHolder slackItConfigurationHolder;

    public AdminResource(UserManager userManager, SlackItConfigurationHolder slackItConfigurationHolder) {
        this.userManager = userManager;
        this.slackItConfigurationHolder = slackItConfigurationHolder;
    }

    @GET
    @Path("/reloadProperties")
    @Produces({MediaType.APPLICATION_JSON})
    public Response reloadProperties(@Context HttpServletRequest request) {
        LOG.info("Reloading slakit configuration");
        UserProfile user = userManager.getRemoteUser(request);
        if (user == null || !userManager.isSystemAdmin(user.getUserKey())) {
            return Response.status(Status.UNAUTHORIZED).build();
        }
        ErrorCollection errors = slackItConfigurationHolder.forceReload();
        if (errors.hasAnyErrors()) {
            return Response.ok().entity("{\"status\":\"error\",\"title\":\"Error\",\"message\":\"" + errors.toString() + "\"").build();
        } else {
            return Response.ok().entity("{\"status\":\"success\",\"title\":\"Success\",\"message\":\"Configuration file successfully reloaded\"}").build();
        }
    }
}
