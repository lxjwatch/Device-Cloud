package center.misaki.device.Auth.config;

import center.misaki.device.base.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.UnapprovedClientAuthenticationException;
import org.springframework.security.oauth2.provider.NoSuchClientException;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 自定义异常处理
 */
@Component
@Slf4j
public class CustomAuthenticationEntryPoint extends OAuth2AuthenticationEntryPoint {
    private String typeName = OAuth2AccessToken.BEARER_TYPE;

    private String realmName = "oauth";
    @Autowired
    private ObjectMapper objectMapper;
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {

            response.setStatus(HttpStatus.OK.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(Result.error(authException.getMessage())));

    }

    @Override
    protected ResponseEntity<?> enhanceResponse(ResponseEntity<?> response, Exception exception) {
        HttpHeaders headers = response.getHeaders();
        String existing = null;
        if (headers.containsKey("WWW-Authenticate")) {
            existing = extractTypePrefix(headers.getFirst("WWW-Authenticate"));
        }
        StringBuilder builder = new StringBuilder();
        builder.append(typeName+" ");
        builder.append("realm=\"" + realmName + "\"");
        if (existing!=null) {
            builder.append(", "+existing);
        }
        HttpHeaders update = new HttpHeaders();
        update.putAll(response.getHeaders());
        update.set("WWW-Authenticate", builder.toString());
        if(exception instanceof UnapprovedClientAuthenticationException)
        {
            return new ResponseEntity<Object>(Result.error(401,exception.getMessage()), update, HttpStatus.OK);
        }
        return new ResponseEntity<Object>(Result.error(401,"token过期，或者错误"), update, HttpStatus.OK);

    }

    private String extractTypePrefix(String header) {
        String existing = header;
        String[] tokens = existing.split(" +");
        if (tokens.length > 1 && !tokens[0].endsWith(",")) {
            existing = StringUtils.arrayToDelimitedString(tokens, " ").substring(existing.indexOf(" ") + 1);
        }
        return existing;
    }
}
