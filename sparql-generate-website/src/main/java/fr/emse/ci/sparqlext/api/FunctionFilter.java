/*
 * Copyright 2017 École des Mines de Saint-Étienne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.emse.ci.sparqlext.api;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class filters the calls to resources, and dispatches to the endpoint
 * that exposes the ontology that defines the resource.
 *
 * @author maxime.lefrancois
 */
@WebFilter(urlPatterns = {"/fn/*"})
public class FunctionFilter implements Filter {
    
    private static final Logger LOG = LoggerFactory.getLogger(FunctionFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOG.info("starting...");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = ((HttpServletRequest) request);
        String contextPath = req.getContextPath() + "/";
        String requestURI = req.getRequestURI();
        String resourcePath = requestURI.substring(contextPath.length());
        String functionName = resourcePath.substring(3);
        String redirection;
        if(functionName.equals("")) {
            redirection = contextPath+"apidocs/fr/emse/ci/sparqlext/function/library/package-summary.html";
        } else {
            redirection = contextPath+"apidocs/fr/emse/ci/sparqlext/function/library/FUN_"+functionName+".html";
        }
        HttpServletResponse res = (HttpServletResponse) response;
        res.setHeader("Location", redirection);
        res.setStatus(HttpServletResponse.SC_SEE_OTHER); 
        res.flushBuffer();
        return;
    }

    @Override
    public void destroy() {
    }
}