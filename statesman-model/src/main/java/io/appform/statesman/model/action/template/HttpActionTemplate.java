package io.appform.statesman.model.action.template;

import com.fasterxml.jackson.databind.JsonNode;
import io.appform.statesman.model.action.ActionType;
import io.appform.statesman.model.action.template.httpaction.FormData;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class HttpActionTemplate extends ActionTemplate {

    @NotNull
    @NotEmpty
    private String method;

    @NotNull
    @NotEmpty
    private String url;

    private String payload;

    private String headers;

    private String responseTranslator;

    private List<FormData> formData;

    private boolean noop;

    private JsonNode noopResponse;

    public HttpActionTemplate() {
        super(ActionType.HTTP);
    }

    @Builder
    public HttpActionTemplate(String templateId,
                              String name,
                              boolean active,
                              String method,
                              String url,
                              String payload,
                              String headers,
                              String responseTranslator,
                              boolean noop,
                              JsonNode noopResponse,
                              List<FormData> formData) {
        super(ActionType.HTTP, templateId, name, active);
        this.method = method;
        this.url = url;
        this.payload = payload;
        this.headers = headers;
        this.responseTranslator = responseTranslator;
        this.noop = noop;
        this.noopResponse = noopResponse;
        this.formData = formData;
    }

    @Override
    public <T> T visit(ActionTemplateVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
