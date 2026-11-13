import { CompileError } from "./diagnostics";
import { BOOL_LITERALS, KEYWORDS, OPERATORS, PUNCTUATION, Position, Token, TokenKind } from "./token";

const LEXICAL = "лексическая ошибка";
const ESCAPES: ReadonlySet<string> = new Set(['"', "'", "\\", "t", "n", "r", "f"]);

class UnterminatedLiteral extends CompileError {}

export interface ScanResult {
  tokens: Token[];
  errors: CompileError[];
}

function isAlpha(c: string): boolean {
  return (c >= "a" && c <= "z") || (c >= "A" && c <= "Z") || c === "_";
}

function isDigit(c: string): boolean {
  return c >= "0" && c <= "9";
}

function isHexDigit(c: string): boolean {
  return isDigit(c) || (c >= "a" && c <= "f") || (c >= "A" && c <= "F");
}

function isWhitespace(c: string): boolean {
  return c === " " || c === "\t" || c === "\n" || c === "\r" || c === "\f";
}

function isPrintable(c: string): boolean {
  const code = c.charCodeAt(0);
  return code >= 32 && code <= 126;
}

function show(c: string): string {
  if (c === "") {
    return "конец файла";
  }
  if (isPrintable(c)) {
    return `'${c}'`;
  }
  return `символ с кодом ${c.codePointAt(0)}`;
}

function showCodePoint(codePoint: number): string {
  const hex = codePoint.toString(16).toUpperCase().padStart(4, "0");
  if (codePoint >= 32 && codePoint <= 126) {
    return `'${String.fromCodePoint(codePoint)}'`;
  }
  if (codePoint > 126 && codePoint !== 0x7f) {
    return `'${String.fromCodePoint(codePoint)}' (U+${hex})`;
  }
  return `U+${hex}`;
}

export class Scanner {
  private index = 0;
  private line = 1;
  private column = 1;
  private readonly tokens: Token[] = [];
  private readonly errors: CompileError[] = [];

  constructor(private readonly source: string) {}

  scan(): ScanResult {
    while (true) {
      this.skipTrivia();
      if (this.atEnd()) {
        break;
      }
      const start = this.position();
      try {
        this.scanToken(start);
      } catch (error) {
        if (!(error instanceof CompileError)) {
          throw error;
        }
        this.errors.push(error);
      }
    }
    this.tokens.push({ kind: TokenKind.EOF, text: "", pos: this.position() });
    return { tokens: this.tokens, errors: this.errors };
  }

  private atEnd(): boolean {
    return this.index >= this.source.length;
  }

  private peek(offset = 0): string {
    return this.source.charAt(this.index + offset);
  }

  private advance(): string {
    const c = this.source.charAt(this.index++);
    if (c === "\n") {
      this.line++;
      this.column = 1;
    } else {
      this.column++;
    }
    return c;
  }

  private position(): Position {
    return { line: this.line, column: this.column };
  }

  private emit(kind: TokenKind, text: string, pos: Position): void {
    this.tokens.push({ kind, text, pos });
  }

  private fail(pos: Position, message: string): never {
    throw new CompileError(LEXICAL, pos, message);
  }

  private skipTrivia(): void {
    while (!this.atEnd()) {
      const c = this.peek();
      if (isWhitespace(c)) {
        this.advance();
      } else if (c === "/" && this.peek(1) === "/") {
        while (!this.atEnd() && this.peek() !== "\n") {
          this.advance();
        }
      } else if (c === "/" && this.peek(1) === "*") {
        const start = this.position();
        this.advance();
        this.advance();
        while (!this.atEnd() && !(this.peek() === "*" && this.peek(1) === "/")) {
          this.advance();
        }
        if (this.atEnd()) {
          this.errors.push(new CompileError(LEXICAL, start, "незакрытый блочный комментарий"));
          return;
        }
        this.advance();
        this.advance();
      } else {
        return;
      }
    }
  }

  private scanToken(start: Position): void {
    const c = this.peek();
    if (isAlpha(c)) {
      this.scanWord(start);
    } else if (isDigit(c)) {
      this.scanNumber(start);
    } else if (c === "'") {
      this.scanChar(start);
    } else if (c === '"') {
      this.scanString(start);
    } else {
      this.scanSymbol(start);
    }
  }

  private scanWord(start: Position): void {
    let text = "";
    while (isAlpha(this.peek()) || isDigit(this.peek())) {
      text += this.advance();
    }
    if (BOOL_LITERALS.has(text)) {
      this.emit(TokenKind.BoolLiteral, text, start);
    } else if (KEYWORDS.has(text)) {
      this.emit(TokenKind.Keyword, text, start);
    } else {
      this.emit(TokenKind.Identifier, text, start);
    }
  }

  private scanNumber(start: Position): void {
    let text = "";
    if (this.peek() === "0" && this.peek(1) === "x") {
      text += this.advance();
      text += this.advance();
      if (!isHexDigit(this.peek())) {
        this.fail(start, `после '0x' ожидается шестнадцатеричная цифра, найдено: ${show(this.peek())}`);
      }
      while (isHexDigit(this.peek())) {
        text += this.advance();
      }
    } else {
      while (isDigit(this.peek())) {
        text += this.advance();
      }
    }
    if (this.peek() === "L") {
      text += this.advance();
      this.emit(TokenKind.LongLiteral, text, start);
    } else {
      this.emit(TokenKind.IntLiteral, text, start);
    }
  }

  private readChar(quote: string, start: Position, what: string): string {
    const pos = this.position();
    const c = this.peek();
    if (this.atEnd() || c === "\n") {
      throw new UnterminatedLiteral(LEXICAL, start, `незакрытый ${what}`);
    }
    if (c === "\\") {
      this.advance();
      const e = this.peek();
      if (this.atEnd() || e === "\n") {
        throw new UnterminatedLiteral(LEXICAL, start, `незакрытый ${what}`);
      }
      if (!ESCAPES.has(e)) {
        this.advance();
        this.fail(pos, `недопустимая escape-последовательность '\\${isPrintable(e) ? e : "?"}'`);
      }
      this.advance();
      return "\\" + e;
    }
    if (!isPrintable(c)) {
      this.advance();
      this.fail(pos, `недопустимый ${show(c)} в литерале`);
    }
    if (c === '"' || c === "'") {
      if (c === quote) {
        this.fail(pos, `пустой ${what}`);
      }
      this.advance();
      this.fail(pos, `кавычка ${c} в литерале должна быть экранирована`);
    }
    this.advance();
    return c;
  }

  private scanChar(start: Position): void {
    this.advance();
    if (this.peek() === "'") {
      this.advance();
      if (this.peek() === "'") {
        this.advance();
        this.fail(start, "одинарная кавычка в символьном литерале должна быть экранирована: '\\''");
      }
      this.fail(start, "пустой символьный литерал");
    }
    let body: string;
    try {
      body = this.readChar("'", start, "символьный литерал");
    } catch (error) {
      if (error instanceof CompileError && !(error instanceof UnterminatedLiteral)) {
        this.skipUntil("'");
      }
      throw error;
    }
    if (this.peek() === "'") {
      this.advance();
      this.emit(TokenKind.CharLiteral, `'${body}'`, start);
      return;
    }
    if (this.restOfLineHas("'")) {
      this.skipUntil("'");
      this.fail(start, "символьный литерал должен содержать ровно один символ");
    }
    this.fail(start, "незакрытый символьный литерал");
  }

  private restOfLineHas(c: string): boolean {
    const end = this.source.indexOf("\n", this.index);
    return this.source.slice(this.index, end === -1 ? undefined : end).includes(c);
  }

  private scanString(start: Position): void {
    this.advance();
    let body = "";
    let firstError: CompileError | null = null;
    while (this.peek() !== '"') {
      if (this.atEnd() || this.peek() === "\n") {
        throw new CompileError(LEXICAL, start, "незакрытый строковый литерал");
      }
      try {
        body += this.readChar('"', start, "строковый литерал");
      } catch (error) {
        if (!(error instanceof CompileError) || error instanceof UnterminatedLiteral) {
          throw error;
        }
        firstError ??= error;
      }
    }
    this.advance();
    if (firstError) {
      throw firstError;
    }
    this.emit(TokenKind.StringLiteral, `"${body}"`, start);
  }

  private skipUntil(quote: string): void {
    while (!this.atEnd() && this.peek() !== "\n" && this.peek() !== quote) {
      this.advance();
    }
    if (this.peek() === quote) {
      this.advance();
    }
  }

  private scanSymbol(start: Position): void {
    for (const op of OPERATORS) {
      if (this.source.startsWith(op, this.index)) {
        for (let i = 0; i < op.length; i++) {
          this.advance();
        }
        this.emit(TokenKind.Operator, op, start);
        return;
      }
    }
    const c = this.peek();
    if (PUNCTUATION.has(c)) {
      this.advance();
      this.emit(TokenKind.Punctuation, c, start);
      return;
    }
    const first = this.takeCodePoint();
    if (first <= 126) {
      this.fail(start, `недопустимый символ ${showCodePoint(first)}`);
    }
    let bad = String.fromCodePoint(first);
    while (!this.atEnd() && (this.source.codePointAt(this.index) ?? 0) > 126) {
      bad += String.fromCodePoint(this.takeCodePoint());
    }
    if ([...bad].length === 1) {
      this.fail(start, `недопустимый символ ${showCodePoint(first)}`);
    }
    this.fail(start, `недопустимые символы '${bad}'`);
  }

  private takeCodePoint(): number {
    const codePoint = this.source.codePointAt(this.index) ?? 0;
    this.advance();
    if (codePoint > 0xffff) {
      this.advance();
    }
    return codePoint;
  }
}

export function scan(source: string): ScanResult {
  return new Scanner(source).scan();
}
