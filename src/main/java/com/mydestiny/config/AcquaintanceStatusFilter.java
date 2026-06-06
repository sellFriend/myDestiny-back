package com.mydestiny.config;

import com.mydestiny.domain.enums.ProfileStatus;
import com.mydestiny.repository.DatingProfileRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AcquaintanceStatusFilter extends OncePerRequestFilter {

    private final DatingProfileRepository profileRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 폼 경로는 무조건 통과 (폼 작성/수정은 허용)
        if (request.getRequestURI().startsWith("/form/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // JWT로 인증된 요청만 체크 (비인증 요청은 Security에서 처리)
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof UsernamePasswordAuthenticationToken)
                || !(auth.getPrincipal() instanceof String userId)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (profileRepository.existsBySubjectIdAndStatusNot(userId, ProfileStatus.DELETED)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"매물로 등록된 사용자는 서비스를 이용할 수 없습니다.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
