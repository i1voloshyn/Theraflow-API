package dev.ivanvoloshyn.theraflowapi.practicelocation;

import dev.ivanvoloshyn.theraflowapi.practicelocation.dto.PracticeLocationRequest;
import dev.ivanvoloshyn.theraflowapi.practicelocation.dto.PracticeLocationResponse;
import dev.ivanvoloshyn.theraflowapi.practicelocation.model.PracticeLocation;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface PracticeLocationMapper {

    PracticeLocation toPracticeLocation(PracticeLocationRequest request);

    PracticeLocationResponse toResponse(PracticeLocation location);
}
