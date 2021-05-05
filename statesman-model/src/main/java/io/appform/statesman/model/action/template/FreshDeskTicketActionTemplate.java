package io.appform.statesman.model.action.template;

import io.appform.statesman.model.action.ActionType;
import io.appform.statesman.model.action.freshdeskticket.CreateFreshDeskTicketRequest;
import io.appform.statesman.model.action.freshdeskticket.FreshDeskTicketType;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class FreshDeskTicketActionTemplate extends ActionTemplate {



    private CreateFreshDeskTicketRequest ticketRequest;

    public FreshDeskTicketActionTemplate() {
        super(ActionType.FRESH_DESK_TICKET);
    }

    @Builder
    public FreshDeskTicketActionTemplate(final CreateFreshDeskTicketRequest ticketRequest) {
        super(ActionType.FRESH_DESK_TICKET);
        this.ticketRequest = ticketRequest;
    }

    @Override
    public <T> T visit(ActionTemplateVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
