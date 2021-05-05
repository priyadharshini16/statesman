package io.appform.statesman.model.action.freshdeskticket;

public enum FreshDeskTicketType {
    SIMPLE_TICKET {
        @Override
        public <T> T visit(FreshDeskTicketTypeVisitor<T> visitor) {
            return visitor.visitSimpleTicket();
        }
    },
    TICKET_WITH_ATTACHMENT {
        @Override
        public <T> T visit(FreshDeskTicketTypeVisitor<T> visitor) {
            return visitor.visitTicketWithAttachment();
        }
    }
    ;

    public abstract <T> T visit(FreshDeskTicketType.FreshDeskTicketTypeVisitor<T> visitor);

    public interface FreshDeskTicketTypeVisitor<T> {
        T visitSimpleTicket();

        T visitTicketWithAttachment();
    }
}
