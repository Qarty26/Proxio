package com.proxio.backend.models.logs;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("execution(* com.proxio.backend.service.*.*(..))")
    public void serviceMethods() {}

    @Before("serviceMethods()")
    public void logBefore(JoinPoint joinPoint) {
        log.debug("Method args {}: {}",
                joinPoint.getSignature().getName(),
                joinPoint.getArgs());
        log.info("Join in method: {} from class: {}",
                joinPoint.getSignature().getName(),
                joinPoint.getTarget().getClass().getSimpleName());
    }

    @AfterReturning(pointcut = "serviceMethods()", returning = "result")
    public void logAfterSuccess(JoinPoint joinPoint, Object result) {
        log.info("Method {} finished successfully.", joinPoint.getSignature().getName());
    }

    @AfterThrowing(pointcut = "serviceMethods()", throwing = "exception")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable exception) {
        log.error("Error in method: {} | Message: {}",
                joinPoint.getSignature().getName(),
                exception.getMessage());
    }
}