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
            preAllocatedVUs: 30000,
            gracefulStop: '3s',

            // Total duration: 6m = 360s
            stages: [
                {target: 25000, duration: '5m'},
                {target: 0, duration: '1m'},
            ],
        },
    },
};
