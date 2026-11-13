export type Target = "scan" | "parse" | "inter" | "assembly";

export const TARGETS: readonly Target[] = ["scan", "parse", "inter", "assembly"];
export const DEFAULT_TARGET: Target = "parse";

export interface Options {
  target: Target;
  output: string | null;
  optimizations: string[];
  debug: boolean;
  input: string;
}

export class UsageError extends Error {}

function isTarget(value: string): value is Target {
  return (TARGETS as readonly string[]).includes(value);
}

export const USAGE = [
  "Использование: run.sh [параметры] <файл>",
  "  -t, --target <stage>        scan | parse | inter | assembly (по умолчанию parse)",
  "  -o, --output <outname>      записать результат в файл",
  "  -O, --opt <opt,...>         список оптимизаций (all, -<opt>)",
  "  -d, --debug                 вывести отладочную информацию",
].join("\n");

function takeValue(args: string[], i: number, flag: string): string {
  const value = args[i + 1];
  if (value === undefined) {
    throw new UsageError(`параметр ${flag} требует значения`);
  }
  return value;
}

export function parseArgs(args: string[]): Options {
  let target: Target = DEFAULT_TARGET;
  let output: string | null = null;
  let optimizations: string[] = [];
  let debug = false;
  const inputs: string[] = [];

  for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    let flag = arg;
    let inline: string | null = null;
    const eq = arg.indexOf("=");
    if (arg.startsWith("--") && eq > 0) {
      flag = arg.slice(0, eq);
      inline = arg.slice(eq + 1);
    }
    const value = (): string => {
      if (inline !== null) {
        return inline;
      }
      return takeValue(args, i++, flag);
    };
    switch (flag) {
      case "-t":
      case "--target": {
        const raw = value();
        if (!isTarget(raw)) {
          throw new UsageError(`неизвестный этап '${raw}', допустимо: ${TARGETS.join(", ")}`);
        }
        target = raw;
        break;
      }
      case "-o":
      case "--output":
        output = value();
        break;
      case "-O":
      case "--opt":
        optimizations = value()
          .split(",")
          .filter((s) => s.length > 0);
        break;
      case "-d":
      case "--debug":
        debug = true;
        break;
      default:
        if (arg.startsWith("-")) {
          throw new UsageError(`неизвестный параметр '${arg}'`);
        }
        inputs.push(arg);
    }
  }

  if (inputs.length !== 1) {
    throw new UsageError(inputs.length === 0 ? "не указан входной файл" : "должен быть указан ровно один входной файл");
  }
  return { target, output, optimizations, debug, input: inputs[0] };
}
