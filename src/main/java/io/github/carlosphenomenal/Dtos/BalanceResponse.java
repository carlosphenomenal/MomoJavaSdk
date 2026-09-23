package io.github.carlosphenomenal.Dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response object for account balance requests.
 *
 * @author Carlos Amanya
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceResponse {
    private String availableBalance;
    private String currency;
}
