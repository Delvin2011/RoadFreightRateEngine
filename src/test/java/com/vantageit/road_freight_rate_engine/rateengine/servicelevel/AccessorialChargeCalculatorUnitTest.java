package com.vantageit.road_freight_rate_engine.rateengine.servicelevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ServiceLevel;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ServiceRequest;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.SurchargeRateRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Mockito unit test (not {@code @SpringBootTest}): proves {@link AccessorialChargeCalculator}
 * never queries {@link SurchargeRateRepository} at all when no accessorial flags are set, rather
 * than just proving the returned list happens to be empty — a repository call that returns
 * "no active rate for this code" would look the same from the outside if the codes being queried
 * were wrong, but this catches that class of bug directly via {@code verifyNoInteractions}.
 */
@ExtendWith(MockitoExtension.class)
class AccessorialChargeCalculatorUnitTest {

    @Mock
    private SurchargeRateRepository surchargeRateRepository;

    @Test
    void unsetFlagsNeverQueryTheRepository() {
        AccessorialChargeCalculator calculator = new AccessorialChargeCalculator(surchargeRateRepository);
        ServiceRequest service = new ServiceRequest(
                ServiceLevel.ECONOMY, LocalDate.of(2025, 7, 15), null, false, false, false, false, false, false, false, false);

        List<LineItem> result = calculator.compute(service, LocalDate.of(2025, 7, 15), "ZAR");

        assertThat(result).isEmpty();
        verifyNoInteractions(surchargeRateRepository);
    }
}
