package io.github.carlosphenomenal.Dtos;

import lombok.Builder;
import lombok.Getter;

/**
 * Internal representation of the refund body sent to the MoMo API.
 *
 * @author Carlos Amanya
 */
@Getter
@Builder
public class RefundBody {
    private String amount;
    private String currency;
    private String externalId;
    private String payerMessage;
    private String payeeNote;
    private String referenceIdToRefund;
}
