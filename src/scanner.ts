export type Token = {
  kind: string;
  text: string;
  line: number;
  column: number;
};

export type LexError = { message: string; line: number; column: number };

const keywords = new Set([
  "import", "void", "int", "long", "bool", "if", "else", "for", "while",
  "return", "break", "continue", "true", "false", "len"
]);
const operators = ["+=", "-=", "*=", "/=", "%=", "++", "--", "<=", ">=", "==", "!=", "&&", "||"];
const singleSymbols = new Set("{}()[];,:+-*/%<>=!".split(""));
const validEscapes = new Set(["\\\"", "\\'", "\\\\", "\\t", "\\n", "\\r", "\\f"]);

export class Scanner {
  private index = 0;
  private line = 1;
  private column = 1;
  readonly tokens: Token[] = [];
  readonly errors: LexError[] = [];

  constructor(private readonly source: string, private readonly filename: string) {}

  scan(): { tokens: Token[]; errors: LexError[] } {
    while (!this.atEnd()) {
      const ch = this.peek();
      if (/\s/.test(ch)) { this.advance(); continue; }
      if (ch === "/" && this.peek(1) === "/") { this.lineComment(); continue; }
      if (ch === "/" && this.peek(1) === "*") { this.blockComment(); continue; }
      if (ch === '"') { this.stringLiteral(); continue; }
      if (ch === "'") { this.charLiteral(); continue; }
      if (/[A-Za-z_]/.test(ch)) { this.identifier(); continue; }
      if (/[0-9]/.test(ch)) { this.number(); continue; }
      const op = operators.find(candidate => this.source.startsWith(candidate, this.index));
      if (op) { this.add(op, op); for (let i = 0; i < op.length; i++) this.advance(); continue; }
      if (singleSymbols.has(ch)) { this.add(ch, ch); this.advance(); continue; }
      const startLine = this.line, startColumn = this.column;
      this.errors.push({ message: `Недопустимый символ ${JSON.stringify(ch)}`, line: startLine, column: startColumn });
      this.advance();
    }
    return { tokens: this.tokens, errors: this.errors };
  }

  private atEnd(): boolean { return this.index >= this.source.length; }
  private peek(offset = 0): string { return this.source[this.index + offset] ?? "\0"; }
  private advance(): string {
    const ch = this.source[this.index++] ?? "\0";
    if (ch === "\n") { this.line++; this.column = 1; }
    else { this.column++; }
    return ch;
  }
  private add(kind: string, text: string, line = this.line, column = this.column): void {
    this.tokens.push({ kind, text, line, column });
  }
  private lineComment(): void { while (!this.atEnd() && this.peek() !== "\n") this.advance(); }
  private blockComment(): void {
    const line = this.line, column = this.column;
    this.advance(); this.advance();
    while (!this.atEnd() && !(this.peek() === "*" && this.peek(1) === "/")) this.advance();
    if (this.atEnd()) this.errors.push({ message: "Незакрытый блочный комментарий", line, column });
    else { this.advance(); this.advance(); }
  }
  private identifier(): void {
    const line = this.line, column = this.column, start = this.index;
    this.advance();
    while (/[A-Za-z0-9_]/.test(this.peek())) this.advance();
    const text = this.source.slice(start, this.index);
    if (text === "true" || text === "false") this.add("BOOLEANLITERAL", text, line, column);
    else if (keywords.has(text)) this.add(text, text, line, column);
    else this.add("IDENTIFIER", text, line, column);
  }
  private number(): void {
    const line = this.line, column = this.column, start = this.index;
    if (this.peek() === "0" && (this.peek(1) === "x")) {
      this.advance(); this.advance();
      const digitStart = this.index;
      while (/[0-9a-fA-F]/.test(this.peek())) this.advance();
      if (digitStart === this.index) this.errors.push({ message: "После 0x ожидается шестнадцатеричная цифра", line, column });
    } else {
      while (/[0-9]/.test(this.peek())) this.advance();
    }
    let kind = "INTLITERAL";
    if (this.peek() === "L") { this.advance(); kind = "LONGLITERAL"; }
    this.add(kind, this.source.slice(start, this.index), line, column);
  }
  private quoted(quote: string, kind: string): void {
    const line = this.line, column = this.column, start = this.index;
    this.advance();
    let closed = false;
    while (!this.atEnd()) {
      const ch = this.peek();
      if (ch === quote) { this.advance(); closed = true; break; }
      if (ch === "\n" || ch === "\r") break;
      if (ch === "\\") {
        const escape = this.peek() + this.peek(1);
        if (!validEscapes.has(escape)) {
          this.errors.push({ message: `Недопустимая escape-последовательность ${JSON.stringify(escape)}`, line: this.line, column: this.column });
          this.advance(); if (!this.atEnd()) this.advance();
        } else { this.advance(); this.advance(); }
      } else {
        const code = ch.charCodeAt(0);
        if (code < 32 || code > 126 || ch === '"' && quote === "'" || ch === "'" && quote === '"') {
          this.errors.push({ message: "Недопустимый символ в литерале", line: this.line, column: this.column });
        }
        this.advance();
      }
    }
    if (!closed) this.errors.push({ message: quote === '"' ? "Незакрытая строка" : "Незакрытый символьный литерал", line, column });
    const text = this.source.slice(start, this.index);
    if (closed) {
      if (kind === "CHARLITERAL") {
        const body = text.slice(1, -1);
        if (body.length === 0 || (body[0] !== "\\" && body.length !== 1) || (body[0] === "\\" && body.length !== 2)) {
          this.errors.push({ message: "Символьный литерал должен содержать один символ или escape-последовательность", line, column });
        }
      }
      this.add(kind, text, line, column);
    }
  }
  private stringLiteral(): void { this.quoted('"', "STRINGLITERAL"); }
  private charLiteral(): void { this.quoted("'", "CHARLITERAL"); }
}

export function scanSource(source: string, filename: string): { tokens: Token[]; errors: LexError[] } {
  return new Scanner(source, filename).scan();
}
