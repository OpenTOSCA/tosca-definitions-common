package org.opentosca.artifacttemplates;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WsdlFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(WsdlFilter.class);

    @Override
    public void init(FilterConfig filterConfig) {
        LOG.info("Initialized WsdlFilter enabling accessing the wsdl through ?wsdl at the service endpoint!");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if ("wsdl".equalsIgnoreCase(httpRequest.getQueryString())) {
            LOG.info("Redirecting request to WSDL location!");
            HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper(httpRequest) {
                @Override
                public String getQueryString() {
                    return null;
                }

                @Override
                public String getRequestURI() {
                    // redirect from ?wsdl to .wsdl
                    return super.getRequestURI() + ".wsdl";
                }
            };
            chain.doFilter(requestWrapper, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        LOG.info("Destroying WsdlFilter enabling accessing the wsdl through ?wsdl at the service endpoint!");
    }
}
