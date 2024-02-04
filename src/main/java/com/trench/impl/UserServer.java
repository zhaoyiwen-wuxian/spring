package com.trench.impl;

import com.trench.interfa.Autowired;
import com.trench.interfa.Component;
import com.trench.interfa.Scope;
import com.trench.service.BeanNameAware;
import com.trench.service.InitializingBean;

@Component("userServer")
@Scope(value = "prototype")
public class UserServer implements BeanNameAware, InitializingBean {

    @Autowired
    private ObjectServer objectServer;

    private String str;

    public void test(){
        System.out.println(objectServer);
        System.out.println(str);
    }

    @Override
    public void setBeanName(String beanName) {
        str=beanName;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("初始化");
    }
}
