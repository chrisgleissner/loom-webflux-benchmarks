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
            executor: 'ramping-vus',
            startVUs: 0,
            gracefulStop: '3s',

            // Total duration: 6m = 360s
            stages: [
                {target: 25000, duration: '5m'},
                {target: 0, duration: '1m'},
            ],
        },
    },
};
