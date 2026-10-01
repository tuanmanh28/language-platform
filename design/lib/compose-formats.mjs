import {
  capitalize,
  GENERATED_NOTICE,
  groupByMode,
  lookUpFontWeight,
  parseDimension,
  parseDurationMillis,
  requireSystemFont,
  toCamelCase,
  toUpperSnakeCase,
  typeNameOf,
} from './values.mjs';

const FONT_WEIGHTS = { 400: 'Normal', 500: 'Medium', 600: 'SemiBold', 700: 'Bold' };

export const composeFormats = {
  'compose/colors': ({ dictionary, file, options }) => {
    const typeName = typeNameOf(file.destination);
    const modes = groupByMode(dictionary.allTokens);
    const [firstMode] = modes.values();
    return kotlinFile(options.packageName, ['androidx.compose.ui.graphics.Color'], [
      `internal data class ${typeName}(`,
      ...firstMode.map(({ name }) => `  val ${name}: Color,`),
      ')',
      ...[...modes].flatMap(([mode, colors]) => [
        '',
        `internal val ${capitalize(mode)}${typeName} =`,
        `  ${typeName}(`,
        ...colors.map(({ name, value }) => `    ${name} = Color(0xFF${value.slice(1).toUpperCase()}),`),
        '  )',
      ]),
    ]);
  },

  'compose/typography': ({ dictionary, file, options }) => {
    const imports = [
      'androidx.compose.ui.text.TextStyle',
      'androidx.compose.ui.text.font.FontFamily',
      'androidx.compose.ui.text.font.FontWeight',
      'androidx.compose.ui.unit.sp',
    ];
    const styles = dictionary.allTokens.flatMap((token, index) => {
      const { fontFamily, fontWeight, fontSize, lineHeight, letterSpacing } = token.$value;
      requireSystemFont(fontFamily);
      return [
        ...(index === 0 ? [] : ['']),
        `  val ${toCamelCase(token.path.slice(1))} =`,
        '    TextStyle(',
        '      fontFamily = FontFamily.Default,',
        `      fontWeight = FontWeight.${lookUpFontWeight(FONT_WEIGHTS, fontWeight)},`,
        `      fontSize = ${sp(fontSize)},`,
        `      lineHeight = ${sp(lineHeight)},`,
        `      letterSpacing = ${sp(letterSpacing)},`,
        '    )',
      ];
    });
    return kotlinFile(options.packageName, imports, [`internal object ${typeNameOf(file.destination)} {`, ...styles, '}']);
  },

  'compose/dimensions': ({ dictionary, file, options }) =>
    kotlinFile(options.packageName, ['androidx.compose.ui.unit.dp'], [
      `internal object ${typeNameOf(file.destination)} {`,
      ...dictionary.allTokens.map(
        (token) => `  val ${toCamelCase(token.path.slice(1))} = ${kotlinNumber(parseDimension(token.$value))}.dp`,
      ),
      '}',
    ]),

  'compose/durations': ({ dictionary, file, options }) =>
    kotlinFile(options.packageName, [], [
      `internal object ${typeNameOf(file.destination)} {`,
      ...dictionary.allTokens.map(
        (token) => `  const val ${toUpperSnakeCase(token.path.slice(1))}_MILLIS = ${parseDurationMillis(token.$value)}`,
      ),
      '}',
    ]),
};

function kotlinFile(packageName, imports, body) {
  const importLines = imports.length === 0 ? [] : [...imports.map((name) => `import ${name}`), ''];
  return [`// ${GENERATED_NOTICE}`, `package ${packageName}`, '', ...importLines, ...body, ''].join('\n');
}

function sp(dimension) {
  return `${kotlinNumber(parseDimension(dimension))}.sp`;
}

function kotlinNumber(value) {
  return value < 0 ? `(${value})` : `${value}`;
}
