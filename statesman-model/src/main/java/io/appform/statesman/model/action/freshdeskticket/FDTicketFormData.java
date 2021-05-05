package io.appform.statesman.model.action.freshdeskticket;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@Data
public class FDTicketFormData {
    @NotNull
    private FieldType type;

    @NotEmpty
    private String key;

    @NotEmpty
    private String value;
}
