import http from 'k6/http';

const url = __ENV.SERVICE_API_BASE_URL + "/movies?directorLastName=Allen&delayCallDepth=" + __ENV.DELAY_CALL_DEPTH + "&delayInMillis=" + __ENV.DELAY_IN_MILLIS
const rps = __ENV.RPS ? parseInt(__ENV.RPS, 10) : undefined

export default function () {
    http.get(url);
}

export const options = {
    discardResponseBodies: true,
    // Only cap the request rate when a positive RPS is configured; an empty RPS means "no cap".
    ...(rps ? {rps} : {}),
    vus: __ENV.VUS,
    duration: __ENV.DURATION_IN_SECONDS + "s",
};
