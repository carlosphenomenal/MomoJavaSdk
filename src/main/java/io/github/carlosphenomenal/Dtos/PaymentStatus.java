package io.github.carlosphenomenal.Dtos;

import io.github.carlosphenomenal.Enums.PartyIdType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the status of a payment transaction.
 *
 * @author Carlos Amanya
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatus {
    private String financialTransactionId;
    private String externalId;
    private String amount;
    private String currency;
    private Payer payer;
    private String payerMessage;
    private String payeeNote;
    private String status;
    private String reason;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payer {
        private PartyIdType partyIdType;
        private String partyId;
    }
}
