package io.github.carlosphenomenal.Dtos;

import io.github.carlosphenomenal.Enums.PartyIdType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the status of a refund transaction.
 *
 * @author Carlos Amanya
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundStatus {
    private String amount;
    private String currency;
    private String financialTransactionId;
    private String externalId;
    private Payee payee;
    private String payerMessage;
    private String payeeNote;
    private String status;
    private String reason;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payee {
        private PartyIdType partyIdType;
        private String partyId;
    }
}
