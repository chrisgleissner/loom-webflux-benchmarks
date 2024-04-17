import http from 'k6/http';
import {sleep} from 'k6';

export default function () {
    http.get(__ENV.SERVICE_URL);
    sleep(1.8);
}

export const options = {
    discardResponseBodies: true,
    scenarios: {
        contacts: {
            executor: 'ramping-arrival-rate',
            startRate: 0,
            timeUnit: '1s',
            preAllocatedVUs: 10000,
            gracefulStop: '1s',

            stages: [
                {target: 1000, duration: '5s'},
                {target: 0, duration: '5s'},
            ],
        },
    },
};
