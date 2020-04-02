package io.appform.statesman.server.dao.callback;

import io.appform.statesman.server.callbacktransformation.TransformationTemplate;
import io.appform.statesman.server.callbacktransformation.TranslationTemplateType;

import java.util.List;
import java.util.Optional;

public interface CallbackTemplateProvider {

    Optional<TransformationTemplate> createTemplate(TransformationTemplate workflowTemplate);

    Optional<TransformationTemplate> updateTemplate(TransformationTemplate workflowTemplate);

    Optional<TransformationTemplate> getTemplate(String provider, TranslationTemplateType translationTemplateType);

    List<TransformationTemplate> getAll();
}
