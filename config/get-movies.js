import http from 'k6/http';

const url = __ENV.SERVICE_API_BASE_URL + "/movies?directorLastName=Allen&approach=" + __ENV.APPROACH + "&delayCallDepth=" + __ENV.DELAY_CALL_DEPTH + "&delayInMillis=" + __ENV.DELAY_IN_MILLIS

export default function () {
    http.get(url);
}

export const options = {
    discardResponseBodies: true,
    rps: __ENV.RPS,
    vus: __ENV.VUS,
    duration: __ENV.DURATION_IN_SECONDS + "s",
};
