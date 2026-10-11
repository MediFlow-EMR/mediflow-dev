import { test } from 'node:test';
import assert from 'node:assert/strict';
import { JSDOM } from 'jsdom';
const window = new JSDOM('').window;
globalThis.window = window;
const { sanitizeClinicalHtml } = await import('../src/utils/clinicalHtml.js');

test('clinical HTML strips executable attributes, active content and external resources', () => {
    const html = sanitizeClinicalHtml('<script>alert(1)</script><img src="https://example.invalid/image" onerror="alert(1)"><svg onload="alert(1)"></svg><a href="javascript:alert(1)">관찰</a><p onclick="alert(1)" style="background-image:url(https://example.invalid/x);position:fixed;color:red">기록</p>');
    const container = window.document.createElement('div'); container.innerHTML = html;
    assert.equal(container.querySelector('script,img,svg,a'), null);
    assert.equal(container.querySelector('p').hasAttribute('onclick'), false);
    assert.equal(container.querySelector('p').style.backgroundImage, '');
    assert.equal(container.querySelector('p').style.position, '');
    assert.equal(container.textContent, '관찰기록');
});
test('clinical emphasis, line breaks and permitted colors survive sanitization', () => {
    const html = sanitizeClinicalHtml('<p><b>중요</b><br><span style="color:rgb(255,0,0);background-color:rgb(255,245,157)">관찰</span></p>');
    const container = window.document.createElement('div'); container.innerHTML = html;
    assert.equal(container.querySelector('b').textContent, '중요');
    assert.ok(container.querySelector('br'));
    assert.equal(container.querySelector('span').style.color, 'rgb(255, 0, 0)');
    assert.equal(container.querySelector('span').style.backgroundColor, 'rgb(255, 245, 157)');
});
