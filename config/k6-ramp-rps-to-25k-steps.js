import http from 'k6/http';

export default function () {
    http.get(__ENV.SERVICE_URL);
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

            // Total duration: 6m
            stages: [
                {target: 5000, duration: '10s'},
                {target: 5000, duration: '50s'},

                {target: 10000, duration: '10s'},
                {target: 10000, duration: '50s'},

                {target: 15000, duration: '10s'},
                {target: 15000, duration: '50s'},

                {target: 20000, duration: '10s'},
                {target: 20000, duration: '50s'},

                {target: 25000, duration: '10s'},
                {target: 25000, duration: '50s'},

                {target: 0, duration: '1m'},
            ],
        },
    },
};
