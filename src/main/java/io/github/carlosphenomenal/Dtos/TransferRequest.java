package io.github.carlosphenomenal.Dtos;

import io.github.carlosphenomenal.Enums.PartyIdType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents a transfer request to a payee.
 *
 * @author Carlos Amanya
 */
@Getter
@Builder
public class TransferRequest {
    private String amount;
    private String externalId;
    private Payee payee;
    private String payerMessage;
    private String payeeNote;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Payee {
        private PartyIdType partyIdType;
        private String partyId;
    }
}
