import assert from 'node:assert/strict';
import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, test } from 'node:test';
import { resolveReferences } from 'style-dictionary/utils';
import { contrastRatio } from '../lib/contrast.mjs';
import { capitalize, parseDimension } from '../lib/values.mjs';

const tokensDir = join(import.meta.dirname, '../tokens');
const tokens = readdirSync(tokensDir)
  .map((file) => JSON.parse(readFileSync(join(tokensDir, file), 'utf8')))
  .reduce(mergeGroups, {});
const MODES = ['light', 'dark'];
const BODY_TEXT = 4.5;
const UI_COMPONENT = 3;

const textPairs = [
  ['onPrimary', 'primary'],
  ['onPrimaryContainer', 'primaryContainer'],
  ['onSecondary', 'secondary'],
  ['onSecondaryContainer', 'secondaryContainer'],
  ['onTertiary', 'tertiary'],
  ['onTertiaryContainer', 'tertiaryContainer'],
  ['onError', 'error'],
  ['onErrorContainer', 'errorContainer'],
  ['onSuccess', 'success'],
  ['onSuccessContainer', 'successContainer'],
  ['onWarning', 'warning'],
  ['onWarningContainer', 'warningContainer'],
  ...['primary', 'secondary', 'tertiary'].flatMap((role) => {
    const fixed = `${role}Fixed`;
    const onFixed = `on${capitalize(role)}Fixed`;
    return [fixed, `${fixed}Dim`].flatMap((background) => [
      [onFixed, background],
      [`${onFixed}Variant`, background],
    ]);
  }),
  ['onBackground', 'background'],
  ['inverseOnSurface', 'inverseSurface'],
  ['inversePrimary', 'inverseSurface'],
  ['onSurfaceVariant', 'surfaceVariant'],
  ...['onSurface', 'onSurfaceVariant', 'primary', 'secondary', 'tertiary', 'error', 'correct', 'wrong', 'warning'].flatMap(
    (foreground) =>
      [
        'surface',
        'surfaceDim',
        'surfaceBright',
        'surfaceContainerLowest',
        'surfaceContainerLow',
        'surfaceContainer',
        'surfaceContainerHigh',
        'surfaceContainerHighest',
      ].map((background) => [foreground, background]),
  ),
];

function mergeGroups(target, source) {
  for (const [key, value] of Object.entries(source)) {
    target[key] = typeof value === 'object' && key in target ? mergeGroups(target[key], value) : value;
  }
  return target;
}

function color(mode, name) {
  const token = tokens.color[mode][name];
  assert.ok(token, `color.${mode}.${name} is missing`);
  return resolveReferences(token.$value, tokens, { usesDtcg: true });
}

function semanticNames(mode) {
  return Object.keys(tokens.color[mode]).filter((key) => !key.startsWith('$'));
}

describe('color tokens', () => {
  test('light and dark define the same semantic colors', () => {
    assert.deepEqual(semanticNames('dark'), semanticNames('light'));
  });

  for (const mode of MODES) {
    test(`${mode} text pairs meet WCAG AA for body text`, () => {
      for (const [foreground, background] of textPairs) {
        const ratio = contrastRatio(color(mode, foreground), color(mode, background));
        assert.ok(ratio >= BODY_TEXT, `${mode}: ${foreground} on ${background} is ${ratio.toFixed(2)}:1`);
      }
    });

    test(`${mode} outline is visible against the surface`, () => {
      const ratio = contrastRatio(color(mode, 'outline'), color(mode, 'surface'));
      assert.ok(ratio >= UI_COMPONENT, `${mode}: outline on surface is ${ratio.toFixed(2)}:1`);
    });
  }
});

describe('dimension tokens', () => {
  test('spacing follows the 4-pt grid', () => {
    for (const [name, token] of Object.entries(tokens.spacing).filter(([key]) => !key.startsWith('$'))) {
      assert.equal(parseDimension(token.$value) % 4, 0, `spacing.${name} is not a multiple of 4`);
    }
  });
});
