package com.trench.service;

public interface BeanPostProcessor {
    Object postProcessorBeforeInitialization(Object bean,String beanName);

    Object postProcessorAfterInitialization(Object bean,String beanName);
}
