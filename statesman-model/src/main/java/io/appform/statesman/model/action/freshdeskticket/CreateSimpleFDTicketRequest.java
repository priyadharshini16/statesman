package io.appform.statesman.model.action.freshdeskticket;

import lombok.Data;

@Data
public class CreateSimpleFDTicketRequest extends CreateFreshDeskTicketRequest {
    private String payload;
    public CreateSimpleFDTicketRequest(FreshDeskTicketType ticketType, String authorization) {
        super(ticketType, authorization);
    }
}
