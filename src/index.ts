import * as fs from "node:fs";
import { scanSource } from "./scanner";
import { Parser } from "./parser";

function usage(): never {
  console.error("Использование: decaf-stage1 -t <scan|parse> <файл.decaf>");
  process.exit(2);
}

function main(): void {
  const args = process.argv.slice(2);
  let target: string | undefined;
  let filename: string | undefined;
  for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    if (arg === "-t" || arg === "--target") {
      target = args[++i];
      if (!target) usage();
    } else if (arg.startsWith("-")) {
      console.error(`Неизвестный параметр: ${arg}`);
      usage();
    } else if (!filename) filename = arg;
    else { console.error("Нужно указать ровно один входной файл."); usage(); }
  }
  if (!target || !["scan", "parse"].includes(target) || !filename) usage();

  let source: string;
  try { source = fs.readFileSync(filename, "utf8"); }
  catch (error) {
    console.error(`${filename}: не удалось прочитать файл: ${(error as Error).message}`);
    process.exitCode = 1; return;
  }

  const result = scanSource(source, filename);
  for (const error of result.errors) {
    console.error(`${filename}:${error.line}:${error.column}: лексическая ошибка: ${error.message}`);
  }

  if (target === "scan") {
    for (const token of result.tokens) {
      const hasType = ["CHARLITERAL", "INTLITERAL", "LONGLITERAL", "BOOLEANLITERAL", "STRINGLITERAL", "IDENTIFIER"].includes(token.kind);
      console.log(hasType ? `${token.line} ${token.kind} ${token.text}` : `${token.line} ${token.text}`);
    }
    if (result.errors.length) process.exitCode = 1;
    return;
  }

  if (result.errors.length) { process.exitCode = 1; return; }
  try { new Parser(result.tokens, filename).parseProgram(); }
  catch (error) {
    console.error((error as Error).message);
    process.exitCode = 1;
  }
}

main();
