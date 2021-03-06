package com.lucifiere.container;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.StaticLog;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.lucifiere.common.ClassManager;
import com.lucifiere.common.GlobalConfig;
import com.lucifiere.templates.spec.Template;
import com.lucifiere.templates.spec.TemplateSpec;
import com.lucifiere.templates.spec.Templates;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 模板容器
 *
 * @author wuhuilin
 * Date 2020-8-23.
 */
public class GlobalContext {

    private GlobalConfig config;

    private final AtomicBoolean isInit = new AtomicBoolean(false);

    private final Map<String, TemplateSpec> templateMap = Maps.newConcurrentMap();

    private final Map<String, ManagedBeanSpec> componentMap = Maps.newConcurrentMap();

    public GlobalConfig getConfig() {
        return this.config;
    }

    @SuppressWarnings("unchecked")
    public <T> T getComponent(Class<T> clazz) {
        return (T) componentMap.values().stream().filter(c -> Objects.equals(clazz, c.getClazz())).map(ManagedBeanSpec::getInstant).findAny().orElseThrow(() -> new ContainerException("不存在目标类型的组件！"));
    }

    public <T, R> R calByComponent(Class<T> clazz, Function<T, R> function) {
        var bean = Objects.requireNonNull(getComponent(clazz));
        return function.apply(bean);
    }

    public <T> void doWithComponent(Class<T> clazz, Consumer<T> function) {
        var bean = Objects.requireNonNull(getComponent(clazz));
        function.accept(bean);
    }

    @SuppressWarnings("unchecked")
    public <T> T getComponent(String id) {
        return (T) componentMap.values().stream().filter(c -> Objects.equals(id, c.getId())).findAny().orElseThrow(() -> new ContainerException("不存在目标类型的组件！"));
    }

    public TemplateSpec getTemplateById(String id) {
        return templateMap.get(id);
    }

    public Set<TemplateSpec> getAllTemplates() {
        return Sets.newHashSet(templateMap.values());
    }

    public static GlobalContext create(GlobalConfig config) {
        var container = new GlobalContext();
        if (container.isInit.get()) {
            return container;
        }
        container.init0(config);
        return container;
    }

    private synchronized void init0(GlobalConfig config) {
        try {
            this.config = config;
            registerComponents();
            registerTemplates();
            processGlobalContextAware();
            this.isInit.compareAndSet(false, true);
        } catch (Exception e) {
            clear();
            StaticLog.error("全局上下文初始化失败！", e);
            throw new ContainerException("容器初始化失败", e);
        }
    }

    private void processGlobalContextAware() {
        componentMap.values().stream().map(ManagedBeanSpec::getInstant).forEach(bean -> {
            if (bean instanceof GlobalContextAware globalContextAware) {
                ReflectUtil.invoke(globalContextAware, "setGlobalContext", this);
            }
        });
    }

    private void registerTemplates(List<TemplateSpec> templates) {
        Optional.ofNullable(templates).ifPresent(templateSpecs -> templateSpecs.forEach(this::registerTemplate));
    }

    private void registerTemplate(TemplateSpec template) {
        if (null != templateMap.get(template.getId())) {
            return;
        }
        templateMap.put(template.getId(), template);
    }

    private static final String TEMPLATES_EMBED = "com.lucifiere.templates.embed";

    private void registerTemplates() {
        String customizedTPath = config.templatesConfigScanPath();
        Set<Class<?>> templates = ClassManager.getClazzByPath(customizedTPath, TEMPLATES_EMBED);
        templates.parallelStream().forEach(clazz -> {
            var ts = AnnotationUtil.getAnnotation(clazz, Templates.class);
            if (ts != null && !ts.skip()) {
                var ins = ReflectUtil.newInstance(clazz);
                Arrays.stream(clazz.getDeclaredMethods()).forEach(method -> {
                    var define = AnnotationUtil.getAnnotation(method, Template.class);
                    if (define != null) {
                        var obj = ReflectUtil.invoke(ins, method);
                        if (obj instanceof TemplateSpec spec) {
                            spec.setId(define.value());
                            templateMap.put(spec.getId(), spec);
                        }
                    }
                });
            }
        });
    }

    private void registerComponents() {
        Set<Class<?>> clazzSet = ClassManager.getCoderPlusClazz();
        clazzSet.forEach(clazz -> {
            var an = AnnotationUtil.getAnnotation(clazz, ManagedBean.class);
            if (an != null) {
                ManagedBeanSpec c = ManagedBeanSpec.of(clazz);
                Optional.of(an.value()).filter(StrUtil::isNotBlank).ifPresent(c::setId);
                componentMap.put(c.getId(), c);
            }
        });
    }

    private void clear() {
        StaticLog.error("模板快照 --> {0}, 组件快照 --> {1}, " + JSONUtil.toJsonStr(templateMap), JSONUtil.toJsonStr(componentMap));
        templateMap.clear();
        componentMap.clear();
    }

}
