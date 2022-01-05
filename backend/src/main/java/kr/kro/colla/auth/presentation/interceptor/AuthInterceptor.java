package kr.kro.colla.auth.presentation.interceptor;

import kr.kro.colla.auth.service.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

@RequiredArgsConstructor
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Cookie[] cookies = request.getCookies();
        Cookie accessToken = parseCookies(cookies, "accessToken");

        if(accessToken == null || jwtProvider.validateToken(accessToken.getValue())) {
            return false;
        }

        request.setAttribute("accessToken", accessToken.getValue());
        return true;
    }

    private Cookie parseCookies(Cookie[] cookies, String name) {
        return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(name))
                .findAny()
                .orElse(null);
    }

}
