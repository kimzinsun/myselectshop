package com.sparta.myselectshop.aop;

import com.sparta.myselectshop.entity.ApiUseTime;
import com.sparta.myselectshop.entity.User;
import com.sparta.myselectshop.repository.ApiUseTimeRepository;
import com.sparta.myselectshop.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j(topic = "UseTimeAop")
@Aspect
@Component
@RequiredArgsConstructor
public class UserTimeAop {
    private final ApiUseTimeRepository apiUseTimeRepository;

    @Pointcut("execution(* com.sparta.myselectshop.controller.ProductController.*(..))")
    private void product() {
    }

    @Pointcut("execution(* com.sparta.myselectshop.controller.FolderController.*(..))")
    private void folder() {
    }

    @Pointcut("execution(* com.sparta.myselectshop.naver.controller.NaverApiController.*(..))")
    private void naver() {
    }

    @Around("product() || folder() || naver()")  // 메소드 실행 전후에 적용할 내용
    public Object execute(ProceedingJoinPoint joinPoint) throws Throwable {
        // 측정 시작 시간
        long startTime = System.currentTimeMillis();
        try {
            // 핵심기능 실행
            Object output = joinPoint.proceed();
            return output;
        } finally {
            // 측정 종료 시간
            long endTime = System.currentTimeMillis();
            long runTime = endTime - startTime;

            // 사용자 정보 가져오기
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if(auth!=null && auth.getPrincipal().getClass() == UserDetailsImpl.class) {
                UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
                User loginUser = userDetails.getUser();

                ApiUseTime apiUseTime = apiUseTimeRepository.findByUser(loginUser).orElse(null);

                if(apiUseTime == null) {
                    apiUseTime = new ApiUseTime(loginUser, runTime);
                } else {
                    apiUseTime.addUseTime(runTime);
                }

                log.info("API 사용 시간: " + runTime + "ms"+ " / 사용자: " + loginUser.getUsername());
                apiUseTimeRepository.save(apiUseTime);
            }

        }
    }

}
