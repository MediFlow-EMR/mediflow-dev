import { test } from 'node:test';
import assert from 'node:assert/strict';
import { AxiosError } from 'axios';
import { createApiClient } from '../src/services/createApiClient.js';
const ok = config => ({ data: {}, status: 200, statusText: 'OK', headers: {}, config });
const unauthorized = config => Promise.reject(new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, null, {status:401, data:{}, config}));

test('simultaneous expired requests share a refresh and each retry only once', async () => {
    let refreshes = 0, retries = 0, expires = 0;
    const client = createApiClient({ baseURL:'/api', onAuthExpired:()=>expires++ });
    let release;
    const barrier = new Promise(resolve => { release = resolve; });
    client.defaults.adapter = async config => {
        if (config.url === '/auth/refresh') { refreshes++; await barrier; return ok(config); }
        if (!config._retry) return unauthorized(config);
        retries++; return ok(config);
    };
    const pending = Promise.all([client.get('/records/1'),client.get('/records/2')]);
    await new Promise(resolve => setTimeout(resolve, 10));
    release(); await pending;
    assert.equal(refreshes, 1); assert.equal(retries, 2); assert.equal(expires, 0);
});
test('failed refresh expires authentication once and does not retry mutations', async () => {
    let refreshes = 0, writes = 0, expires = 0;
    const client = createApiClient({ baseURL:'/api', onAuthExpired:()=>expires++ });
    client.defaults.adapter = async config => {
        if (config.url === '/auth/refresh') { refreshes++; await new Promise(resolve => setTimeout(resolve, 10)); return unauthorized(config); }
        writes++; return unauthorized(config);
    };
    const results = await Promise.allSettled([client.post('/records',{}),client.post('/records',{})]);
    assert.ok(results.every(r=>r.status==='rejected'));
    assert.equal(refreshes,1); assert.equal(writes,2); assert.equal(expires,1);
});
test('login errors never refresh; a retried protected 401 stops', async () => {
    let expires = 0, refreshes = 0;
    const client = createApiClient({ baseURL:'/api', onAuthExpired:()=>expires++ });
    client.defaults.adapter = async config => {
        if (config.url === '/auth/refresh') { refreshes++; return ok(config); }
        return unauthorized(config);
    };
    await assert.rejects(client.post('/auth/email/login',{}));
    assert.equal(refreshes,0); assert.equal(expires,0);
    await assert.rejects(client.get('/records'));
    assert.equal(refreshes,1); assert.equal(expires,1);
});
test('timeouts preserve failures without replaying a write', async () => {
    let calls=0;
    const client=createApiClient({baseURL:'/api',onAuthExpired:()=>assert.fail('must not expire')});
    client.defaults.adapter=async config=>{ calls++; throw new AxiosError('timeout','ECONNABORTED',config); };
    await assert.rejects(client.post('/records',{}));
    assert.equal(calls,1);
});
