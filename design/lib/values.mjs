export const GENERATED_NOTICE = 'Generated from design/tokens by `npm --prefix design run build`. Do not edit.';

const SYSTEM_FONT = 'system-ui';

export function parseDimension(value) {
  const match = /^(-?\d+(?:\.\d+)?)px$/.exec(value);
  if (!match) throw new Error(`Unsupported dimension "${value}", expected a px value`);
  return Number(match[1]);
}

export function parseDurationMillis(value) {
  const match = /^(\d+)ms$/.exec(value);
  if (!match) throw new Error(`Unsupported duration "${value}", expected a ms value`);
  return Number(match[1]);
}

export function parseHexColor(value) {
  const match = /^#([0-9A-F]{6})$/i.exec(value);
  if (!match) throw new Error(`Unsupported color "${value}", expected #RRGGBB`);
  const rgb = parseInt(match[1], 16);
  return { red: (rgb >> 16) & 0xff, green: (rgb >> 8) & 0xff, blue: rgb & 0xff };
}

export function toCamelCase(segments) {
  return segments.map((segment, index) => (index === 0 ? segment : capitalize(segment))).join('');
}

export function toUpperSnakeCase(segments) {
  return segments.map((segment) => segment.replace(/([a-z0-9])([A-Z])/g, '$1_$2').toUpperCase()).join('_');
}

export function groupByMode(tokens) {
  const modes = new Map();
  for (const token of tokens) {
    const [, mode, ...name] = token.path;
    if (!modes.has(mode)) modes.set(mode, []);
    modes.get(mode).push({ name: toCamelCase(name), value: token.$value });
  }
  return modes;
}

export function lookUpFontWeight(names, weight) {
  const name = names[weight];
  if (!name) throw new Error(`Unsupported font weight ${weight}`);
  return name;
}

export function requireSystemFont(fontFamily) {
  if (fontFamily !== SYSTEM_FONT) throw new Error(`Unsupported font family "${fontFamily}", only ${SYSTEM_FONT} is bundled`);
}

export function typeNameOf(destination) {
  return destination.replace(/\.\w+$/, '');
}

export function capitalize(text) {
  return text.charAt(0).toUpperCase() + text.slice(1);
}
