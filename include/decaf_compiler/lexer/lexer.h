#pragma once
#include <cctype>
#include <fstream>
#include <stdexcept>
#include <string>
#include <vector>

enum class TokenType {
    EndOfFile,
    Invalid,
    Identifier,
    IntLiteral,
    LongLiteral,
    BoolLiteral,
    CharLiteral,
    StringLiteral,
    KeywordBool,
    KeywordBreak,
    KeywordContinue,
    KeywordElse,
    KeywordFor,
    KeywordIf,
    KeywordImport,
    KeywordInt,
    KeywordLen,
    KeywordLong,
    KeywordReturn,
    KeywordVoid,
    KeywordWhile,
    LBrace,
    RBrace,
    LParen,
    RParen,
    LBracket,
    RBracket,
    Semicolon,
    Comma,
    Assign,
    PlusAssign,
    MinusAssign,
    StarAssign,
    SlashAssign,
    PercentAssign,
    Plus,
    Minus,
    Star,
    Slash,
    Percent,
    Increment,
    Decrement,
    Less,
    LessEqual,
    Greater,
    GreaterEqual,
    Equal,
    NotEqual,
    LogicalAnd,
    LogicalOr,
    LogicalNot,
};

const char* tokenTypeName(TokenType type);

struct Position {
    unsigned long long line;
    unsigned long long column;
};

struct LexerError {
    std::string message;
    Position    position;
};

struct Token {
    TokenType   tokenType = TokenType::EndOfFile;
    std::string data;
    Position    position{1, 1};
};

std::string tokenToString(const Token& token);

class Lexer {
public:
    explicit Lexer(const std::string& filename);

    Token next();
    Token peek();

    bool                           hasErrors() const { return !m_errors.empty(); }
    const std::vector<LexerError>& errors() const { return m_errors; }
    void                           clearErrors() { m_errors.clear(); }
    const std::string&             filename() const { return m_filename; }

private:
    std::ifstream      m_file;
    std::string        m_filename;
    unsigned long long line = 1;
    unsigned long long col  = 1;

    Token m_lookahead;
    bool  m_hasLookahead = false;

    std::vector<LexerError> m_errors;

    void     recordError(const std::string& message, Position position);
    Position currentPosition() const;

    Token     scanToken();
    TokenType classifySymbol(const std::string& symbol);
    TokenType classifyWord(const std::string& word);

    std::string readIdentifierOrKeyword();
    std::string readNumber(bool& isLongLiteral);
    std::string readStringOrChar(char quote, int& elementCount, bool& closed, Position start);
    std::string readOperatorOrPunctuation();

    void skipWhitespace();
    void advance();
    int  currentChar() { return m_file.peek(); }
};
