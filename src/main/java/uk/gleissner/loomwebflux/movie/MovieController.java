package uk.gleissner.loomwebflux.movie;

import org.springframework.core.env.Environment;
import uk.gleissner.loomwebflux.controller.LoomWebFluxController;
import uk.gleissner.loomwebflux.movie.repo.MovieRepo;

public abstract class MovieController extends LoomWebFluxController {

    protected static final String API_PATH = "/movies";
    protected final MovieRepo movieRepo;

    MovieController(Environment environment, MovieRepo movieRepo) {
        super(environment);
        this.movieRepo = movieRepo;
    }
}
