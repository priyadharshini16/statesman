package io.appform.statesman.engine.action.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.appform.statesman.engine.action.BaseAction;
import io.appform.statesman.engine.commands.FreshDeskCommands;
import io.appform.statesman.engine.handlebars.HandleBarsService;
import io.appform.statesman.model.ActionImplementation;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.action.ActionType;
import io.appform.statesman.model.action.freshdeskticket.CreateFDTicketWithAttachmentRequest;
import io.appform.statesman.model.action.freshdeskticket.CreateSimpleFDTicketRequest;
import io.appform.statesman.model.action.freshdeskticket.FreshDeskTicketType;
import io.appform.statesman.model.action.template.FreshDeskTicketActionTemplate;
import io.appform.statesman.publisher.EventPublisher;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.stream.Collectors;

@Slf4j
@Data
@Singleton
@ActionImplementation(name = "FRESH_DESK_TICKET")
public class FreshDeskTicketAction extends BaseAction<FreshDeskTicketActionTemplate> {

    private HandleBarsService handleBarsService;
    private FreshDeskCommands freshDeskCommands;

    @Inject
    public FreshDeskTicketAction(HandleBarsService handleBarsService,
                                 FreshDeskCommands freshDeskCommands,
                                 @Named("eventPublisher") final EventPublisher publisher,
                                 ObjectMapper mapper) {
        super(publisher, mapper);
        this.handleBarsService = handleBarsService;
        this.freshDeskCommands = freshDeskCommands;
    }

    @Override
    public ActionType getType() {
        return ActionType.FRESH_DESK_TICKET;
    }

    @Override
    protected JsonNode execute(FreshDeskTicketActionTemplate actionTemplate, Workflow workflow) {
        log.debug("Fresh Desk ticket Action triggered with Template: {} and Workflow: {}",
                actionTemplate, workflow);

        return actionTemplate.getTicketRequest().getTicketType().visit(new FreshDeskTicketType.FreshDeskTicketTypeVisitor<JsonNode>() {
            @Override
            public JsonNode visitSimpleTicket() {
                val ticketRequest = (CreateSimpleFDTicketRequest) actionTemplate.getTicketRequest();
                JsonNode jsonNode = mapper.valueToTree(workflow);
                val payload = handleBarsService.transform(ticketRequest.getPayload(), jsonNode);
                ticketRequest.setPayload(payload);
                val id = freshDeskCommands.createTicket(ticketRequest);
                ObjectNode ticketId = mapper.createObjectNode();
                ticketId.put("ticketId", id);
                ObjectNode freshDeskAction = mapper.createObjectNode();
                freshDeskAction.put("freshDeskActionCall", ticketId);
                return freshDeskAction;
            }

            @Override
            public JsonNode visitTicketWithAttachment() {
                JsonNode jsonNode = mapper.valueToTree(workflow);
                val ticketRequest = (CreateFDTicketWithAttachmentRequest) actionTemplate.getTicketRequest();
                ticketRequest.getFdTicketFormData().forEach(
                        data -> {
                            String transform = handleBarsService.transform(data.getValue(), jsonNode);
                            data.setValue(transform);
                        }
                );
                val id = freshDeskCommands.createTicketWithAttachment(ticketRequest);
                ObjectNode ticketId = mapper.createObjectNode();
                ticketId.put("ticketId", id);
                ObjectNode freshDeskAction = mapper.createObjectNode();
                freshDeskAction.put("freshDeskActionCall", ticketId);
                return freshDeskAction;
            }
        });
    }

}
