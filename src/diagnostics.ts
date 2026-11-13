import { Position } from "./token";

export class CompileError extends Error {
  constructor(
    public readonly kind: string,
    public readonly pos: Position,
    message: string,
  ) {
    super(message);
  }
}

export function formatError(file: string, error: CompileError): string {
  return `${file}:${error.pos.line}:${error.pos.column}: ${error.kind}: ${error.message}`;
}
