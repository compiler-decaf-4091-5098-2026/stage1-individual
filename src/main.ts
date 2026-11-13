import * as fs from "fs";
import * as path from "path";
import { Options, USAGE, UsageError, parseArgs } from "./cli";
import { CompileError, formatError } from "./diagnostics";
import { parse } from "./parser";
import { printTree } from "./printer";
import { scan } from "./scanner";
import { Position, formatToken } from "./token";

const EXIT_OK = 0;
const EXIT_ERROR = 1;
const EXIT_USAGE = 2;

class Output {
  private readonly chunks: string[] = [];

  constructor(private readonly file: string | null) {}

  line(text: string): void {
    if (this.file === null) {
      process.stdout.write(text + "\n");
    } else {
      this.chunks.push(text + "\n");
    }
  }

  close(): boolean {
    if (this.file === null) {
      return true;
    }
    try {
      fs.writeFileSync(this.file, this.chunks.join(""));
      return true;
    } catch {
      process.stderr.write(`${this.file}: не удалось записать файл\n`);
      return false;
    }
  }
}

function reportError(name: string, error: CompileError): void {
  process.stderr.write(formatError(name, error) + "\n");
}

function comesBefore(a: Position, b: Position): boolean {
  return a.line < b.line || (a.line === b.line && a.column < b.column);
}

function compile(options: Options): number {
  const name = path.basename(options.input);
  let source: string;
  try {
    source = fs.readFileSync(options.input, "utf8");
  } catch {
    process.stderr.write(`${options.input}: не удалось прочитать файл\n`);
    return EXIT_ERROR;
  }

  const out = new Output(options.output);
  const { tokens, errors } = scan(source);

  if (options.target === "scan") {
    const pending = [...errors];
    for (const token of tokens.slice(0, -1)) {
      while (pending.length > 0 && comesBefore(pending[0].pos, token.pos)) {
        reportError(name, pending.shift()!);
      }
      out.line(formatToken(token));
    }
    pending.forEach((error) => reportError(name, error));
    const written = out.close();
    return errors.length > 0 || !written ? EXIT_ERROR : EXIT_OK;
  }

  if (errors.length > 0) {
    errors.forEach((error) => reportError(name, error));
    return EXIT_ERROR;
  }

  let program;
  try {
    program = parse(tokens);
  } catch (error) {
    if (error instanceof CompileError) {
      reportError(name, error);
      return EXIT_ERROR;
    }
    throw error;
  }

  if (options.target !== "parse") {
    process.stderr.write(`этап '${options.target}' ещё не реализован\n`);
    return EXIT_ERROR;
  }

  if (options.debug) {
    out.line(printTree(program));
  }
  return out.close() ? EXIT_OK : EXIT_ERROR;
}

function main(argv: string[]): number {
  let options: Options;
  try {
    options = parseArgs(argv);
  } catch (error) {
    if (error instanceof UsageError) {
      process.stderr.write(`ошибка: ${error.message}\n${USAGE}\n`);
      return EXIT_USAGE;
    }
    throw error;
  }
  return compile(options);
}

process.exitCode = main(process.argv.slice(2));
