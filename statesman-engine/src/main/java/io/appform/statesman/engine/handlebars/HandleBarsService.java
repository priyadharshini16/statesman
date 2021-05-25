package io.appform.statesman.engine.handlebars;

import com.github.jknack.handlebars.*;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;
import com.google.common.base.Strings;
import com.google.inject.Singleton;
import io.appform.statesman.engine.utils.DateUtils;
import io.appform.statesman.model.exception.ResponseCode;
import io.appform.statesman.model.exception.StatesmanError;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Singleton
public class HandleBarsService {

    private static final ValueResolver[] NOTIFY_VALUE_RESOLVERS = {
            JavaBeanValueResolver.INSTANCE,
            FieldValueResolver.INSTANCE,
            MapValueResolver.INSTANCE,
            JsonNodeValueResolver.INSTANCE,
            MethodValueResolver.INSTANCE
    };

    private Handlebars handlebars;
    private Map<String, Template> compiledTemplates;


    public HandleBarsService() {
        this(Clock.system(ZoneId.of(DateUtils.getLocalZone())));
    }

    public HandleBarsService(Clock clock) {
        handlebars = new Handlebars();
        registerHelpers(handlebars, clock);
        compiledTemplates = new ConcurrentHashMap<>();
    }

    @Nullable
    public String transform(String template, Object data) {
        try {
            if(Strings.isNullOrEmpty(template)) {
                return null;
            }
            if (!compiledTemplates.containsKey(template)) {
                addTemplate(template);
            }
            return compiledTemplates.get(template).apply(Context.newBuilder(data)
                    .resolver(NOTIFY_VALUE_RESOLVERS)
                    .build());
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.TRANSFORMATION_ERROR);
        }
    }

    public String transform(ValueResolver resolver, String template, Object data) {
        try {
            if(Strings.isNullOrEmpty(template)) {
                return null;
            }
            if (!compiledTemplates.containsKey(template)) {
                addTemplate(template);
            }
            return compiledTemplates.get(template).apply(Context.newBuilder(data)
                    .resolver(resolver)
                    .build());
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.TRANSFORMATION_ERROR);
        }
    }

    private void registerHelpers(Handlebars handlebars, Clock clock) {
        HandleBarsHelperRegistry.newInstance(handlebars, clock).register();
    }

    private synchronized void addTemplate(String template) throws Exception {
        if (!compiledTemplates.containsKey(template)) {
            compiledTemplates.put(template, handlebars.compileInline(template));
        }
    }
}