package io.github.carlosphenomenal.Dtos;

import io.github.carlosphenomenal.Enums.PartyIdType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Internal representation of the transfer body sent to the MoMo API.
 *
 * @author Carlos Amanya
 */
@Getter
@Builder
public class TransferBody {
    private String amount;
    private String currency;
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
