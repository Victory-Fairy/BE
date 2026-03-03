package kr.co.victoryfairy.support.config;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.victoryfairy.support.interceptor.CurlCommandErrorInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web에 사용되는 Configuration을 여기에 위치 한다.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	private static final Set<String> ALLOWED_ORIGINS = Set.of("http://localhost:8080", "http://localhost:3000",
			"https://victory-fairy.duckdns.org", "https://fe-next-sigma.vercel.app", "https://victoryfairy.shop",
			"https://seungyo.shop");

	private final CurlCommandErrorInterceptor curlCommandErrorInterceptor;

	@Bean
	public FilterRegistrationBean<Filter> corsFilter() {
		FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
		bean.setFilter(new OncePerRequestFilter() {
			@Override
			protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
					FilterChain filterChain) throws ServletException, IOException {
				String origin = request.getHeader("Origin");
				if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
					response.setHeader("Access-Control-Allow-Origin", origin);
					response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
					response.setHeader("Access-Control-Allow-Headers",
							"Content-Type, Authorization, Accept, X-Requested-With, Origin, clientid");
					response.setHeader("Access-Control-Allow-Credentials", "true");
				}
				if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
					response.setStatus(HttpServletResponse.SC_OK);
					return;
				}
				filterChain.doFilter(request, response);
			}
		});
		bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return bean;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(curlCommandErrorInterceptor).addPathPatterns("/**");
	}

}
