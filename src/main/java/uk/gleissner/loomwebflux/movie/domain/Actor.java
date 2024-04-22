package uk.gleissner.loomwebflux.movie.domain;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class Actor {
    Person person;
    String role;
}
