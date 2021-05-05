package io.appform.statesman.model.action.freshdeskticket;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Data
@ToString
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "ticketType", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CreateSimpleFDTicketRequest.class, name = "SIMPLE_TICKET"),
        @JsonSubTypes.Type(value = CreateFDTicketWithAttachmentRequest.class, name = "TICKET_WITH_ATTACHMENT"),

})
public  abstract class CreateFreshDeskTicketRequest {
    @NotNull
    protected final FreshDeskTicketType ticketType;
    private final String authorization;
}
