package io.appform.statesman.model.action.freshdeskticket;

import lombok.Data;

import java.util.List;

@Data
public class CreateFDTicketWithAttachmentRequest extends CreateFreshDeskTicketRequest {
    private List<FDTicketFormData> fdTicketFormData;

    public CreateFDTicketWithAttachmentRequest(FreshDeskTicketType ticketType, String authorization) {
        super(ticketType, authorization);
    }
}
