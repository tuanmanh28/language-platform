import { join } from 'node:path';
import StyleDictionary from 'style-dictionary';
import { composeFormats } from './lib/compose-formats.mjs';
import { swiftFormats } from './lib/swift-formats.mjs';

const root = import.meta.dirname;
const inGroup = (group) => (token) => token.path[0] === group;

const composeFiles = [
  { destination: 'ColorTokens.kt', format: 'compose/colors', filter: inGroup('color') },
  { destination: 'TypographyTokens.kt', format: 'compose/typography', filter: inGroup('typography') },
  { destination: 'SpacingTokens.kt', format: 'compose/dimensions', filter: inGroup('spacing') },
  { destination: 'RadiusTokens.kt', format: 'compose/dimensions', filter: inGroup('radius') },
  { destination: 'ElevationTokens.kt', format: 'compose/dimensions', filter: inGroup('elevation') },
  { destination: 'SizeTokens.kt', format: 'compose/dimensions', filter: inGroup('size') },
  { destination: 'BorderTokens.kt', format: 'compose/dimensions', filter: inGroup('border') },
  { destination: 'MotionTokens.kt', format: 'compose/durations', filter: inGroup('motion') },
];

const swiftFiles = [
  { destination: 'ColorTokens.swift', format: 'swiftui/colors', filter: inGroup('color') },
  { destination: 'TypographyTokens.swift', format: 'swiftui/typography', filter: inGroup('typography') },
  { destination: 'SpacingTokens.swift', format: 'swiftui/dimensions', filter: inGroup('spacing') },
  { destination: 'RadiusTokens.swift', format: 'swiftui/dimensions', filter: inGroup('radius') },
  { destination: 'ElevationTokens.swift', format: 'swiftui/dimensions', filter: inGroup('elevation') },
  { destination: 'SizeTokens.swift', format: 'swiftui/dimensions', filter: inGroup('size') },
  { destination: 'BorderTokens.swift', format: 'swiftui/dimensions', filter: inGroup('border') },
  { destination: 'MotionTokens.swift', format: 'swiftui/durations', filter: inGroup('motion') },
];

const dictionary = new StyleDictionary({
  source: [join(root, 'tokens/*.json')],
  hooks: { formats: { ...composeFormats, ...swiftFormats } },
  platforms: {
    compose: {
      transforms: ['name/camel'],
      buildPath: join(root, '../ui-compose/src/commonMain/kotlin/com/app/platform/language/ui/theme/generated/'),
      options: { packageName: 'com.app.platform.language.ui.theme.generated' },
      files: composeFiles,
    },
    swiftui: {
      transforms: ['name/camel'],
      buildPath: join(root, '../app-apple/Sources/DesignSystem/Generated/'),
      files: swiftFiles,
    },
    css: {
      transformGroup: 'css',
      expand: { include: ['typography'] },
      buildPath: join(root, 'build/css/'),
      files: [{ destination: 'tokens.css', format: 'css/variables', filter: (token) => token.path[0] !== 'palette' }],
    },
  },
});

await dictionary.buildAllPlatforms();
