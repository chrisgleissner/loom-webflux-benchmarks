package uk.gleissner.loomwebflux.movie.domain

import uk.gleissner.loomwebflux.movie.domain.Directors.davidLynch
import java.time.LocalDate

object Movies {

    val mulhollandDrive = Movie.builder()
        .title("Mulholland Drive")
        .releaseYear(2001)
        .characters(
            listOf(
                Character.builder().actor(Person.of("Naomi Watts", LocalDate.of(1968, 9, 28))).name("Betty Elms").build(),
                Character.builder().actor(Person.of("Laura Harring", LocalDate.of(1964, 3, 3))).name("Rita").build()
            )
        )
        .directors(listOf(davidLynch))
        .writers(listOf(davidLynch))
        .awards(
            listOf(
                Award.builder().name("Cannes Film Festival").year(2001).build(),
                Award.builder().name("National Society of Film Critics Award").year(2001).build()
            )
        )
        .genre(Genre.MYSTERY)
        .rating(7.9)
        .build()

    val theStraightStory = Movie.builder()
        .title("The Straight Story")
        .releaseYear(1999)
        .characters(
            listOf(
                Character.builder().actor(Person.of("Richard Farnsworth", LocalDate.of(1920, 9, 1))).name("Alvin Straight")
                    .build(),
                Character.builder().actor(Person.of("Sissy Spacek", LocalDate.of(1949, 12, 25))).name("Rose Straight")
                    .build()
            )
        )
        .directors(listOf(davidLynch))
        .writers(
            listOf(
                Person.of("John Roach", LocalDate.of(1960, 3, 2)),
                Person.of("Mary Sweeney", LocalDate.of(1954, 4, 29))
            )
        )
        .awards(
            listOf(
                Award.builder().name("Cannes Film Festival").year(1999).build(),
                Award.builder().name("Golden Globe Award").year(2000).build()
            )
        )
        .genre(Genre.DRAMA)
        .rating(8.0)
        .build()
}