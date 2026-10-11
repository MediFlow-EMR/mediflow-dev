import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { applyTheme, readTheme, saveTheme } from '../src/utils/theme.js';

afterEach(() => {
    delete globalThis.localStorage;
    delete globalThis.document;
    delete globalThis.window;
});

test('theme defaults to light and only restores a supported choice', () => {
    assert.equal(readTheme(), 'light');
    for (const [stored, expected] of [[null, 'light'], ['dark', 'dark'], ['light', 'light'], ['invalid', 'light']]) {
        globalThis.localStorage = { getItem: () => stored };
        assert.equal(readTheme(), expected);
    }
});

test('switching updates the document, browser chrome, persistence and listeners', () => {
    let stored, chrome, event;
    globalThis.document = { documentElement: { dataset: {} }, querySelector: () => ({ setAttribute: (name, value) => { chrome = value; } }) };
    globalThis.localStorage = { setItem: (key, value) => { stored = [key, value]; } };
    globalThis.window = { dispatchEvent: value => { event = value.type; } };
    saveTheme('dark');
    assert.equal(document.documentElement.dataset.theme, 'dark');
    assert.equal(chrome, '#131b26');
    assert.deepEqual(stored, ['mediflow-theme', 'dark']);
    assert.equal(event, 'mediflow-theme-change');
    assert.equal(applyTheme('invalid'), 'light');
});

test('switching still works when storage is unavailable', () => {
    globalThis.document = { documentElement: { dataset: {} }, querySelector: () => null };
    globalThis.localStorage = { getItem: () => { throw new Error('blocked'); }, setItem: () => { throw new Error('blocked'); } };
    globalThis.window = { dispatchEvent: () => {} };
    assert.equal(readTheme(), 'light');
    assert.doesNotThrow(() => saveTheme('dark'));
    assert.equal(document.documentElement.dataset.theme, 'dark');
});

test('both palettes provide the same complete set of color tokens', () => {
    const [light, dark] = readFileSync(new URL('../src/styles/theme.css', import.meta.url), 'utf8').split(':root[data-theme="dark"]');
    const keys = text => [...text.matchAll(/(--(?:surface|ink|line)-[a-f0-9]{6}):/g)].map(match => match[1]).sort();
    assert.deepEqual(keys(light), keys(dark));
    assert.ok(keys(light).length > 50);
});
