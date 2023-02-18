package com.datasophon.api.configuration;

import com.datasophon.api.interceptor.LocaleChangeInterceptor;
import com.datasophon.api.interceptor.LoginHandlerInterceptor;
import com.datasophon.api.interceptor.UserPermissionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import java.util.Locale;

/**
 * application configuration
 */
@Configuration
public class AppConfiguration implements WebMvcConfigurer {

  public static final String LOGIN_INTERCEPTOR_PATH_PATTERN = "/**/*";
  public static final String LOGIN_PATH_PATTERN = "/login";
  public static final String PATH_PATTERN = "/**";
  public static final String LOCALE_LANGUAGE_COOKIE = "language";

  @Bean
  public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOrigin("*");
    config.addAllowedMethod("*");
    config.addAllowedHeader("*");
    UrlBasedCorsConfigurationSource configSource = new UrlBasedCorsConfigurationSource();
    configSource.registerCorsConfiguration(PATH_PATTERN, config);
    return new CorsFilter(configSource);
  }

  @Bean
  public LoginHandlerInterceptor loginInterceptor() {
    return new LoginHandlerInterceptor();
  }

  /**
   * Cookie
   * @return local resolver
   */
  @Bean(name = "localeResolver")
  public LocaleResolver localeResolver() {
    CookieLocaleResolver localeResolver = new CookieLocaleResolver();
    localeResolver.setCookieName(LOCALE_LANGUAGE_COOKIE);
    /** set default locale **/
    localeResolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
    /** set language tag compliant **/
    localeResolver.setLanguageTagCompliant(false);
    return localeResolver;
  }

  @Bean
  public LocaleChangeInterceptor localeChangeInterceptor() {
    return new LocaleChangeInterceptor();
  }

  @Bean
  public UserPermissionHandler userPermissionHandler() {
    return new UserPermissionHandler();
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    // i18n
    registry.addInterceptor(localeChangeInterceptor());
    registry.addInterceptor(userPermissionHandler());
    // login
    registry.addInterceptor(loginInterceptor())
            .addPathPatterns("/**").excludePathPatterns("/login","/error",
            "/service/install/downloadPackage",
            "/cluster/alert/history/save",
            "/cluster/kerberos/downloadKeytab"
    );
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
    registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
    registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");
    registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    registry.addResourceHandler("/ui/**").addResourceLocations("file:ui/");
  }

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/ui/").setViewName("forward:/ui/index.html");
    registry.addViewController("/").setViewName("forward:/ui/index.html");
  }

  /**
   * Turn off suffix-based content negotiation
   *
   * @param configurer configurer
   */
  @Override
  public void configureContentNegotiation(final ContentNegotiationConfigurer configurer) {
    configurer.favorPathExtension(false);
  }



}
