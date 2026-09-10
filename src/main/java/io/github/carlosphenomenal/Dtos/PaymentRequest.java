package io.github.carlosphenomenal.Dtos;

import io.github.carlosphenomenal.Enums.PartyIdType;
import lombok.*;

@Getter
@Builder
public class PaymentRequest {

    private String amount;
    private String externalId;
    private Payer payer;
    private String payerMessage;
    private String payeeNote;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Payer {
        private PartyIdType partyIdType;
        private String partyId;

    }
}
