import assert from 'node:assert/strict';
import { describe, test } from 'node:test';
import { composeFormats } from '../lib/compose-formats.mjs';
import { swiftFormats } from '../lib/swift-formats.mjs';

const colors = [
  { path: ['color', 'light', 'primary'], $value: '#7543ba' },
  { path: ['color', 'light', 'onPrimary'], $value: '#FFFFFF' },
  { path: ['color', 'dark', 'primary'], $value: '#DBB8FF' },
  { path: ['color', 'dark', 'onPrimary'], $value: '#3D0F84' },
];

const spacing = [{ path: ['spacing', 'lg'], $value: '16px' }];
const durations = [{ path: ['motion', 'duration', 'short'], $value: '150ms' }];

const bodyStyle = {
  fontFamily: 'system-ui',
  fontWeight: 400,
  fontSize: '16px',
  lineHeight: '26px',
  letterSpacing: '-0.25px',
};

function render(format, allTokens, destination) {
  return format({ dictionary: { allTokens }, file: { destination }, options: { packageName: 'test.generated' } });
}

function typography(style) {
  return [{ path: ['typography', 'bodyLarge'], $value: style }];
}

describe('compose formats', () => {
  test('colors become a data class with one instance per mode', () => {
    const output = render(composeFormats['compose/colors'], colors, 'ColorTokens.kt');
    assert.match(output, /^package test\.generated$/m);
    assert.match(output, /internal data class ColorTokens\(\n {2}val primary: Color,\n {2}val onPrimary: Color,\n\)/);
    assert.match(output, /internal val LightColorTokens =\n {2}ColorTokens\(\n {4}primary = Color\(0xFF7543BA\),/);
    assert.match(output, /internal val DarkColorTokens =\n {2}ColorTokens\(\n {4}primary = Color\(0xFFDBB8FF\),/);
  });

  test('typography wraps negative letter spacing and uses the system font', () => {
    const output = render(composeFormats['compose/typography'], typography(bodyStyle), 'TypographyTokens.kt');
    assert.match(output, /fontFamily = FontFamily\.Default,/);
    assert.match(output, /fontWeight = FontWeight\.Normal,/);
    assert.match(output, /lineHeight = 26\.sp,/);
    assert.match(output, /letterSpacing = \(-0\.25\)\.sp,/);
  });

  test('typography rejects unsupported weights and font families', () => {
    const format = composeFormats['compose/typography'];
    assert.throws(() => render(format, typography({ ...bodyStyle, fontWeight: 300 }), 'T.kt'), /font weight 300/);
    assert.throws(() => render(format, typography({ ...bodyStyle, fontFamily: 'Inter' }), 'T.kt'), /font family/);
  });

  test('dimensions and durations become object members', () => {
    const dimensionOutput = render(composeFormats['compose/dimensions'], spacing, 'SpacingTokens.kt');
    assert.match(dimensionOutput, /internal object SpacingTokens \{\n {2}val lg = 16\.dp\n\}/);
    const durationOutput = render(composeFormats['compose/durations'], durations, 'MotionTokens.kt');
    assert.match(durationOutput, /const val DURATION_SHORT_MILLIS = 150/);
  });
});

describe('swift formats', () => {
  test('colors use rounded sRGB channels and no trailing comma', () => {
    const output = render(swiftFormats['swiftui/colors'], colors, 'ColorTokens.swift');
    assert.match(
      output,
      /static let light = ColorTokens\(\n {4}primary: Color\(red: 0\.4588, green: 0\.2627, blue: 0\.7294\),/,
    );
    assert.match(output, /onPrimary: Color\(red: 1, green: 1, blue: 1\)\n {2}\)/);
  });

  test('typography styles are nested in the tokens type', () => {
    const output = render(swiftFormats['swiftui/typography'], typography(bodyStyle), 'TypographyTokens.swift');
    assert.match(output, /enum TypographyTokens \{\n {2}struct Style \{/);
    assert.match(
      output,
      /static let bodyLarge = Style\(size: 16, lineHeight: 26, weight: \.regular, letterSpacing: -0\.25\)/,
    );
  });

  test('durations are expressed in seconds', () => {
    const output = render(swiftFormats['swiftui/durations'], durations, 'MotionTokens.swift');
    assert.match(output, /static let durationShort: TimeInterval = 0\.15/);
  });
});
