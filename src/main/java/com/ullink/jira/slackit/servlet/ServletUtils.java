package com.ullink.jira.slackit.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class ServletUtils {

    public static void printError(Logger logger, HttpServletResponse resp, String msg) throws IOException {
        if (logger != null)
            logger.error(msg);
        resp.getWriter().write(msg);
        resp.getWriter().flush();
    }

    public static void printInfo(Logger logger, HttpServletResponse resp, String msg) throws IOException {
        if (logger != null)
            logger.info(msg);
        resp.getWriter().write(msg);
        resp.getWriter().flush();
    }
}
