package kr.co.victoryfairy.support.webfilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class PathPatternWebFilter extends OncePerRequestFilter {

    private final List<String> includePathPatterns = new ArrayList<>();

    private final List<String> excludePathPatterns = new ArrayList<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        if (excludePathPatterns.stream().anyMatch(pattern -> pathMatches(pattern, requestUri))) {
            filterChain.doFilter(request, response);
            return;
        }

        if (includePathPatterns.stream().anyMatch(pattern -> pathMatches(pattern, requestUri))) {
            filterMatched(request, response, filterChain);
            return;
        }

        filterChain.doFilter(request, response);
    }

    protected boolean pathMatches(String pattern, String requestUri) {
        AntPathMatcher matcher = new AntPathMatcher();
        return matcher.match(pattern, requestUri);
    }

    protected void addIncludePathPatterns(String... patterns) {
        includePathPatterns.addAll(List.of(patterns));
    }

    protected void addExcludePathPatterns(String... patterns) {
        excludePathPatterns.addAll(List.of(patterns));
    }

    public abstract void filterMatched(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException;

}