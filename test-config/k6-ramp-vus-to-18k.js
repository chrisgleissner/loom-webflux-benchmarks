import http from 'k6/http';
import { sleep } from 'k6';

export default function() {
  http.get(__ENV.SERVICE_URL);
  sleep(0.8);
}

export const options = {
  discardResponseBodies: true,
  scenarios: {
    contacts: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        // Duration: 8m = 480s
        { duration: '10s', target: 3000 },
        { duration: '50s', target: 3000 },

        { duration: '10s', target: 6000 },
        { duration: '50s', target: 6000 },

        { duration: '10s', target: 9000 },
        { duration: '50s', target: 9000 },

        { duration: '10s', target: 12000 },
        { duration: '50s', target: 12000 },

        { duration: '10s', target: 15000 },
        { duration: '50s', target: 15000 },
        
        { duration: '10s', target: 18000 },
        { duration: '50s', target: 18000 },

        { duration: '10s', target: 9000 },
        { duration: '50s', target: 9000 },

        { duration: '10s', target: 3000 },
        { duration: '50s', target: 3000 },
      ],
      gracefulRampDown: '0s',
    },
  },
};
