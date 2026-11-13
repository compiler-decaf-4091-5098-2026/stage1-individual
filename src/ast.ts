import { Position } from "./token";

export type TypeName = "int" | "long" | "bool";
export type ReturnType = TypeName | "void";
export type AssignOp = "=" | "+=" | "-=" | "*=" | "/=" | "%=";
export type IncrementOp = "++" | "--";
export type BinaryOp = "+" | "-" | "*" | "/" | "%" | "<" | ">" | "<=" | ">=" | "==" | "!=" | "&&" | "||";
export type UnaryOp = "-" | "!";

interface Node {
  pos: Position;
}

export interface Program extends Node {
  kind: "Program";
  imports: ImportDecl[];
  fields: FieldDecl[];
  methods: MethodDecl[];
}

export interface ImportDecl extends Node {
  kind: "ImportDecl";
  name: string;
}

export interface FieldDecl extends Node {
  kind: "FieldDecl";
  type: TypeName;
  vars: VarDecl[];
}

export interface VarDecl extends Node {
  kind: "VarDecl";
  name: string;
  size: string | null;
}

export interface Param extends Node {
  kind: "Param";
  type: TypeName;
  name: string;
}

export interface MethodDecl extends Node {
  kind: "MethodDecl";
  returnType: ReturnType;
  name: string;
  params: Param[];
  body: Block;
}

export interface Block extends Node {
  kind: "Block";
  fields: FieldDecl[];
  statements: Statement[];
}

export type Statement =
  | AssignStatement
  | CallStatement
  | IfStatement
  | ForStatement
  | WhileStatement
  | ReturnStatement
  | BreakStatement
  | ContinueStatement;

export interface Location extends Node {
  kind: "Location";
  name: string;
  index: Expr | null;
}

export interface AssignStatement extends Node {
  kind: "Assign";
  target: Location;
  op: AssignOp | IncrementOp;
  value: Expr | null;
}

export interface CallStatement extends Node {
  kind: "CallStatement";
  call: MethodCall;
}

export interface IfStatement extends Node {
  kind: "If";
  condition: Expr;
  then: Block;
  otherwise: Block | null;
}

export interface ForStatement extends Node {
  kind: "For";
  variable: string;
  init: Expr;
  condition: Expr;
  update: AssignStatement;
  body: Block;
}

export interface WhileStatement extends Node {
  kind: "While";
  condition: Expr;
  body: Block;
}

export interface ReturnStatement extends Node {
  kind: "Return";
  value: Expr | null;
}

export interface BreakStatement extends Node {
  kind: "Break";
}

export interface ContinueStatement extends Node {
  kind: "Continue";
}

export type Expr = Location | MethodCall | Literal | Cast | Len | Binary | Unary;

export interface MethodCall extends Node {
  kind: "MethodCall";
  name: string;
  args: (Expr | StringLiteral)[];
}

export interface Literal extends Node {
  kind: "Literal";
  type: "int" | "long" | "char" | "bool";
  text: string;
}

export interface StringLiteral extends Node {
  kind: "StringLiteral";
  text: string;
}

export interface Cast extends Node {
  kind: "Cast";
  type: "int" | "long";
  operand: Expr;
}

export interface Len extends Node {
  kind: "Len";
  name: string;
}

export interface Binary extends Node {
  kind: "Binary";
  op: BinaryOp;
  left: Expr;
  right: Expr;
}

export interface Unary extends Node {
  kind: "Unary";
  op: UnaryOp;
  operand: Expr;
}
