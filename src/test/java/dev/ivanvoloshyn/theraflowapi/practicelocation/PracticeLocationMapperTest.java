package dev.ivanvoloshyn.theraflowapi.practicelocation;

import dev.ivanvoloshyn.theraflowapi.fixtures.PracticeLocationTestFixtures;
import dev.ivanvoloshyn.theraflowapi.practicelocation.dto.PracticeLocationRequest;
import dev.ivanvoloshyn.theraflowapi.practicelocation.model.PracticeLocation;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

public class PracticeLocationMapperTest {
    PracticeLocationMapper mapper = Mappers.getMapper(PracticeLocationMapper.class);

    @Test
    void fromRequestToModelTest() {
        PracticeLocationRequest request = PracticeLocationTestFixtures.validRequest();
        PracticeLocation expectedModel = PracticeLocationTestFixtures.validModel();

        PracticeLocation actual = mapper.toPracticeLocation(request);

        assertThat(actual.getName()).isEqualTo(expectedModel.getName());
        assertThat(actual.getCity()).isEqualTo(expectedModel.getCity());
        assertThat(actual.getStreet()).isEqualTo(expectedModel.getStreet());
        assertThat(actual.getBuildingNumber()).isEqualTo(expectedModel.getBuildingNumber());
        assertThat(actual.getUnitNumber()).isEqualTo(expectedModel.getUnitNumber());
        assertThat(actual.getPostalCode()).isEqualTo(expectedModel.getPostalCode());
        assertThat(actual.getPhoneNumber()).isEqualTo(expectedModel.getPhoneNumber());
    }
}
