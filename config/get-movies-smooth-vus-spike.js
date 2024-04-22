import http from 'k6/http';
import {sleep} from 'k6';

const url = __ENV.SERVICE_API_BASE_URL + "/movies?directorLastName=Allen&delayInMillis=" + __ENV.DELAY_IN_MILLIS
const rampUpTime = 0.8 * __ENV.DURATION_IN_SECONDS + 's'
const rampDownTime = 0.2 * __ENV.DURATION_IN_SECONDS + 's'

export default function () {
    http.get(url)
    sleep(2 * Math.random() + 1); // between 1s and 3s
}
export const options = {
    discardResponseBodies: true,
    scenarios: {
        contacts: {
            executor: 'ramping-vus',
            startVUs: 0,
            gracefulStop: '2s',
            stages: [
                {target: __ENV.VUS, duration: rampUpTime},
                {target: 0, duration: rampDownTime},
            ],
        },
    },
};
