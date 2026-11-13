import * as Ast from "./ast";
import { CompileError } from "./diagnostics";
import { Token, TokenKind, describeToken } from "./token";

const SYNTAX = "синтаксическая ошибка";
const TYPES: ReadonlySet<string> = new Set(["int", "long", "bool"]);
const ASSIGN_OPS: ReadonlySet<string> = new Set(["=", "+=", "-=", "*=", "/=", "%="]);
const INCREMENT_OPS: ReadonlySet<string> = new Set(["++", "--"]);
const BINARY_LEVELS: readonly (readonly Ast.BinaryOp[])[] = [
  ["||"],
  ["&&"],
  ["==", "!="],
  ["<", "<=", ">=", ">"],
  ["+", "-"],
  ["*", "/", "%"],
];

export class Parser {
  private current = 0;

  constructor(private readonly tokens: Token[]) {}

  parseProgram(): Ast.Program {
    const pos = this.peek().pos;
    const imports: Ast.ImportDecl[] = [];
    const fields: Ast.FieldDecl[] = [];
    const methods: Ast.MethodDecl[] = [];
    while (this.check("import")) {
      imports.push(this.parseImport());
    }
    while (!this.atEnd()) {
      if (this.check("import")) {
        this.fail("объявление import допустимо только в начале программы");
      }
      if (this.isFieldDeclStart()) {
        if (methods.length > 0) {
          this.fail("объявления полей должны предшествовать объявлениям методов");
        }
        fields.push(this.parseFieldDecl());
      } else if (this.isType() || this.check("void")) {
        methods.push(this.parseMethodDecl());
      } else {
        this.fail(`ожидалось объявление поля или метода, найдено: ${describeToken(this.peek())}`);
      }
    }
    return { kind: "Program", pos, imports, fields, methods };
  }

  private peek(offset = 0): Token {
    return this.tokens[Math.min(this.current + offset, this.tokens.length - 1)];
  }

  private atEnd(): boolean {
    return this.peek().kind === TokenKind.EOF;
  }

  private advance(): Token {
    const token = this.peek();
    if (!this.atEnd()) {
      this.current++;
    }
    return token;
  }

  private check(text: string, offset = 0): boolean {
    const token = this.peek(offset);
    return token.text === text && token.kind !== TokenKind.StringLiteral && token.kind !== TokenKind.CharLiteral;
  }

  private match(text: string): boolean {
    if (this.check(text)) {
      this.advance();
      return true;
    }
    return false;
  }

  private fail(message: string, token: Token = this.peek()): never {
    throw new CompileError(SYNTAX, token.pos, message);
  }

  private expect(text: string): Token {
    if (!this.check(text)) {
      this.fail(`ожидалось '${text}', найдено: ${describeToken(this.peek())}`);
    }
    return this.advance();
  }

  private expectIdentifier(): Token {
    if (this.peek().kind !== TokenKind.Identifier) {
      this.fail(`ожидался идентификатор, найдено: ${describeToken(this.peek())}`);
    }
    return this.advance();
  }

  private isType(offset = 0): boolean {
    const token = this.peek(offset);
    return token.kind === TokenKind.Keyword && TYPES.has(token.text);
  }

  private isFieldDeclStart(): boolean {
    return this.isType() && this.peek(1).kind === TokenKind.Identifier && !this.check("(", 2);
  }

  private parseType(): Ast.TypeName {
    if (!this.isType()) {
      this.fail(`ожидался тип (int, long или bool), найдено: ${describeToken(this.peek())}`);
    }
    return this.advance().text as Ast.TypeName;
  }

  private parseImport(): Ast.ImportDecl {
    const pos = this.expect("import").pos;
    const name = this.expectIdentifier().text;
    this.expect(";");
    return { kind: "ImportDecl", pos, name };
  }

  private parseFieldDecl(): Ast.FieldDecl {
    const pos = this.peek().pos;
    const type = this.parseType();
    const vars: Ast.VarDecl[] = [];
    do {
      const id = this.expectIdentifier();
      let size: string | null = null;
      if (this.match("[")) {
        if (this.peek().kind !== TokenKind.IntLiteral) {
          this.fail(`размер массива должен быть целочисленным литералом, найдено: ${describeToken(this.peek())}`);
        }
        size = this.advance().text;
        this.expect("]");
      }
      vars.push({ kind: "VarDecl", pos: id.pos, name: id.text, size });
    } while (this.match(","));
    this.expect(";");
    return { kind: "FieldDecl", pos, type, vars };
  }

  private parseMethodDecl(): Ast.MethodDecl {
    const pos = this.peek().pos;
    const returnType: Ast.ReturnType = this.match("void") ? "void" : this.parseType();
    const name = this.expectIdentifier().text;
    this.expect("(");
    const params: Ast.Param[] = [];
    if (!this.check(")")) {
      do {
        const paramPos = this.peek().pos;
        const type = this.parseType();
        const id = this.expectIdentifier();
        params.push({ kind: "Param", pos: paramPos, type, name: id.text });
      } while (this.match(","));
    }
    this.expect(")");
    const body = this.parseBlock();
    return { kind: "MethodDecl", pos, returnType, name, params, body };
  }

  private parseBlock(): Ast.Block {
    const pos = this.expect("{").pos;
    const fields: Ast.FieldDecl[] = [];
    const statements: Ast.Statement[] = [];
    while (this.isType()) {
      fields.push(this.parseFieldDecl());
    }
    while (!this.check("}")) {
      if (this.atEnd()) {
        this.fail("ожидалось '}', найдено: конец файла");
      }
      if (this.isType()) {
        this.fail("объявления переменных должны предшествовать операторам блока");
      }
      statements.push(this.parseStatement());
    }
    this.expect("}");
    return { kind: "Block", pos, fields, statements };
  }

  private parseStatement(): Ast.Statement {
    const token = this.peek();
    if (token.kind === TokenKind.Identifier) {
      if (this.check("(", 1)) {
        const call = this.parseMethodCall();
        this.expect(";");
        return { kind: "CallStatement", pos: token.pos, call };
      }
      const assign = this.parseAssignment();
      this.expect(";");
      return assign;
    }
    if (token.kind === TokenKind.Keyword) {
      switch (token.text) {
        case "if":
          return this.parseIf();
        case "for":
          return this.parseFor();
        case "while":
          return this.parseWhile();
        case "return":
          return this.parseReturn();
        case "break":
          this.advance();
          this.expect(";");
          return { kind: "Break", pos: token.pos };
        case "continue":
          this.advance();
          this.expect(";");
          return { kind: "Continue", pos: token.pos };
      }
    }
    return this.fail(`ожидался оператор, найдено: ${describeToken(token)}`);
  }

  private parseLocation(): Ast.Location {
    const id = this.expectIdentifier();
    let index: Ast.Expr | null = null;
    if (this.match("[")) {
      index = this.parseExpr();
      this.expect("]");
    }
    return { kind: "Location", pos: id.pos, name: id.text, index };
  }

  private parseAssignment(): Ast.AssignStatement {
    const target = this.parseLocation();
    const opToken = this.peek();
    if (opToken.kind === TokenKind.Operator && INCREMENT_OPS.has(opToken.text)) {
      this.advance();
      return { kind: "Assign", pos: target.pos, target, op: opToken.text as Ast.IncrementOp, value: null };
    }
    if (opToken.kind === TokenKind.Operator && ASSIGN_OPS.has(opToken.text)) {
      this.advance();
      const value = this.parseExpr();
      return { kind: "Assign", pos: target.pos, target, op: opToken.text as Ast.AssignOp, value };
    }
    return this.fail(`ожидался оператор присваивания, найдено: ${describeToken(opToken)}`);
  }

  private parseIf(): Ast.IfStatement {
    const pos = this.expect("if").pos;
    this.expect("(");
    const condition = this.parseExpr();
    this.expect(")");
    const then = this.parseBlock();
    const otherwise = this.match("else") ? this.parseBlock() : null;
    return { kind: "If", pos, condition, then, otherwise };
  }

  private parseFor(): Ast.ForStatement {
    const pos = this.expect("for").pos;
    this.expect("(");
    const variable = this.expectIdentifier().text;
    this.expect("=");
    const init = this.parseExpr();
    this.expect(";");
    const condition = this.parseExpr();
    this.expect(";");
    const update = this.parseAssignment();
    this.expect(")");
    const body = this.parseBlock();
    return { kind: "For", pos, variable, init, condition, update, body };
  }

  private parseWhile(): Ast.WhileStatement {
    const pos = this.expect("while").pos;
    this.expect("(");
    const condition = this.parseExpr();
    this.expect(")");
    const body = this.parseBlock();
    return { kind: "While", pos, condition, body };
  }

  private parseReturn(): Ast.ReturnStatement {
    const pos = this.expect("return").pos;
    const value = this.check(";") ? null : this.parseExpr();
    this.expect(";");
    return { kind: "Return", pos, value };
  }

  private parseMethodCall(): Ast.MethodCall {
    const id = this.expectIdentifier();
    this.expect("(");
    const args: (Ast.Expr | Ast.StringLiteral)[] = [];
    if (!this.check(")")) {
      do {
        const token = this.peek();
        if (token.kind === TokenKind.StringLiteral) {
          this.advance();
          args.push({ kind: "StringLiteral", pos: token.pos, text: token.text });
        } else {
          args.push(this.parseExpr());
        }
      } while (this.match(","));
    }
    this.expect(")");
    return { kind: "MethodCall", pos: id.pos, name: id.text, args };
  }

  private parseExpr(): Ast.Expr {
    return this.parseBinary(0);
  }

  private parseBinary(level: number): Ast.Expr {
    if (level === BINARY_LEVELS.length) {
      return this.parseUnary();
    }
    const ops = BINARY_LEVELS[level];
    let left = this.parseBinary(level + 1);
    while (this.peek().kind === TokenKind.Operator && ops.includes(this.peek().text as Ast.BinaryOp)) {
      const opToken = this.advance();
      const right = this.parseBinary(level + 1);
      left = { kind: "Binary", pos: opToken.pos, op: opToken.text as Ast.BinaryOp, left, right };
    }
    return left;
  }

  private parseUnary(): Ast.Expr {
    const token = this.peek();
    if (token.kind === TokenKind.Operator && (token.text === "-" || token.text === "!")) {
      this.advance();
      const operand = this.parseUnary();
      return { kind: "Unary", pos: token.pos, op: token.text, operand };
    }
    return this.parsePrimary();
  }

  private parsePrimary(): Ast.Expr {
    const token = this.peek();
    switch (token.kind) {
      case TokenKind.IntLiteral:
        this.advance();
        return { kind: "Literal", pos: token.pos, type: "int", text: token.text };
      case TokenKind.LongLiteral:
        this.advance();
        return { kind: "Literal", pos: token.pos, type: "long", text: token.text };
      case TokenKind.CharLiteral:
        this.advance();
        return { kind: "Literal", pos: token.pos, type: "char", text: token.text };
      case TokenKind.BoolLiteral:
        this.advance();
        return { kind: "Literal", pos: token.pos, type: "bool", text: token.text };
      case TokenKind.Identifier:
        return this.check("(", 1) ? this.parseMethodCall() : this.parseLocation();
      case TokenKind.Keyword:
        if (token.text === "int" || token.text === "long") {
          this.advance();
          this.expect("(");
          const operand = this.parseExpr();
          this.expect(")");
          return { kind: "Cast", pos: token.pos, type: token.text, operand };
        }
        if (token.text === "len") {
          this.advance();
          this.expect("(");
          const name = this.expectIdentifier().text;
          this.expect(")");
          return { kind: "Len", pos: token.pos, name };
        }
        break;
      case TokenKind.Punctuation:
        if (token.text === "(") {
          this.advance();
          const inner = this.parseExpr();
          this.expect(")");
          return inner;
        }
        break;
      case TokenKind.StringLiteral:
        return this.fail("строковый литерал допустим только как аргумент вызова метода");
      default:
        break;
    }
    return this.fail(`ожидалось выражение, найдено: ${describeToken(token)}`);
  }
}

export function parse(tokens: Token[]): Ast.Program {
  return new Parser(tokens).parseProgram();
}
