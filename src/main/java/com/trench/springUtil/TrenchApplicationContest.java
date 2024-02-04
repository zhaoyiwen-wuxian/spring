package com.trench.springUtil;

import com.trench.interfa.*;
import com.trench.service.BeanNameAware;
import com.trench.service.BeanPostProcessor;
import com.trench.service.InitializingBean;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class TrenchApplicationContest {

    private Class configClass;

    private ConcurrentHashMap<String,Object> concurrentHashMap=new ConcurrentHashMap<>();

    private ConcurrentHashMap<String,BeanDefintion> beanDefintionConcurrentHashMap=new ConcurrentHashMap<>();
    private List<BeanPostProcessor> beanPostProcessorList=new ArrayList<>();

    public TrenchApplicationContest(Class configClass) {
        this.configClass = configClass;
        scan(configClass);
        preInstantita();
    }

    private void preInstantita() {
        for (Map.Entry<String,BeanDefintion> entry :beanDefintionConcurrentHashMap.entrySet()){
            String beanName=entry.getKey();
            BeanDefintion beanDefintion=entry.getValue();
            if (beanDefintion.getScope().equals("singleton")){
                Object bean = saveBean(beanName,beanDefintion);
                concurrentHashMap.put(beanName,bean);
            }
        }
    }

    //创建bean
    public Object saveBean(String beanName,BeanDefintion beanDefintion) {
        Class aClass = beanDefintion.getaClass();
        AtomicReference<Object> instance = new AtomicReference<>();
        try {
            instance.set(aClass.getDeclaredConstructor().newInstance());
            //依赖注入
            insteadBean(aClass, instance);
            beanInstance(beanName, instance);

            return instance.get();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e){
            e.printStackTrace();
        }catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void beanInstance(String beanName, AtomicReference<Object> instance) throws Exception {
        //beanPost
        beanPostProcessorList.stream().forEach(beanPostProcessor -> {
            instance.set(beanPostProcessor.postProcessorBeforeInitialization(instance.get(), beanName));
        });
        //回调
        if (instance.get() instanceof BeanNameAware) {
            ((BeanNameAware) instance.get()).setBeanName(beanName);
        }

        //初始化
        if (instance.get() instanceof InitializingBean) {
            ((InitializingBean) instance.get()).afterPropertiesSet();
        }

        beanPostProcessorList.stream().forEach(beanPostProcessor -> {
            instance.set(beanPostProcessor.postProcessorAfterInitialization(instance.get(), beanName));
        });
    }

    private void insteadBean(Class aClass, AtomicReference<Object> instance) throws IllegalAccessException {
        for (Field declareField: aClass.getDeclaredFields()){
            if (declareField.isAnnotationPresent(Autowired.class)||
                    declareField.isAnnotationPresent(Resource.class)){
                Object bean = getBean(declareField.getName());
                if (bean==null){
                    throw new RuntimeException("没有该bean");
                }
                declareField.setAccessible(true);
                declareField.set(instance.get(),bean);
            }
        }
    }

    private void scan(Class configClass) {
        //解析配置类
        ComponenSan componenSan=(ComponenSan)  configClass.getDeclaredAnnotation(ComponenSan.class);
        //路径
        String path=componenSan.value();
        if (path.contains(".")){
            //app
            ClassLoader classLoader = TrenchApplicationContest.class.getClassLoader();
            File file = new File(classLoader.getResource(path.replace(".","/")).getFile());

            if (file.isDirectory()){
                File[] files = file.listFiles();
                classNameAndPost(classLoader, files);
            }
        }
    }

    private void classNameAndPost(ClassLoader classLoader, File[] files) {
        for (File fi: files){
            try {
                String absolutePath = fi.getAbsolutePath();
                if (absolutePath.endsWith(".class")) {
                    String classname = absolutePath.substring(absolutePath.indexOf("com"), absolutePath.indexOf(".class"));
                    classname = classname.replace("\\", ".");
                    Class<?> aClass = classLoader.loadClass(classname);

                    if (BeanPostProcessor.class.isAssignableFrom(aClass)){
                        BeanPostProcessor beanPostProcessor =(BeanPostProcessor) aClass.getDeclaredConstructor().newInstance();
                        beanPostProcessorList.add(beanPostProcessor);
                    }
                    isAnnotation(aClass);
                }
            } catch (ClassNotFoundException | NoSuchMethodException classNotFoundException) {
                classNotFoundException.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void isAnnotation(Class<?> aClass) {
        if (aClass.isAnnotationPresent(Component.class)) {
            //生成bean,解析bean判断是单一还是prototype
            Component annotation = aClass.getDeclaredAnnotation(Component.class);
            //BEAMN名字
            String beanName = annotation.value();
            BeanDefintion beanDefintion=new BeanDefintion();
            beanDefintion.setaClass(aClass);
            if (aClass.isAnnotationPresent(Scope.class)) {
                Scope scope = aClass.getDeclaredAnnotation(Scope.class);
                beanDefintion.setScope(scope.value());
            }else {
                beanDefintion.setScope("singleton");
            }
            beanDefintionConcurrentHashMap.put(beanName,beanDefintion);
        }
    }

    public Object getBean(String beanName){
        if (beanDefintionConcurrentHashMap.containsKey(beanName)){
            BeanDefintion beanDefintion = beanDefintionConcurrentHashMap.get(beanName);
            if (beanDefintion.getScope().equals("singleton")){
                Object o = concurrentHashMap.get(beanName);
                return o;
            }else {
                //创建bean对象
                return saveBean(beanName,beanDefintion);
            }
        }else {
            throw new RuntimeException("不存在bean");
        }
    }
}
