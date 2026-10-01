import assert from 'node:assert/strict';
import { describe, test } from 'node:test';
import { contrastRatio } from '../lib/contrast.mjs';
import {
  parseDimension,
  parseDurationMillis,
  parseHexColor,
  toCamelCase,
  toUpperSnakeCase,
  typeNameOf,
} from '../lib/values.mjs';

describe('values', () => {
  test('parseDimension reads px values including negatives and decimals', () => {
    assert.equal(parseDimension('16px'), 16);
    assert.equal(parseDimension('-0.25px'), -0.25);
  });

  test('parseDimension rejects other units', () => {
    assert.throws(() => parseDimension('1rem'), /Unsupported dimension/);
  });

  test('parseDurationMillis reads ms values and rejects seconds', () => {
    assert.equal(parseDurationMillis('150ms'), 150);
    assert.throws(() => parseDurationMillis('0.15s'), /Unsupported duration/);
  });

  test('parseHexColor splits channels and rejects short hex', () => {
    assert.deepEqual(parseHexColor('#7543BA'), { red: 0x75, green: 0x43, blue: 0xba });
    assert.throws(() => parseHexColor('#FFF'), /Unsupported color/);
  });

  test('names convert to camelCase and UPPER_SNAKE_CASE', () => {
    assert.equal(toCamelCase(['surface', 'containerHigh']), 'surfaceContainerHigh');
    assert.equal(toUpperSnakeCase(['duration', 'short']), 'DURATION_SHORT');
    assert.equal(toUpperSnakeCase(['onSurface']), 'ON_SURFACE');
  });

  test('typeNameOf strips the file extension', () => {
    assert.equal(typeNameOf('SpacingTokens.kt'), 'SpacingTokens');
  });
});

describe('contrastRatio', () => {
  test('black on white is 21:1 regardless of order', () => {
    assert.equal(contrastRatio('#000000', '#FFFFFF'), 21);
    assert.equal(contrastRatio('#FFFFFF', '#000000'), 21);
  });

  test('identical colors are 1:1', () => {
    assert.equal(contrastRatio('#7543BA', '#7543BA'), 1);
  });
});
