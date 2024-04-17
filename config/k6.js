import http from 'k6/http';

export default function () {
    http.get(__ENV.SERVICE_URL);
}
