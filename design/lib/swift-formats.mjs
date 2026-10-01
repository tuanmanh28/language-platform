import {
  GENERATED_NOTICE,
  groupByMode,
  lookUpFontWeight,
  parseDimension,
  parseDurationMillis,
  parseHexColor,
  requireSystemFont,
  toCamelCase,
  typeNameOf,
} from './values.mjs';

const FONT_WEIGHTS = { 400: 'regular', 500: 'medium', 600: 'semibold', 700: 'bold' };

export const swiftFormats = {
  'swiftui/colors': ({ dictionary, file }) => {
    const typeName = typeNameOf(file.destination);
    const modes = groupByMode(dictionary.allTokens);
    const [firstMode] = modes.values();
    return swiftFile([
      `struct ${typeName} {`,
      ...firstMode.map(({ name }) => `  let ${name}: Color`),
      ...[...modes].flatMap(([mode, colors]) => [
        '',
        `  static let ${mode} = ${typeName}(`,
        ...colors.map(({ name, value }, index) => `    ${name}: ${swiftColor(value)}${index < colors.length - 1 ? ',' : ''}`),
        '  )',
      ]),
      '}',
    ]);
  },

  'swiftui/typography': ({ dictionary, file }) =>
    swiftFile([
      `enum ${typeNameOf(file.destination)} {`,
      '  struct Style {',
      '    let size: CGFloat',
      '    let lineHeight: CGFloat',
      '    let weight: Font.Weight',
      '    let letterSpacing: CGFloat',
      '  }',
      '',
      ...dictionary.allTokens.map((token) => {
        const { fontFamily, fontSize, lineHeight, fontWeight, letterSpacing } = token.$value;
        requireSystemFont(fontFamily);
        return (
          `  static let ${toCamelCase(token.path.slice(1))} = Style(` +
          `size: ${parseDimension(fontSize)}, lineHeight: ${parseDimension(lineHeight)}, ` +
          `weight: .${lookUpFontWeight(FONT_WEIGHTS, fontWeight)}, letterSpacing: ${parseDimension(letterSpacing)})`
        );
      }),
      '}',
    ]),

  'swiftui/dimensions': ({ dictionary, file }) =>
    swiftFile([
      `enum ${typeNameOf(file.destination)} {`,
      ...dictionary.allTokens.map(
        (token) => `  static let ${toCamelCase(token.path.slice(1))}: CGFloat = ${parseDimension(token.$value)}`,
      ),
      '}',
    ]),

  'swiftui/durations': ({ dictionary, file }) =>
    swiftFile([
      `enum ${typeNameOf(file.destination)} {`,
      ...dictionary.allTokens.map(
        (token) =>
          `  static let ${toCamelCase(token.path.slice(1))}: TimeInterval = ${parseDurationMillis(token.$value) / 1000}`,
      ),
      '}',
    ]),
};

function swiftFile(body) {
  return [`// ${GENERATED_NOTICE}`, 'import SwiftUI', '', ...body, ''].join('\n');
}

function swiftColor(hex) {
  const { red, green, blue } = parseHexColor(hex);
  const channel = (value) => Number((value / 255).toFixed(4));
  return `Color(red: ${channel(red)}, green: ${channel(green)}, blue: ${channel(blue)})`;
}
