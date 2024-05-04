import http from 'k6/http';
import {sleep} from 'k6';

const url = __ENV.SERVICE_API_BASE_URL + "/movies?directorLastName=Allen&delayCallDepth=" + __ENV.DELAY_CALL_DEPTH + "&delayInMillis=" + __ENV.DELAY_IN_MILLIS

const rampUpStepCount = 4
const rampUpStepSeconds = (0.8 * __ENV.DURATION_IN_SECONDS / rampUpStepCount)
const rampUpRiserTime = rampUpStepSeconds * 0.3 + 's'
const rampUpTreadTime = rampUpStepSeconds * 0.7 + 's'
const rampDownTime = 0.2 * __ENV.DURATION_IN_SECONDS + 's'
const vuStep = __ENV.VUS / rampUpStepCount

export function setup() {
    console.info("K6 config: rampUpRiserTime=" + rampUpRiserTime + ", rampUpTreadTime=" + rampUpTreadTime + ", rampDownTime=" + rampDownTime)
}

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
                {target: vuStep, duration: rampUpRiserTime},
                {target: vuStep, duration: rampUpTreadTime},

                {target: 2 * vuStep, duration: rampUpRiserTime},
                {target: 2 * vuStep, duration: rampUpTreadTime},

                {target: 3 * vuStep, duration: rampUpRiserTime},
                {target: 3 * vuStep, duration: rampUpTreadTime},

                {target: 4 * vuStep, duration: rampUpRiserTime},
                {target: 4 * vuStep, duration: rampUpTreadTime},

                {target: 0, duration: rampDownTime},
            ],
        },
    },
};
