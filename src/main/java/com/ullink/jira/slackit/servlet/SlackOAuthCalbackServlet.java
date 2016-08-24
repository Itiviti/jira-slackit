package com.ullink.jira.slackit.servlet;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.ullink.jira.slackit.servlet.ServletUtils.printError;

import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.codehaus.jackson.annotate.JsonProperty;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.client.urlconnection.HttpURLConnectionFactory;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;
import com.ullink.jira.slackit.managers.SlackItConfigurationHolder;

public class SlackOAuthCalbackServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(SlackOAuthCalbackServlet.class.getName());

    private SlackItConfigurationHolder slackItConfigurationHolder;

    public SlackOAuthCalbackServlet(SlackItConfigurationHolder slackItConfigurationHolder) {
        this.slackItConfigurationHolder = checkNotNull(slackItConfigurationHolder);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String code = req.getParameter("code");
        String state = req.getParameter("state");
        String error = req.getParameter("error");

        LOG.info("Slack code : " + code);
        LOG.info("Slack state : " + state);
        LOG.info("Slack error : " + error);

        if ("access_denied".equals(error)) {
            printError(LOG, resp, "JiraMember denied access.");
            return;
        }

        Client client = new Client(new URLConnectionClientHandler(
                new HttpURLConnectionFactory() {
            Proxy p = null;

            @Override
            public HttpURLConnection getHttpURLConnection(URL url) throws IOException {
                    if(slackItConfigurationHolder.hasProxy()) {
                        p = slackItConfigurationHolder.getProxy();
                    } else {
                        p = Proxy.NO_PROXY;
                    }
                    return (HttpURLConnection) url.openConnection(p);
            }
        }));
        String redirectUri = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL) + SlackItConfigurationHolder.OAUTH_REDIRECT_URL;
        client.setConnectTimeout(28 * 1000);
        WebResource webResource = client.resource("https://slack.com/api").path("oauth.access")
                .queryParam("client_id", slackItConfigurationHolder.getSlackAppClientId())
                .queryParam("client_secret", slackItConfigurationHolder.getSlackAppClientSecret()).queryParam("code", code)
                .queryParam("redirect_uri", redirectUri);

        LOG.info("Slack OAuth url : " + webResource.toString());

        Builder builder = webResource.type(MediaType.APPLICATION_JSON);
        ClientResponse response;
        try {
            response = builder.get(ClientResponse.class);
        } catch (Exception e) {
            printError(LOG, resp, "Error during oauth.access GET.");
            return;
        }

        String entity = response.getEntity(String.class);
        String token = null, replyError = null;
        try {
            JSONObject jsonObject = new JSONObject(entity);
            token = jsonObject.getString("access_token");
            replyError = jsonObject.has("error") ? jsonObject.getString("error") : null;
        } catch (JSONException e) {
            LOG.error(e.getMessage(), e);
        }

        if (token != null && state != null) {
            resp.getWriter().write("token: " + token);
            resp.sendRedirect(req.getContextPath() + SlackItConfigurationHolder.OAUTH_AUTHORIZE_URI + "?token=" + token + "&state=" + state);
        } else {
            printError(LOG, resp, "Wrong state or token. Error: " + replyError);
        }
        resp.getWriter().flush();
    }

    public static class SlackResponse implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("access_token")
        private String token;
        private String scope;

        public SlackResponse() {
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

    }
}
