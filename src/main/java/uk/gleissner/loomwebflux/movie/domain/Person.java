package uk.gleissner.loomwebflux.movie.domain;

import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import lombok.val;

import java.time.LocalDate;

@Value
@Builder
@Jacksonized
public class Person {
    String firstName;
    String lastName;
    LocalDate birthday;

    public static Person of(String fullName) {
        return of(fullName, null);
    }

    public static Person of(String fullName, @Nullable LocalDate birthday) {
        val lastSpaceIdx = fullName.lastIndexOf(' ');
        return Person.builder()
                .firstName(fullName.substring(0, lastSpaceIdx).trim())
                .lastName(fullName.substring(lastSpaceIdx + 1))
                .birthday(birthday)
                .build();
    }
}
