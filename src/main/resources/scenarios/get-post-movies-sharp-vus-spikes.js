import http from 'k6/http';
import {sleep} from 'k6';

const getMovies1Url = getMoviesUrl("Allen")
const getMovies2Url = getMoviesUrl("Kubrick")
const getMovies3Url = getMoviesUrl("Hitchcock")
const postMoviesUrl = __ENV.SERVICE_API_BASE_URL + "/movies?delayCallDepth=" + __ENV.DELAY_CALL_DEPTH + "&delayInMillis=" + __ENV.DELAY_IN_MILLIS
const moviesJson = JSON.stringify(JSON.parse(open("./movies.json")));
const stageDuration = 0.025 * __ENV.DURATION_IN_SECONDS + 's'

function getMoviesUrl(directorLastName) {
    return __ENV.SERVICE_API_BASE_URL + "/movies?directorLastName=" + directorLastName + "&delayCallDepth=" + __ENV.DELAY_CALL_DEPTH + "&delayInMillis=" + __ENV.DELAY_IN_MILLIS
}

export function getMovies1() {
    http.get(getMovies1Url)
    sleep(2 * Math.random() + 1); // between 1s and 3s
}

export function getMovies2() {
    http.get(getMovies2Url)
    sleep(2 * Math.random() + 1); // between 1s and 3s
}

export function getMovies3() {
    http.get(getMovies3Url)
    sleep(2 * Math.random() + 1); // between 1s and 3s
}

export function postMovies() {
    http.post(postMoviesUrl, moviesJson, {
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        }
    })
    sleep(2 * Math.random() + 1); // between 1s and 3s
}

export const options = {
    scenarios: {
        getMovies1: {
            executor: 'ramping-vus',
            exec: 'getMovies1',
            startVUs: 0,
            gracefulStop: '2s',
            stages: [
                // Identical for all scenarios
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
            ],
        },
        getMovies2: {
            executor: 'ramping-vus',
            exec: 'getMovies2',
            startVUs: 0,
            gracefulStop: '2s',
            stages: [
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
            ],
        },
        getMovies3: {
            executor: 'ramping-vus',
            exec: 'getMovies3',
            startVUs: 0,
            gracefulStop: '2s',
            stages: [
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
            ],
        },
        postMovies: {
            executor: 'ramping-vus',
            exec: 'postMovies',
            startVUs: 0,
            gracefulStop: '2s',
            stages: [
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: __ENV.VUS / 4, duration: stageDuration},
                {target: 0, duration: stageDuration},
                {target: 0, duration: stageDuration},
            ],
        },
    },
};
