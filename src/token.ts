export enum TokenKind {
  Identifier = "IDENTIFIER",
  IntLiteral = "INTLITERAL",
  LongLiteral = "LONGLITERAL",
  CharLiteral = "CHARLITERAL",
  StringLiteral = "STRINGLITERAL",
  BoolLiteral = "BOOLEANLITERAL",
  Keyword = "KEYWORD",
  Operator = "OPERATOR",
  Punctuation = "PUNCTUATION",
  EOF = "EOF",
}

export interface Position {
  line: number;
  column: number;
}

export interface Token {
  kind: TokenKind;
  text: string;
  pos: Position;
}

export const KEYWORDS: ReadonlySet<string> = new Set([
  "bool",
  "break",
  "continue",
  "else",
  "for",
  "if",
  "import",
  "int",
  "len",
  "long",
  "return",
  "void",
  "while",
]);

export const BOOL_LITERALS: ReadonlySet<string> = new Set(["true", "false"]);

export const OPERATORS: readonly string[] = [
  "&&",
  "||",
  "==",
  "!=",
  "<=",
  ">=",
  "+=",
  "-=",
  "*=",
  "/=",
  "%=",
  "++",
  "--",
  "+",
  "-",
  "*",
  "/",
  "%",
  "<",
  ">",
  "!",
  "=",
];

export const PUNCTUATION: ReadonlySet<string> = new Set(["(", ")", "[", "]", "{", "}", ";", ","]);

const TYPED_KINDS: ReadonlySet<TokenKind> = new Set([
  TokenKind.Identifier,
  TokenKind.IntLiteral,
  TokenKind.LongLiteral,
  TokenKind.CharLiteral,
  TokenKind.StringLiteral,
  TokenKind.BoolLiteral,
]);

export function formatToken(token: Token): string {
  if (TYPED_KINDS.has(token.kind)) {
    return `${token.pos.line} ${token.kind} ${token.text}`;
  }
  return `${token.pos.line} ${token.text}`;
}

export function describeToken(token: Token): string {
  switch (token.kind) {
    case TokenKind.EOF:
      return "конец файла";
    case TokenKind.Identifier:
      return `идентификатор '${token.text}'`;
    case TokenKind.Keyword:
      return `ключевое слово '${token.text}'`;
    case TokenKind.IntLiteral:
    case TokenKind.LongLiteral:
      return `числовой литерал ${token.text}`;
    case TokenKind.CharLiteral:
      return `символьный литерал ${token.text}`;
    case TokenKind.StringLiteral:
      return `строковый литерал ${token.text}`;
    case TokenKind.BoolLiteral:
      return `логический литерал ${token.text}`;
    default:
      return `'${token.text}'`;
  }
}
