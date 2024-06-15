package uk.gleissner.loomwebflux.movie.domain;

import jakarta.annotation.Nullable;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.val;

import java.time.LocalDate;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Cacheable
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Person {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @EqualsAndHashCode.Exclude
    Long id;

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
