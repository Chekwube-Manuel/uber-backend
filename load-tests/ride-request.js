import http from 'k6/http';
import { check } from 'k6';
import { Rate } from 'k6/metrics';

const failures = new Rate('ride_request_failures');
const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const riderToken = __ENV.RIDER_TOKEN;
const rate = Number(__ENV.RATE || 10);
const duration = __ENV.DURATION || '30s';

if (!riderToken) {
  throw new Error('Set RIDER_TOKEN to a dedicated staging rider JWT. This test creates ride records.');
}

export const options = {
  scenarios: {
    ride_request_rate: {
      executor: 'constant-arrival-rate',
      rate,
      timeUnit: '1s',
      duration,
      preAllocatedVUs: Math.max(20, rate),
      maxVUs: Math.max(100, rate * 2),
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
    ride_request_failures: ['rate<0.01'],
  },
};

export default function () {
  const payload = JSON.stringify({
    pickupLatitude: 6.5244,
    pickupLongitude: 3.3792,
    destinationLatitude: 6.5350,
    destinationLongitude: 3.3840,
  });

  const response = http.post(`${baseUrl}/api/v1/rider/rides`, payload, {
    headers: {
      Authorization: `Bearer ${riderToken}`,
      'Content-Type': 'application/json',
    },
    tags: { endpoint: 'create_ride' },
  });

  const ok = check(response, {
    'ride created': (r) => r.status === 200 || r.status === 201,
  });
  failures.add(!ok);
}
