package ca.bc.gov.educ.api.student.profile.email.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class StudentProfileEmailRequestInterceptor extends HandlerInterceptorAdapter {

    private static final Logger log = LoggerFactory.getLogger(StudentProfileEmailRequestInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getMethod() != null && request.getRequestURL() != null)
            log.info("{} {}", request.getMethod(), request.getRequestURL());
        if (request.getQueryString() != null)
            log.debug("Query string     : {}", request.getQueryString());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        int status = response.getStatus();
        if(status >= 200 && status < 300) {
            log.info("RESPONSE STATUS: {}", status);
        } else {
            log.error("RESPONSE STATUS: {}", status);
        }
    }
}
