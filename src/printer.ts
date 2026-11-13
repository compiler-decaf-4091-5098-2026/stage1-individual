import * as Ast from "./ast";

class TreePrinter {
  private readonly lines: string[] = [];

  print(program: Ast.Program): string {
    this.line(0, "Program");
    for (const decl of program.imports) {
      this.line(1, `Import ${decl.name}`);
    }
    for (const field of program.fields) {
      this.field(1, field);
    }
    for (const method of program.methods) {
      this.method(1, method);
    }
    return this.lines.join("\n");
  }

  private line(depth: number, text: string): void {
    this.lines.push("  ".repeat(depth) + text);
  }

  private field(depth: number, field: Ast.FieldDecl): void {
    const vars = field.vars.map((v) => (v.size === null ? v.name : `${v.name}[${v.size}]`));
    this.line(depth, `Field ${field.type} ${vars.join(", ")}`);
  }

  private method(depth: number, method: Ast.MethodDecl): void {
    const params = method.params.map((p) => `${p.type} ${p.name}`).join(", ");
    this.line(depth, `Method ${method.returnType} ${method.name}(${params})`);
    this.block(depth + 1, method.body);
  }

  private block(depth: number, block: Ast.Block): void {
    this.line(depth, "Block");
    for (const field of block.fields) {
      this.field(depth + 1, field);
    }
    for (const statement of block.statements) {
      this.statement(depth + 1, statement);
    }
  }

  private statement(depth: number, s: Ast.Statement): void {
    switch (s.kind) {
      case "Assign":
        this.assign(depth, s);
        break;
      case "CallStatement":
        this.expr(depth, s.call);
        break;
      case "If":
        this.line(depth, "If");
        this.expr(depth + 1, s.condition);
        this.block(depth + 1, s.then);
        if (s.otherwise) {
          this.line(depth, "Else");
          this.block(depth + 1, s.otherwise);
        }
        break;
      case "For":
        this.line(depth, `For ${s.variable}`);
        this.expr(depth + 1, s.init);
        this.expr(depth + 1, s.condition);
        this.assign(depth + 1, s.update);
        this.block(depth + 1, s.body);
        break;
      case "While":
        this.line(depth, "While");
        this.expr(depth + 1, s.condition);
        this.block(depth + 1, s.body);
        break;
      case "Return":
        this.line(depth, "Return");
        if (s.value) {
          this.expr(depth + 1, s.value);
        }
        break;
      case "Break":
        this.line(depth, "Break");
        break;
      case "Continue":
        this.line(depth, "Continue");
        break;
    }
  }

  private assign(depth: number, s: Ast.AssignStatement): void {
    this.line(depth, `Assign ${s.op}`);
    this.expr(depth + 1, s.target);
    if (s.value) {
      this.expr(depth + 1, s.value);
    }
  }

  private expr(depth: number, e: Ast.Expr | Ast.StringLiteral): void {
    switch (e.kind) {
      case "Location":
        this.line(depth, e.index ? `Index ${e.name}` : `Var ${e.name}`);
        if (e.index) {
          this.expr(depth + 1, e.index);
        }
        break;
      case "MethodCall":
        this.line(depth, `Call ${e.name}`);
        for (const arg of e.args) {
          this.expr(depth + 1, arg);
        }
        break;
      case "Literal":
        this.line(depth, `Literal ${e.type} ${e.text}`);
        break;
      case "StringLiteral":
        this.line(depth, `Literal string ${e.text}`);
        break;
      case "Cast":
        this.line(depth, `Cast ${e.type}`);
        this.expr(depth + 1, e.operand);
        break;
      case "Len":
        this.line(depth, `Len ${e.name}`);
        break;
      case "Binary":
        this.line(depth, `Binary ${e.op}`);
        this.expr(depth + 1, e.left);
        this.expr(depth + 1, e.right);
        break;
      case "Unary":
        this.line(depth, `Unary ${e.op}`);
        this.expr(depth + 1, e.operand);
        break;
    }
  }
}

export function printTree(program: Ast.Program): string {
  return new TreePrinter().print(program);
}
