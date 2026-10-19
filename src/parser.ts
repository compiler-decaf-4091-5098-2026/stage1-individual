import { Token } from "./scanner";

export class ParseError extends Error {
  constructor(message: string, public readonly token: Token) { super(message); }
}

const TYPE_TOKENS = new Set(["int", "long", "bool"]);
const ASSIGN_OPS = new Set(["=", "+=", "-=", "*=", "/=", "%="]);
const BINARY_PRECEDENCE: Record<string, number> = {
  "||": 1, "&&": 2, "==": 3, "!=": 3, "<": 4, ">": 4, "<=": 4, ">=": 4,
  "+": 5, "-": 5, "*": 6, "/": 6, "%": 6
};

export class Parser {
  private current = 0;
  constructor(private readonly tokens: Token[], private readonly filename: string) {}

  parseProgram(): void {
    while (!this.isAtEnd()) {
      if (this.match("import")) this.importDecl();
      else if (TYPE_TOKENS.has(this.peek().kind)) this.fieldOrMethodDecl();
      else if (this.match("void")) this.methodDeclAfterVoid();
      else this.fail(`Ожидалось объявление import, поля или метода`);
    }
  }

  private importDecl(): void { this.consume("IDENTIFIER", "Ожидалось имя после import"); this.consume(";", "Ожидалась ';' после import"); }

  private fieldOrMethodDecl(): void {
    const type = this.advance().kind;
    const name = this.consume("IDENTIFIER", "Ожидалось имя после типа");
    if (this.match("(")) { this.methodTail(); return; }
    this.fieldTailAfterFirst(name);
  }

  private fieldTailAfterFirst(first: Token): void {
    if (this.match("[")) {
      this.consume("INTLITERAL", "Ожидался размер массива — целочисленный литерал");
      this.consume("]", "Ожидалась ']' после размера массива");
    }
    while (this.match(",")) {
      this.consume("IDENTIFIER", "Ожидалось имя поля после запятой");
      if (this.match("[")) {
        this.consume("INTLITERAL", "Ожидался размер массива — целочисленный литерал");
        this.consume("]", "Ожидалась ']' после размера массива");
      }
    }
    this.consume(";", "Ожидалась ';' после объявления поля");
  }

  private methodDeclAfterVoid(): void {
    this.consume("IDENTIFIER", "Ожидалось имя метода после void");
    this.consume("(", "Ожидалась '(' после имени метода");
    this.methodTail();
  }

  private methodTail(): void {
    if (!this.check(")")) {
      do {
        this.parseType();
        this.consume("IDENTIFIER", "Ожидалось имя параметра");
      } while (this.match(","));
    }
    this.consume(")", "Ожидалась ')' после параметров метода");
    this.block();
  }

  private parseType(): void {
    if (!TYPE_TOKENS.has(this.peek().kind)) this.fail("Ожидался тип int, long или bool");
    this.advance();
  }

  private block(): void {
    this.consume("{", "Ожидалась '{' в начале блока");
    while (TYPE_TOKENS.has(this.peek().kind)) this.localFieldDecl();
    while (!this.check("}") && !this.isAtEnd()) this.statement();
    this.consume("}", "Ожидалась '}' в конце блока");
  }

  private localFieldDecl(): void {
    this.parseType();
    this.consume("IDENTIFIER", "Ожидалось имя локальной переменной");
    if (this.match("[")) {
      this.consume("INTLITERAL", "Ожидался размер массива");
      this.consume("]", "Ожидалась ']'");
    }
    while (this.match(",")) {
      this.consume("IDENTIFIER", "Ожидалось имя переменной после запятой");
      if (this.match("[")) {
        this.consume("INTLITERAL", "Ожидался размер массива");
        this.consume("]", "Ожидалась ']'");
      }
    }
    this.consume(";", "Ожидалась ';' после объявления переменных");
  }

  private statement(): void {
    if (this.match("{") ) { this.current--; this.block(); return; }
    if (this.match("if")) {
      this.consume("(", "Ожидалась '(' после if"); this.expression();
      this.consume(")", "Ожидалась ')' после условия if"); this.block();
      if (this.match("else")) this.block();
      return;
    }
    if (this.match("while")) {
      this.consume("(", "Ожидалась '(' после while"); this.expression();
      this.consume(")", "Ожидалась ')' после условия while"); this.block(); return;
    }
    if (this.match("for")) {
      this.consume("(", "Ожидалась '(' после for");
      this.location(); this.assignmentOrIncrement(); this.consume(";", "Ожидалась ';' после инициализации for");
      this.expression(); this.consume(";", "Ожидалась ';' после условия for");
      this.forUpdate(); this.consume(")", "Ожидалась ')' после заголовка for"); this.block(); return;
    }
    if (this.match("return")) {
      if (!this.check(";")) this.expression();
      this.consume(";", "Ожидалась ';' после return"); return;
    }
    if (this.match("break")) { this.consume(";", "Ожидалась ';' после break"); return; }
    if (this.match("continue")) { this.consume(";", "Ожидалась ';' после continue"); return; }
    if (this.check("IDENTIFIER") && this.checkNext("(")) {
      this.methodCall(); this.consume(";", "Ожидалась ';' после вызова метода"); return;
    }
    if (this.check("IDENTIFIER")) {
      this.location(); this.assignmentOrIncrement(); this.consume(";", "Ожидалась ';' после выражения присваивания"); return;
    }
    this.fail("Ожидался оператор, объявление или закрывающая скобка блока");
  }

  private forUpdate(): void {
    this.location(); this.assignmentOrIncrement();
  }

  private assignmentOrIncrement(): void {
    if (this.match("++", "--")) return;
    if (this.match(...Array.from(ASSIGN_OPS))) { this.expression(); return; }
    this.fail("Ожидался оператор присваивания или ++/--");
  }

  private location(): void {
    this.consume("IDENTIFIER", "Ожидалось имя переменной");
    if (this.match("[")) { this.expression(); this.consume("]", "Ожидалась ']' после индекса массива"); }
  }

  private methodCall(): void {
    this.consume("IDENTIFIER", "Ожидалось имя метода");
    this.consume("(", "Ожидалась '(' после имени метода");
    if (!this.check(")")) {
      this.externalArg();
      while (this.match(",")) this.externalArg();
    }
    this.consume(")", "Ожидалась ')' после аргументов метода");
  }

  private externalArg(): void {
    if (this.check("STRINGLITERAL")) this.advance();
    else this.expression();
  }

  private expression(minPrecedence = 1): void {
    this.unary();
    while (true) {
      const op = this.peek().kind;
      const precedence = BINARY_PRECEDENCE[op];
      if (precedence === undefined || precedence < minPrecedence) break;
      this.advance();
      this.expression(precedence + 1);
    }
  }

  private unary(): void {
    if (this.match("-", "!")) { this.unary(); return; }
    if (this.match("int", "long")) {
      this.consume("(", "Ожидалась '(' после приведения типа"); this.expression();
      this.consume(")", "Ожидалась ')' после приведения типа"); return;
    }
    if (this.match("len")) {
      this.consume("(", "Ожидалась '(' после len"); this.consume("IDENTIFIER", "Ожидалось имя массива внутри len");
      this.consume(")", "Ожидалась ')' после len"); return;
    }
    if (this.match("(")) { this.expression(); this.consume(")", "Ожидалась ')' после выражения"); return; }
    if (this.check("INTLITERAL") || this.check("LONGLITERAL") || this.check("CHARLITERAL") || this.check("BOOLEANLITERAL")) { this.advance(); return; }
    if (this.check("IDENTIFIER")) {
      if (this.checkNext("(")) { this.methodCall(); return; }
      this.location(); return;
    }
    this.fail("Ожидалось выражение");
  }

  private match(...kinds: string[]): boolean {
    if (kinds.includes(this.peek().kind)) { this.advance(); return true; }
    return false;
  }
  private consume(kind: string, message: string): Token {
    if (this.check(kind)) return this.advance();
    this.fail(message);
  }
  private check(kind: string): boolean { return this.peek().kind === kind; }
  private checkNext(kind: string): boolean { return (this.tokens[this.current + 1]?.kind ?? "EOF") === kind; }
  private advance(): Token { if (!this.isAtEnd()) this.current++; return this.tokens[this.current - 1] ?? this.peek(); }
  private isAtEnd(): boolean { return this.current >= this.tokens.length; }
  private peek(): Token { return this.tokens[this.current] ?? { kind: "EOF", text: "конец файла", line: this.tokens.length ? this.tokens[this.tokens.length - 1].line : 1, column: this.tokens.length ? this.tokens[this.tokens.length - 1].column : 1 }; }
  private fail(message: string): never {
    const token = this.peek();
    throw new ParseError(`${this.filename}:${token.line}:${token.column}: синтаксическая ошибка: ${message}; найдено ${JSON.stringify(token.text)}`, token);
  }
}
