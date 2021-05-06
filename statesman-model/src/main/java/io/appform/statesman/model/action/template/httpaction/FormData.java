package io.appform.statesman.model.action.template.httpaction;

import lombok.Data;

@Data
public class FormData {
    private FormFieldType type;
    private String key;
    private String value;
}
