package io.appform.statesman.model.action.template.httpaction;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@Data
public class FormData {
    @NotNull
    private FormFieldType type;

    @NotEmpty
    private String key;

    @NotEmpty
    private String value;
}
