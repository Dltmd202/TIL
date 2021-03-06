# 예외 처리와 오류 페이지

### 서블릿 예외 처리


### 서블릿은 다음 2가지 방식으로 예외 처리를 지원한다. 
* `Exception` (예외)
* `response.sendError(HTTP 상태 코드, 오류 메시지)`

### Exception(예외)

#### 자바 직접 실행
자바의 메인 메서드를 직접 실행하는 경우 main 이라는 이름의 쓰레드가 실행된다.
실행 도중에 예외를 잡지 못하고 처음 실행한 main() 메서드를 넘어서 예외가 던져지면, 예외 정보를 남기고 해당 쓰레드는 종료된다.


#### 웹 애플리케이션
웹 애플리케이션은 사용자 요청별로 별도의 쓰레드가 할당되고, 서블릿 컨테이너 안에서 실행된다. 
애플리케이션에서 예외가 발생했는데, 어디선가 try ~ catch로 예외를 잡아서 처리하면 아무런 문제가 없다.

```
WAS(여기까지 전파) <- 필터 <- 서블릿 <- 인터셉터 <- 컨트롤러(예외발생)
```

#### ServletExController

```java
package hello.exception.servlet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class ServletExController {

    @GetMapping("/error-ex")
    public void errorEx(){
        throw new RuntimeException("예외 발생");
    }
}
```

```
HTTP Status 500 – Internal Server Error
```

웹 브라우저에서 개발자 모드로 확인해보면 HTTP 상태 코드가 500으로 보인다.
Exception 의 경우 서버 내부에서 처리할 수 없는 오류가 발생한 것으로 생각해서 HTTP 상태 코드 500을 반환한다.


#### http://localhost:8080/no-page

```
HTTP Status 404 - Not Found
```

#### response.sendError(HTTP 상태 코드, 오류 메시지)

오류가 발생했을 때 `HttpServletResponse` 가 제공하는 `sendError` 라는 메서드를 사용해도 된다. 
이것을 호출한다고 당장 예외가 발생하는 것은 아니지만, 서블릿 컨테이너에게 오류가 발생했다는 점을 전달할 수 있다.
이 메서드를 사용하면 HTTP 상태 코드와 오류 메시지도 추가할 수 있다.



* `response.sendError(HTTP 상태 코드)`
* `response.sendError(HTTP 상태 코드, 오류 메시지)`

#### ServletExController


```java
@GetMapping("/error-404")
public void error404(HttpServletResponse response) throws IOException {
response.sendError(404, "404 오류!");
}

@GetMapping("/error-500")
public void error500(HttpServletResponse response) throws IOException {
response.sendError(500);
}
```

#### sendError 흐름

```
WAS(sendError 호출 기록 확인) <- 필터 <- 서블릿 <- 인터셉터 <- 컨트롤러(response.sendError())
```

`response.sendError()` 를 호출하면 `response` 내부에는 오류가 발생했다는 상태를 저장해둔다.
그리고 서블릿 컨테이너는 고객에게 응답 전에 `response` 에 `sendError()` 가 호출되었는지 확인한다.
그리고 호출되었다면 설정한 오류 코드에 맞추어 기본 오류 페이지를 보여준다.

### 서블릿 예외 처리 - 오류 화면 제공

서블릿은 Exception (예외)가 발생해서 서블릿 밖으로 전달되거나 또는 `response.sendError()` 가 호출 되었을 때 
각각의 상황에 맞춘 오류 처리 기능을 제공한다.
이 기능을 사용하면 친절한 오류 처리 화면을 준비해서 고객에게 보여줄 수 있다.

```xml
<web-app>
      <error-page>
        <error-code>404</error-code>
        <location>/error-page/404.html</location>
      </error-page>
      <error-page>
        <error-code>500</error-code>
        <location>/error-page/500.html</location>
      </error-page>
      <error-page>
        <exception-type>java.lang.RuntimeException</exception-type>
        <location>/error-page/500.html</location>
      </error-page>
</web-app>
```

#### 서블릿 오류 페이지 등록

```java
package hello.exception;

import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.http.HttpStatus;

public class WebServerCustomizer implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {


    @Override
    public void customize(ConfigurableWebServerFactory factory) {

        ErrorPage errorPage404 = new ErrorPage(HttpStatus.NOT_FOUND, "/error-page/400");
        ErrorPage errorPage500 = new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error-page/500");
        ErrorPage errorPageEx = new ErrorPage(RuntimeException.class, "/error-page/500");
        factory.addErrorPages(errorPage404, errorPage500, errorPageEx);

    }
}
```

* `response.sendError(404)`: errorPage404 호출
* `response.sendError(500)`: errorPage500 호출
* `RuntimeException` 또는 그 자식 타입의 예외: `errorPageEx` 호출

500 예외가 서부 내부에서 발생한 오류라는 뜻을 포함하고 있기 때문에 예외도 500 화면으로 처리

오류 페이지는 예외를 다룰 때 해당 예외와 그 자식 타입의 오류를 함께 처리한다. 
예를 들어서 위의 경우 `RuntimeException` 은 물론이고 `RuntimeException` 의 자식도 함께 처리한다.

오류 페이지는 예외를 다룰 때 해당 예외와 그 자식 타입의 오류를 함께 처리한다. 
예를 들어서 위의 경우 `RuntimeException` 은 물론이고 `RuntimeException` 의 자식도 함께 처리한다.


### 서블릿 예외 처리 - 오류 페이지 작동 원리

서블릿은 `Exception` (예외)가 발생해서 서블릿 밖으로 전달되거나 또는 `response.sendError()` 가 호출
되었을 때 설정된 오류 페이지를 찾는다.

#### 예외 발생 흐름

```
WAS(여기까지 전파) <- 필터 <- 서블릿 <- 인터셉터 <- 컨트롤러(예외발생)
```

#### sendError 흐름

```
WAS(sendError 호출 기록 확인) <- 필터 <- 서블릿 <- 인터셉터 <- 컨트롤러 (response.sendError())
```

#### WAS는 해당 예외를 처리하는 오류 페이지 정보를 확인한다.

`new ErrorPage(RuntimeException.class, "/error-page/500")`


예를 들어서 RuntimeException 예외가 WAS까지 전달되면,
WAS는 오류 페이지 정보를 확인한다. 
확인해보니 `RuntimeException` 의 오류 페이지로 `/error-page/500` 이 지정되어 있다. 
WAS는 오류 페이지를 출력하기 위해 `/error-page/500` 를 다시 요청한다.

#### 오류 페이지 요청 흐름

```
WAS `/error-page/500` 다시 요청 -> 필터 -> 서블릿 -> 인터셉터 -> 컨트롤러(/error-page/ 500) -> View
```

#### 예외 발생과 오류 페이지 요청 흐름

```
1. WAS(여기까지 전파) <- 필터 <- 서블릿 <- 인터셉터 <- 컨트롤러(예외발생)
2. WAS `/error-page/500` 다시 요청 -> 필터 -> 서블릿 -> 인터셉터 -> 컨트롤러(/error- page/500) -> View
```

#### 중요한 점은 웹 브라우저(클라이언트)는 서버 내부에서 이런 일이 일어나는지 전혀 모른다는 점이다. 오직 서버 내부에서 오류 페이지를 찾기 위해 추가적인 호출을 한다.

### 오류 정보 추가

WAS는 오류 페이지를 단순히 다시 요청만 하는 것이 아니라, 오류 정보를 `request` 의 `attribute` 에 추가해서 넘겨준다.
필요하면 오류 페이지에서 이렇게 전달된 오류 정보를 사용할 수 있다.

### 서블릿 예외 처리 - 필터

#### 예외 발생과 오류 페이지 요청 흐름

```
1. WAS(여기까지 전파) <- 필터 <- 서블릿 <- 인터셉터 <- 컨트롤러(예외발생)
2. WAS `/error-page/500` 다시 요청 -> 필터 -> 서블릿 -> 인터셉터 -> 컨트롤러(/error- page/500) -> View
```



오류가 발생하면 오류 페이지를 출력하기 위해 WAS 내부에서 다시 한번 호출이 발생한다. 
이때 필터, 서블릿, 인터셉터도 모두 다시 호출된다. 
그런데 로그인 인증 체크 같은 경우를 생각해보면, 이미 한번 필터나, 
인터셉터에서 로그인 체크를 완료했다. 따라서 서버 내부에서 오류 페이지를 호출한다고 해서 해당 필터나 인터셉트가 한번 더 호출되는 것은 매우 비효율적이다.
결국 클라이언트로 부터 발생한 정상 요청인지, 아니면 오류 페이지를 출력하기 위한 내부 요청인지 구분할 수 있어야 한다.
서블릿은 이런 문제를 해결하기 위해 `DispatcherType` 이라는 추가 정보를 제공한다.


#### DispatcherType

필터는 이런 경우를 위해서 `dispatcherTypes` 라는 옵션을 제공한다. 이전 강의의 마지막에 다음 로그를 추가했다.
`log.info("dispatchType={}", request.getDispatcherType())`
그리고 출력해보면 오류 페이지에서 `dispatchType=ERROR` 로 나오는 것을 확인할 수 있다.
고객이 처음 요청하면 `dispatcherType=REQUEST` 이다.
이렇듯 서블릿 스펙은 실제 고객이 요청한 것인지, 서버가 내부에서 오류 페이지를 요청하는 것인지 `DispatcherType` 으로 구분할 수 있는 방법을 제공한다.

#### `javax.servlet.DispatcherType`

```java
public enum DispatcherType {
      FORWARD,
      INCLUDE,
      REQUEST,
      ASYNC,
      ERROR
}
```

#### DispatcherType

* `REQUEST` : 클라이언트 요청 
* `ERROR` : 오류 요청
* `FORWARD` : MVC에서 배웠던 서블릿에서 다른 서블릿이나 JSP를 호출할 때 `RequestDispatcher.forward(request, response);`
* `INCLUDE` : 서블릿에서 다른 서블릿이나 JSP의 결과를 포함할 때 `RequestDispatcher.include(request, response);`
* `ASYNC` : 서블릿 비동기 호출


#### 필터와 DispatcherType

#### LogFilter

```java
package hello.exception.filter;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;


@Slf4j
public class LogFilter implements Filter {


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("log filter init");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestURI = httpRequest.getRequestURI();

        String uuid = UUID.randomUUID().toString();

        try {
            log.info("REQUEST  [{}][{}][{}]", uuid,
                    request.getDispatcherType(), requestURI);
            chain.doFilter(request, response);
        } catch (Exception e) {
            throw e;
        } finally {
            log.info("RESPONSE [{}][{}][{}]", uuid,
                    request.getDispatcherType(), requestURI);
        }

    }

    @Override
    public void destroy() {
        log.info("log filter destroy");
    }
}
```

#### WebConfig

```java
package hello.exception;

import hello.exception.filter.LogFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;

public class WebConfig implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean logFilter() {
        FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(new LogFilter());
        filterRegistrationBean.setOrder(1);
        filterRegistrationBean.addUrlPatterns("./*");
        filterRegistrationBean.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ERROR);
        return filterRegistrationBean;
    }
}
```

`filterRegistrationBean.setDispatcherTypes(DispatcherType.REQUEST,
DispatcherType.ERROR)`
이렇게 두 가지를 모두 넣으면 클라이언트 요청은 물론이고, 오류 페이지 요청에서도 필터가 호출된다. 
아무것도 넣지 않으면 기본 값이 `DispatcherType.REQUEST` 이다. 즉 클라이언트의 요청이 있는 경우에만 필터가 적용된다. 특별히 오류 페이지 경로도 필터를 적용할 것이 아니면, 기본 값을 그대로 사용하면 된다.

물론 오류 페이지 요청 전용 필터를 적용하고 싶으면 `DispatcherType.ERROR` 만 지정하면 된다

#### 서블릿 예외 처리 - 인터셉터

#### LogInterceptor

```java
package hello.exception.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@Slf4j
public class LogInterceptor implements HandlerInterceptor {

    public static final String LOG_ID = "logId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();

        String uuid = UUID.randomUUID().toString();
        request.setAttribute(LOG_ID, uuid);

        log.info("REQUEST [{}][{}][{}]", uuid, request.getDispatcherType(), handler);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        log.info("postHandle [{}]", modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        String requestURI = request.getRequestURI();
        String logId = (String) request.getAttribute(LOG_ID);
        log.info("RESPONE [{}][{}][{}]", logId, request.getDispatcherType(), requestURI);

        if(ex != null){
            log.error("afterCompletion error!", ex);
        }
    }
}
```

앞서 필터의 경우에는 필터를 등록할 때 어떤 `DispatcherType` 인 경우에 필터를 적용할 지 선택할 수 이었다. 그런데 인터셉터는 서블릿이 제공하는 기능이
아니라 스프링이 제공하는 기능이다. 따라서 `DispatcherType` 과 무관하게 항상 호출된다.

대신에 인터셉터는 다음과 같이 요청 경로에 따라서 추가하거나 제외하기 쉽게 되어 있기 때문에, 이러한 설정을 사용해서 오류 페이지 경로를
`excludePathPattern` 를 사용해서 빼주면 된다.



#### 전체 흐름 정리

```java
package hello.exception;

import hello.exception.filter.LogFilter;
import hello.exception.interceptor.LogInterceptor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;

public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LogInterceptor())
                .order(1)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "*.iso", "/error", "/error-page.**");
    }

//    @Bean
    public FilterRegistrationBean logFilter() {
        FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(new LogFilter());
        filterRegistrationBean.setOrder(1);
        filterRegistrationBean.addUrlPatterns("./*");
        filterRegistrationBean.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ERROR);
        return filterRegistrationBean;
    }
}
```

인터셉터와 중복으로 처리되지 않기 위해 앞의 `logFilter()` 의 `@Bean` 에 주석을 달아두자. 
여기에서 `/error-page/**` 를 제거하면 `error-page/500` 같은 내부 호출의 경우에도 인터셉터가 호출된다.


#### 전체 흐름 정리
* 필터는 DispatcherType 으로 중복 호출 제거(`dispatcherType=REQUEST`)
* 인터셉터는 경로 정보로 중복 호출 제거(`excludePathPattern("/error-page/**")`)

```text
1. WAS(/error-ex, dispatchType=REQUEST) -> 필터 -> 서블릿 -> 인터셉터 -> 컨트롤러
2. WAS(여기까지 전파) <- 필터 <- 서블릿 <- 인터셉터 <- 컨트롤러(예외발생)
3. WAS 오류 페이지 확인
4. WAS(/error-page/500, dispatchType=ERROR) -> 필터(x) -> 서블릿 -> 인터셉터(x) -> 컨트롤러(/error-page/500) -> View
```

### 스프링 부트 - 오류 페이지1

지금까지 예외 처리 페이지를 만들기 위해서 다음과 같은 복잡한 과정을 거쳤다. 
* `WebServerCustomizer` 를 만들고
* 예외 종류에 따라서 `ErrorPage` 를 추가하고
* 예외 처리용 컨트롤러 `ErrorPageController` 를 만듬

#### 스프링 부트는 이런 과정을 모두 기본으로 제공한다.

* `ErrorPage` 를 자동으로 등록한다. 이때 `/error` 라는 경로로 기본 오류 페이지를 설정한다.
* `new ErrorPage("/error")` , 상태코드와 예외를 설정하지 않으면 기본 오류 페이지로 사용된다. 
* 서블릿 밖으로 예외가 발생하거나, `response.sendError(...)` 가 호출되면 모든 오류는 `/error` 를 호출하게 된다.
* `BasicErrorController` 라는 스프링 컨트롤러를 자동으로 등록한다. 
  * `ErrorPage` 에서 등록한 `/error` 를 매핑해서 처리하는 컨트롤러다.

> 참고
> 
> `ErrorMvcAutoConfiguration` 이라는 클래스가 오류 페이지를 자동으로 등록하는 역할을 한다.


#### 개발자는 오류 페이지만 등록

`BasicErrorController` 는 기본적인 로직이 모두 개발되어 있다.
개발자는 오류 페이지 화면만 `BasicErrorController` 가 제공하는 룰과 우선순위에 따라서 등록하면 된다. 
정적 HTML이면 정적 리소스, 뷰 템플릿을 사용해서 동적으로 오류 화면을 만들고 싶으면 뷰 템플릿 경로에 오류 페이지 파일을 만들어서 넣어두기만 하면 된다.

#### 뷰 선택 우선순위

`BasicErrorController`의 처리 순서

1. 뷰템플릿 
   * `resources/templates/error/500.html `
   * `resources/templates/error/5xx.html`

2. 정적리소스(`static`,`public`) 
   * `resources/static/error/400.html`
   * `resources/static/error/404.html`
   * `resources/static/error/4xx.html`

3. 적용 대상이 없을 때 뷰 이름(`error`)
   * `resources/templates/error.html`


#### 스프링 부트 - 오류 페이지2

BasicErrorController가 제공하는 기본 정보들

`BasicErrorController` 컨트롤러는 다음 정보를 model에 담아서 뷰에 전달한다. 뷰 템플릿은 이 값을 활용해서 출력할 수 있다.

```
* timestamp: Fri Feb 05 00:00:00 KST 2021
* status: 400
* error: Bad Request
* exception: org.springframework.validation.BindException * trace: 예외 trace
* message: Validation failed for object='data'. Error count: 1 * errors: Errors(BindingResult)
* path: 클라이언트 요청 경로 (`/hello`)
```

#### 오류 정보 추가 - resources/templates/error/500.html

```html
<!DOCTYPE HTML>
  <html xmlns:th="http://www.thymeleaf.org">
  <head>
      <meta charset="utf-8">
  </head>
<body>
  <div class="container" style="max-width: 600px">
      <div class="py-5 text-center">
        <h2>500 오류 화면 스프링 부트 제공</h2> 
      </div>
      <div>
        <p>오류 화면 입니다.</p>
      </div> 
      <ul>
        <li>오류 정보</li> 
      <ul>
          <li th:text="|timestamp: ${timestamp}|"></li>
          <li th:text="|path: ${path}|"></li>
          <li th:text="|status: ${status}|"></li>
          <li th:text="|message: ${message}|"></li>
          <li th:text="|error: ${error}|"></li>
          <li th:text="|exception: ${exception}|"></li>
          <li th:text="|errors: ${errors}|"></li>
          <li th:text="|trace: ${trace}|"></li>
      </ul>
    </li> 
    </ul>
      <hr class="my-4">
  </div>
</body>
</html>
```

오류 관련 내부 정보들을 고객에게 노출하는 것은 좋지 않다. 고객이 해당 정보를 읽어도 혼란만 더해지고, 보안상 문제가 될 수도 있다.
그래서 `BasicErrorController` 오류 컨트롤러에서 다음 오류 정보를 `model` 에 포함할지 여부 선택할 수 있다.


`application.properties`
* `server.error.include-exception=false` : `exception` 포함 여부( true , false )
* `server.error.include-message=never` : `message` 포함 여부 
* `server.error.include-stacktrace=never` : `trace` 포함 여부
* `server.error.include-binding-errors=never` : `errors` 포함 여부


기본 값이 `never`인 부분은 다음 3가지 옵션을 사용할 수 있다. `never`, `always`, `on_param`

* `never` : 사용하지 않음
* `always` :항상 사용
* `on_param` : 파라미터가 있을 때 사용

`on_param` 은 파라미터가 있으면 해당 정보를 노출한다. 디버그 시 문제를 확인하기 위해 사용할 수 있다. 
그런데 이 부분도 개발 서버에서 사용할 수 있지만, 운영 서버에서는 권장하지 않는다.
`on_param` 으로 설정하고 다음과 같이 HTTP 요청시 파라미터를 전달하면 해당 정보들이 `model` 에 담겨서 뷰 템플릿에서 출력된다.

#### 스프링 부트 오류 관련 옵션

* `server.error.whitelabel.enabled=true` : 오류 처리 화면을 못 찾을 시, 스프링 whitelabel 오류 페이지 적용
* `server.error.path=/error` : 오류 페이지 경로, 스프링이 자동 등록하는 서블릿 글로벌 오류 페이지 경로와 
  `BasicErrorController` 오류 컨트롤러 경로에 함께 사용된다.

#### 확장 포인트

에러 공통 처리 컨트롤러의 기능을 변경하고 싶으면 `ErrorController` 인터페이스를 상속 받아서 
구현하거나 `BasicErrorController` 상속 받아서 기능을 추가하면 된다.

## API 예외

#### `WebServerCustomizer`

```java
package hello.exception;

import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class WebServerCustomizer implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {


    @Override
    public void customize(ConfigurableWebServerFactory factory) {

        ErrorPage errorPage404 = new ErrorPage(HttpStatus.NOT_FOUND, "/error-page/404");
        ErrorPage errorPage500 = new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error-page/500");
        ErrorPage errorPageEx = new ErrorPage(RuntimeException.class, "/error-page/500");
        factory.addErrorPages(errorPage404, errorPage500, errorPageEx);

    }
}
```

`response.sendError()` 가 호출되면 위에 등록한 예외 페이지 경로가 호출된다.


#### ApiExceptionController

```java
package hello.exception.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class ApiExceptionController {

    @GetMapping("/api/members/{id}")
    public MemberDto getMember(@PathVariable("id") String id){
        if(id.equals("ex")){
            throw new RuntimeException("잘못된 사용자");
        }
        return new MemberDto(id, "hello " + id);
    }


    @Data
    @AllArgsConstructor
    static class MemberDto{
        private String memberId;
        private String name;
    }
}
```

#### 정상 호출

`http://localhost:8080/api/members/spring`

```json
{
      "memberId": "spring",
      "name": "hello spring"
  }
```

#### 예외 발생 호출

`http://localhost:8080/api/members/ex`

```html
<html>
500에러~~
</html>
```

오류 페이지 컨트롤러도 JSON 응답을 할 수 있도록 수정해야 한다.

#### ErrorPageController

```java
package hello.exception.servlet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
public class ErrorPageController {

    //RequestDispatcher 상수로 정의되어 있음
    public static final String ERROR_EXCEPTION = "javax.servlet.error.exception";
    public static final String ERROR_EXCEPTION_TYPE = "javax.servlet.error.exception_type";
    public static final String ERROR_MESSAGE = "javax.servlet.error.message";
    public static final String ERROR_REQUEST_URI = "javax.servlet.error.request_uri";
    public static final String ERROR_SERVLET_NAME = "javax.servlet.error.servlet_name";
    public static final String ERROR_STATUS_CODE = "javax.servlet.error.status_code";
    

    @RequestMapping(value = "/error-page/500", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> errorPage500Api(
            HttpServletRequest request, HttpServletResponse response
    ){
        Map<String, Object> result = new HashMap<>();
        Exception ex = (Exception) request.getAttribute(ERROR_EXCEPTION);
        result.put("status", request.getAttribute(ERROR_STATUS_CODE));
        result.put("message", ex.getMessage());

        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        return new ResponseEntity<>(result, HttpStatus.valueOf(statusCode));
    }

    
}
```

`produces = MediaType.APPLICATION_JSON_VALUE` 의 뜻은 클라이언트가 요청하는 HTTP Header의 `Accept` 의 값이 `application/json`
일 때 해당 메서드가 호출된다는 것이다. 결국 클라어인트가 받고 싶은 미디어타입이 json이면 이 컨트롤러의 메서드가 호출된다.


응답 데이터를 위해서 `Map` 을 만들고 `status` , `message` 키에 값을 할당했다. 
Jackson 라이브러리는 `Map` 을 JSON 구조로 변환할 수 있다.

### API 예외 처리 - 스프링 부트 기본 오류 처리

#### BasicErrorController

```java
@RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
public ModelAndView errorHtml(HttpServletRequest request, HttpServletResponse response) {}

@RequestMapping
public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {}
```

`/error` 동일한 경로를 처리하는 `errorHtml()` , `error()` 두 메서드를 확인할 수 있다.

* `errorHtml()` : `produces = MediaType.TEXT_HTML_VALUE` : 클라이언트 요청의 Accept 해더 값이 `text/html` 인 경우에는 
  `errorHtml()` 을 호출해서 view를 제공한다.
* `error()` : 그외 경우에 호출되고 `ResponseEntity` 로 HTTP Body에 JSON 데이터를 반환한다.


`http://localhost:8080/api/members/ex`

```json
{
    "timestamp": "2021-04-28T00:00:00.000+00:00", "status": 500,
    "error": "Internal Server Error",
    "exception": "java.lang.RuntimeException",
    "trace": "java.lang.RuntimeException:...",
    "message": "잘못된 사용자",
      "path": "/api/members/ex"
}
```

다음 옵션들을 설정하면 더 자세한 오류 정보를 추가할 수 있다.
* server.error.include-binding-errors=always 
* server.error.include-exception=true 
* server.error.include-message=always 
* server.error.include-stacktrace=always


### API 예외 처리 - HandlerExceptionResolver

예외가 발생해서 서블릿을 넘어 WAS까지 예외가 전달되면 HTTP 상태코드가 500으로 처리된다. 발생하는 예외에 따라서 400, 404 등등 다른 상태코드도 처리하고 싶다.
오류 메시지, 형식등을 API마다 다르게 처리하고 싶다.

#### 상태코드 변환

예를 들어서 `IllegalArgumentException` 을 처리하지 못해서 컨트롤러 밖으로 넘어가는 일이 발생하면 HTTP 상태코드를 400으로 처리하고 싶다.

#### ApiExceptionController

```java
package hello.exception.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class ApiExceptionController {

    @GetMapping("/api/members/{id}")
    public MemberDto getMember(@PathVariable("id") String id){
        if(id.equals("ex")){
            throw new RuntimeException("잘못된 사용자");
        }
        if(id.equals("bad")){
            throw new IllegalArgumentException("잘못된 입력값");
        }
        return new MemberDto(id, "hello " + id);
    }


    @Data
    @AllArgsConstructor
    static class MemberDto{
        private String memberId;
        private String name;
    }
}
```

#### HandlerExceptionResolver

스프링 MVC는 컨트롤러(핸들러) 밖으로 예외가 던져진 경우 예외를 해결하고, 동작을 새로 정의할 수 있는 방법을 제공한다. 
컨트롤러 밖으로 던져진 예외를 해결하고, 동작 방식을 변경하고 싶으면 `HandlerExceptionResolver` 를 사용하면 된다. 
줄여서 `ExceptionResolver` 라 한다.


#### ExceptionResolver 적용 전

![](./res/1.png)

#### ExceptionResolver 적용 후

![](./res/2.png)

참고: `ExceptionResolver` 로 예외를 해결해도 `postHandle()` 은 호출되지 않는다.

#### HandlerExceptionResolver - 인터페이스

```java
public interface HandlerExceptionResolver {
    ModelAndView resolveException(
      HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex);
}
```

* `handler` : 핸들러(컨트롤러) 정보
* `Exception ex` : 핸들러(컨트롤러)에서 발생한 발생한 예외

#### MyHandlerExceptionResolver

```java
package hello.exception.resolver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class MyHandlerExceptionResolver implements HandlerExceptionResolver {

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            if (ex instanceof IllegalArgumentException) {
                log.info("IllegalArgumentException resolver to 400");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
                return new ModelAndView();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }
}
```

* `ExceptionResolver` 가 `ModelAndView` 를 반환하는 이유는 마치 `try`, `catch를` 하듯이, 
  `Exception` 을 처리해서 정상 흐름 처럼 변경하는 것이 목적이다. 이름 그대로 `Exception` 을 Resolver(해결)하는 것이 목적이다.

여기서는 `IllegalArgumentException` 이 발생하면 `response.sendError(400)` 를 호출해서 
HTTP 상태 코드를 400으로 지정하고, 빈 `ModelAndView` 를 반환한다.

반환 값에 따른 동작 방식
`HandlerExceptionResolver` 의 반환 값에 따른 `DispatcherServlet` 의 동작 방식은 다음과 같다.


* 빈 `ModelAndView`: `new ModelAndView()` 처럼 빈 `ModelAndView` 를 반환하면 뷰를 렌더링 하지 않고, 정상 흐름으로 서블릿이 리턴된다.
* `ModelAndView` 지정: `ModelAndView` 에 `View` , `Model` 등의 정보를 지정해서 반환하면 뷰를 렌더링 한다.
* `null`: `null` 을 반환하면, 다음 `ExceptionResolver` 를 찾아서 실행한다. 만약 처리할 수 있는 `ExceptionResolver` 가 없으면 
  예외 처리가 안되고, 기존에 발생한 예외를 서블릿 밖으로 던진다.


#### `ExceptionResolver` 활용 

* 예외 상태 코드 변환
  * 예외를 `response.sendError(xxx)` 호출로 변경해서 서블릿에서 상태 코드에 따른 오류를 처리하도록 위임 
  * 이후 WAS는 서블릿 오류 페이지를 찾아서 내부 호출, 예를 들어서 스프링부트가 기본으로 설정한 `/error` 가 호출됨 

* 뷰 템플릿 처리
  * `ModelAndView` 에 값을 채워서 예외에 따른 새로운 오류 화면 뷰 렌더링 해서 고객에게 제공

* API 응답 처리
  * `response.getWriter().println("hello");` 처럼 HTTP 응답 바디에 직접 데이터를 넣어주는 것도 가능하다. 
    여기에 JSON 으로 응답하면 API 응답 처리를 할 수 있다.

#### WebConfig - 수정

`WebMvcConfigurer`

```java
@Override
public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
    resolvers.add(new MyHandlerExceptionResolver());
}
```

`configureHandlerExceptionResolvers(..)` 를 사용하면 스프링이 기본으로 등록하는 `ExceptionResolver` 가 제거되므로 주의, 
`extendHandlerExceptionResolvers` 를 사용

### API 예외 처리 - HandlerExceptionResolver 활용

#### 예외를 여기서 마무리하기
예외가 발생하면 WAS까지 예외가 던져지고, WAS에서 오류 페이지 정보를 찾아서 다시 `/error` 를 호출하는 과정은 생각해보면 너무 복잡하다. 
`ExceptionResolver` 를 활용하면 예외가 발생했을 때 이런 복잡한 과정 없이 여기에서 문제를 깔끔하게 해결할 수 있다.

#### UserException

```java
package hello.exception.exception;

public class UserException extends RuntimeException{

    public UserException() {
        super();
    }

    public UserException(String message) {
        super(message);
    }

    public UserException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserException(Throwable cause) {
        super(cause);
    }

    protected UserException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
```

#### ApiExceptionController - 예외 추가

```java
@GetMapping("/api/members/{id}")
public MemberDto getMember(@PathVariable("id") String id){
    if(id.equals("ex")){
        throw new RuntimeException("잘못된 사용자");
    }
    if(id.equals("bad")){
        throw new IllegalArgumentException("잘못된 입력값");
    }
    if(id.equals("user-ex")){
        throw new UserException("사용자 오류");
    }
    return new MemberDto(id, "hello " + id);
}
```

#### UserHandlerExceptionResolver

```java
package hello.exception.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import hello.exception.exception.UserException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class UserHandlerExceptionResolver implements HandlerExceptionResolver {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try{
            if(ex instanceof UserException){
                log.info("UserException resolver to 400");
                String accpetHeader = request.getHeader("accept");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

                if("application/json".equals(accpetHeader)){
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("ex", ex.getClass());
                    errorResult.put("message", ex.getMessage());

                    String result = objectMapper.writeValueAsString(errorResult);

                    response.setContentType("application/json");
                    response.setCharacterEncoding("utf-8");
                    response.getWriter().write(result);

                    return new ModelAndView();
                } else {
                    return new ModelAndView("error/500");
                }
            }
        }catch (IOException e){
            log.error("resollver ex", e);
        }
        return null;
    }
}
```

HTTP 요청 해더의 `ACCEPT` 값이 `application/json` 이면 JSON으로 오류를 내려주고, 
그 외 경우에는 error/500에 있는 HTML 오류 페이지를 보여준다.


### API 예외 처리 - 스프링이 제공하는 ExceptionResolver1

스프링 부트가 기본으로 제공하는 `ExceptionResolver` 는 다음과 같다. `HandlerExceptionResolverComposite` 에 다음 순서로 등록

1. ExceptionHandlerExceptionResolver
2. ResponseStatusExceptionResolver
3. DefaultHandlerExceptionResolver 우선 순위가 가장 낮다.

#### ExceptionHandlerExceptionResolver

`@ExceptionHandler` 을 처리한다. API 예외 처리는 대부분 이 기능으로 해결한다. 조금 뒤에 자세히 설명한다.


#### ResponseStatusExceptionResolver

HTTP 상태 코드를 지정해준다.

예) `@ResponseStatus(value = HttpStatus.NOT_FOUND)`

#### DefaultHandlerExceptionResolver

스프링 내부 기본 예외를 처리한다.

먼저 가장 쉬운 `ResponseStatusExceptionResolver` 부터 알아보자.

### ResponseStatusExceptionResolver

`ResponseStatusExceptionResolver` 는 예외에 따라서 HTTP 상태 코드를 지정해주는 역할을 한다. 

다음 두 가지 경우를 처리한다.
* `@ResponseStatus` 가 달려있는 예외 
* `ResponseStatusException` 예외

하나씩 확인해보자.

예외에 다음과 같이 `@ResponseStatus` 애노테이션을 적용하면 HTTP 상태 코드를 변경해준다.

```java
package hello.exception.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "잘못된 요청 오류") 
public class BadRequestException extends RuntimeException {
}
```

`BadRequestException` 예외가 컨트롤러 밖으로 넘어가면 `ResponseStatusExceptionResolver` 예외가 해당 애노테이션을 확인해서 
오류 코드를 `HttpStatus.BAD_REQUEST` (400)으로 변경하고, 메시지도 담는다.

`ResponseStatusExceptionResolver` 코드를 확인해보면 결국 `response.sendError(statusCode, resolvedReason)` 를 호출하는 것을 확인할 수 있다.
`sendError(400)` 를 호출했기 때문에 WAS에서 다시 오류 페이지( `/error` )를 내부 요청한다.

#### 메시지 기능

`reason` 을 `MessageSource` 에서 찾는 기능도 제공한다. `reason = "error.bad"`

```java
package hello.exception.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "잘못된 요청 오류") 
@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "error.bad") 
public class BadRequestException extends RuntimeException {
}
```

#### ResponseStatusException

`@ResponseStatus` 는 개발자가 직접 변경할 수 없는 예외에는 적용할 수 없다. 
(애노테이션을 직접 넣어야 하는데, 내가 코드를 수정할 수 없는 라이브러리의 예외 코드 같은 곳에는 적용할 수 없다.)
추가로 애노테이션을 사용하기 때문에 조건에 따라 동적으로 변경하는 것도 어렵다. 이때는 `ResponseStatusException` 예외를 사용하면 된다.

### API 예외 처리 - 스프링이 제공하는 ExceptionResolver2

`DefaultHandlerExceptionResolver` 는 스프링 내부에서 발생하는 스프링 예외를 해결한다. 
대표적으로 파라미터 바인딩 시점에 타입이 맞지 않으면 내부에서 `TypeMismatchException` 이 발생하는데, 
이 경우 예외가 발생했기 때문에 그냥 두면 서블릿 컨테이너까지 오류가 올라가고, 결과적으로 500 오류가 발생한다.

그런데 파라미터 바인딩은 대부분 클라이언트가 HTTP 요청 정보를 잘못 호출해서 발생하는 문제이다.
HTTP 에서는 이런 경우 HTTP 상태 코드 400을 사용하도록 되어 있다. 
`DefaultHandlerExceptionResolver` 는 이것을 500 오류가 아니라 HTTP 상태 코드 400 오류로 변경한다.
스프링 내부 오류를 어떻게 처리할지 수 많은 내용이 정의되어 있다.

`DefaultHandlerExceptionResolver.handleTypeMismatch` 를 보면 다음과 같은 코드를 확인할 수 있다.
`response.sendError(HttpServletResponse.SC_BAD_REQUEST)`
결국 `response.sendError()` 를 통해서 문제를 해결한다.

`sendError(400)` 를 호출했기 때문에 WAS에서 다시 오류 페이지( `/error` )를 내부 요청한다.

#### ApiExceptionController

```java
@GetMapping("/api/default-handler-ex")
public String defaultException(@RequestParam Integer data){
    return "ok";   
}
```

#### 과정 
1 `ExceptionHandlerExceptionResolver` 
2 `ResponseStatusExceptionResolver` -> HTTP 응답 코드 변경
3 `DefaultHandlerExceptionResolver` -> 스프링 내부 예외 처리

`HandlerExceptionResolver` 를 직접 사용하기는 복잡하다. API 오류 응답의 경우 `response` 에 직접 데이터를 넣어야 해서 매우 불편하고 번거롭다. 
`ModelAndView` 를 반환해야 하는 것도 API에는 잘 맞지 않는다.
스프링은 이 문제를 해결하기 위해 `@ExceptionHandler` 라는 매우 혁신적인 예외 처리 기능을 제공한다.

### API 예외 처리 - @ExceptionHandler

#### HTML 화면 오류 vs API 오류

웹 브라우저에 HTML 화면을 제공할 때는 오류가 발생하면 `BasicErrorController` 를 사용하는게 편하다.
이때는 단순히 5xx, 4xx 관련된 오류 화면을 보여주면 된다. `BasicErrorController` 는 이런 메커니즘을 모두 구현해두었다.

그런데 API는 각 시스템 마다 응답의 모양도 다르고, 스펙도 모두 다르다. 
예외 상황에 단순히 오류 화면을 보여주는 것이 아니라, 예외에 따라서 각각 다른 데이터를 출력해야 할 수도 있다. 
그리고 같은 예외라고 해도 어떤 컨트롤러에서 발생했는가에 따라서 다른 예외 응답을 내려주어야 할 수 있다. 한마디로 매우 세밀한 제어가 필요하다.

#### API 예외처리의 어려운 점

* `HandlerExceptionResolver` 를 떠올려 보면 `ModelAndView` 를 반환해야 했다. 이것은 API 응답에는 필요하지 않다.
* API 응답을 위해서 `HttpServletResponse` 에 직접 응답 데이터를 넣어주었다. 이것은 매우 불편하다.
* 특정 컨트롤러에서만 발생하는 예외를 별도로 처리하기 어렵다. 
  예를 들어서 회원을 처리하는 컨트롤러에서 발생하는 `RuntimeException` 예외와 상품을 관리하는 컨트롤러에서 발생하는 동일한 
  `RuntimeException` 예외를 서로 다른 방식으로 처리할 수 없다.

#### `@ExceptionHandler`

스프링은 API 예외 처리 문제를 해결하기 위해 `@ExceptionHandler` 라는 애노테이션을 사용하는 매우 편리한 예외 처리 기능을 제공하는데, 
이것이 바로 `ExceptionHandlerExceptionResolver` 이다. 스프링은 `ExceptionHandlerExceptionResolver` 를 기본으로 제공하고, 
기본으로 제공하는 `ExceptionResolver` 중에 우선순위도 가장 높다.

```java
package hello.exception.exhandler;

import hello.exception.exception.UserException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
public class ApiExceptionV2Controller {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResult illegalExHandler(IllegalArgumentException e){
        log.error("[exceptionHandler ex", e);
        return new ErrorResult("BAD", e.getMessage());
    }

    @ExceptionHandler(UserException.class)
    public ResponseEntity userExHandler(UserException e){
        log.error("[exceptionHandler] ex", e);
        ErrorResult errorResult = new ErrorResult("USER-EX", e.getMessage());
        return new ResponseEntity(errorResult, HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler
    public ErrorResult exHander(Exception e){
        log.error("[exceptionHandler] ex", e.getMessage());
        return new ErrorResult("EX", "내부 오류");
    }

    @GetMapping("/api2/members/{id}")
    public MemberDto getMember(@PathVariable("id") String id){
        if(id.equals("ex")){
            throw new RuntimeException("잘못된 사용자");
        }
        if(id.equals("bad")){
            throw new IllegalArgumentException("잘못된 입력값");
        }
        if(id.equals("user-ex")){
            throw new UserException("사용자 오류");
        }
        return new MemberDto(id, "hello " + id);
    }

    @Data
    @AllArgsConstructor
    static class MemberDto{
        private String memberId;
        private String name;
    }
}
```

#### `@ExceptionHandler` 예외 처리 방법
`@ExceptionHandler` 애노테이션을 선언하고, 해당 컨트롤러에서 처리하고 싶은 예외를 지정해주면 된다. 
해당 컨트롤러에서 예외가 발생하면 이 메서드가 호출된다. 참고로 지정한 예외 또는 그 예외의 자식 클래스는 모두 잡을 수 있다.

다음 예제는 `IllegalArgumentException` 또는 그 하위 자식 클래스를 모두 처리할 수 있다.

```java
@ExceptionHandler(IllegalArgumentException.class)
public ErrorResult illegalExHandle(IllegalArgumentException e) {
  log.error("[exceptionHandle] ex", e);
  return new ErrorResult("BAD", e.getMessage());
}
```

#### 우선순위
스프링의 우선순위는 항상 자세한 것이 우선권을 가진다. 예를 들어서 부모, 자식 클래스가 있고 다음과 같이 예외가 처리된다.

```java
@ExceptionHandler(부모예외.class) 
public String 부모예외처리()(부모예외 e) {}

@ExceptionHandler(자식예외.class) 
public String 자식예외처리()(자식예외 e) {}
```

`@ExceptionHandler` 에 지정한 부모 클래스는 자식 클래스까지 처리할 수 있다. 따라서 자식예외 가
발생하면 부모예외처리() , 자식예외처리() 둘다 호출 대상이 된다. 그런데 둘 중 더 자세한 것이 우선권을 가지므로 자식예외처리() 가 호출된다. 
물론 부모예외 가 호출되면 부모예외처리() 만 호출 대상이 되므로 부모예외처리() 가 호출된다.

#### 다양한 예외

다음과 같이 다양한 예외를 한번에 처리할 수 있다.

```java
@ExceptionHandler({AException.class, BException.class})
public String ex(Exception e) {
  log.info("exception e", e);
}
```

#### 예외 생략

`@ExceptionHandler` 에 예외를 생략할 수 있다. 생략하면 메서드 파라미터의 예외가 지정된다.

```java
@ExceptionHandler
public ResponseEntity<ErrorResult> userExHandle(UserException e) {}
```

#### 파리미터와 응답

`@ExceptionHandler` 에는 마치 스프링의 컨트롤러의 파라미터 응답처럼 다양한 파라미터와 응답을 지정할 수 있다.

https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann- exceptionhandler-args

#### 실행 흐름

* 컨트롤러를 호출한 결과 `IllegalArgumentException` 예외가 컨트롤러 밖으로 던져진다. 
* 예외가 발생했으로 `ExceptionResolver` 가 작동한다. 가장 우선순위가 높은 `ExceptionHandlerExceptionResolver` 가 실행된다.
* `ExceptionHandlerExceptionResolver` 는 해당 컨트롤러에 `IllegalArgumentException` 을 처리할 수 있는 `@ExceptionHandler` 가 있는지 확인한다.
* `illegalExHandle()` 를 실행한다. `@RestController` 이므로 `illegalExHandle()` 에도 `@ResponseBody` 가 적용된다. 
  따라서 HTTP 컨버터가 사용되고, 응답이 다음과 같은 JSON으로 반환된다.
* `@ResponseStatus(HttpStatus.BAD_REQUEST)` 를 지정했으므로 HTTP 상태 코드 400으로 응답한다.

#### UserException 처리

```java
@ExceptionHandler(UserException.class)
public ResponseEntity userExHandler(UserException e){
    log.error("[exceptionHandler] ex", e);
    ErrorResult errorResult = new ErrorResult("USER-EX", e.getMessage());
    return new ResponseEntity(errorResult, HttpStatus.BAD_REQUEST);
}
```

* `@ExceptionHandler` 에 예외를 지정하지 않으면 해당 메서드 파라미터 예외를 사용한다. 여기서는 `UserException` 을 사용한다.
* `ResponseEntity` 를 사용해서 HTTP 메시지 바디에 직접 응답한다. 물론 HTTP 컨버터가 사용된다. 
  `ResponseEntity` 를 사용하면 HTTP 응답 코드를 프로그래밍해서 동적으로 변경할 수 있다. 
  앞서 살펴본 `@ResponseStatus` 는 애노테이션이므로 HTTP 응답 코드를 동적으로 변경할 수 없다.


#### Exception

```java
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
@ExceptionHandler
public ErrorResult exHander(Exception e){
    log.error("[exceptionHandler] ex", e.getMessage());
    return new ErrorResult("EX", "내부 오류");
}
```

`throw new RuntimeException("잘못된 사용자")`이코드가 실행되면서, 컨트롤러밖으로 `RuntimeException` 이 던져진다.
`RuntimeException` 은 `Exception` 의 자식 클래스이다. 
따라서 이 메서드가 호출된다. `@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)` 로 HTTP 상태 코드를 500으로 응답한다.

### API 예외 처리 - @ControllerAdvice

`@ExceptionHandler` 를 사용해서 예외를 깔끔하게 처리할 수 있게 되었지만, 정상 코드와 예외 처리 코드가 하나의 컨트롤러에 섞여 있다. 
`@ControllerAdvice` 또는 `@RestControllerAdvice` 를 사용하면 둘을 분리할 수 있다.

#### ExControllerAdvice

```java
package hello.exception.exhandler;

import hello.exception.exception.UserException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExControllerAdvice {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResult illegalExHandler(IllegalArgumentException e){
        log.error("[exceptionHandler ex", e);
        return new ErrorResult("BAD", e.getMessage());
    }

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ErrorResult> userExHandler(UserException e){
        log.error("[exceptionHandler] ex", e);
        ErrorResult errorResult = new ErrorResult("USER-EX", e.getMessage());
        return new ResponseEntity<>(errorResult, HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler
    public ErrorResult exHander(Exception e){
        log.error("[exceptionHandler] ex", e.getMessage());
        return new ErrorResult("EX", "내부 오류");
    }
}
```

#### ApiExceptionV2Controller 코드에 있는 @ExceptionHandler 모두 제거

```java
package hello.exception.exhandler;

import hello.exception.exception.UserException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
public class ApiExceptionV2Controller {

    @GetMapping("/api2/members/{id}")
    public MemberDto getMember(@PathVariable("id") String id){
        if(id.equals("ex")){
            throw new RuntimeException("잘못된 사용자");
        }
        if(id.equals("bad")){
            throw new IllegalArgumentException("잘못된 입력값");
        }
        if(id.equals("user-ex")){
            throw new UserException("사용자 오류");
        }
        return new MemberDto(id, "hello " + id);
    }

    @Data
    @AllArgsConstructor
    static class MemberDto{
        private String memberId;
        private String name;
    }
}
```

#### @ControllerAdvice

* `@ControllerAdvice` 는 대상으로 지정한 여러 컨트롤러에 `@ExceptionHandler` , `@InitBinder` 기능을 부여해주는 역할을 한다.
* `@ControllerAdvice` 에 대상을 지정하지 않으면 모든 컨트롤러에 적용된다. (글로벌 적용)
* `@RestControllerAdvice` 는 `@ControllerAdvice` 와 같고, `@ResponseBody` 가 추가되어 있다. 
  `@Controller` , `@RestController` 의 차이와 같다.

#### 대상 컨트롤러 지정 방법

```java
// Target all Controllers annotated with @RestController
@ControllerAdvice(annotations = RestController.class)
public class ExampleAdvice1 {}

// Target all Controllers within specific packages
@ControllerAdvice("org.example.controllers")
public class ExampleAdvice2 {}

// Target all Controllers assignable to specific classes
@ControllerAdvice(assignableTypes = {ControllerInterface.class, AbstractController.class})
public class ExampleAdvice3 {}
```

https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann- controller-advice

스프링 공식 문서 예제에서 보는 것 처럼 특정 애노테이션이 있는 컨트롤러를 지정할 수 있고, 특정 패키지를 직접 지정할 수도 있다. 
패키지 지정의 경우 해당 패키지와 그 하위에 있는 컨트롤러가 대상이 된다. 그리고 특정 클래스를 지정할 수도 있다.
대상 컨트롤러 지정을 생략하면 모든 컨트롤러에 적용된다.

