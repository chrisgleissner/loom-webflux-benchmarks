package uk.gleissner.loomwebflux.movie.domain;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Cacheable
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Award {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @EqualsAndHashCode.Exclude
    Long id;

    String name;
    int year;
}