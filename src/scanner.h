#pragma once
#include <string>
#include <vector>

enum class TokType {
    IDENTIFIER, INTLITERAL, LONGLITERAL, CHARLITERAL, BOOLEANLITERAL, STRINGLITERAL,
    KW_IF, KW_BOOL, KW_BREAK, KW_IMPORT, KW_CONTINUE, KW_ELSE, KW_FALSE, KW_FOR, KW_WHILE, KW_INT, KW_LONG, KW_RETURN, KW_LEN, KW_TRUE, KW_VOID,
    PLUS, MINUS, STAR, SLASH, PERCENT, LT, GT, LE, GE, EQ, NE, ANDAND, OROR, NOT,
    ASSIGN, PLUS_ASSIGN, MINUS_ASSIGN, STAR_ASSIGN, SLASH_ASSIGN, PERCENT_ASSIGN, PLUSPLUS, MINUSMINUS,
    LPAREN, RPAREN, LBRACKET, RBRACKET, LBRACE, RBRACE, SEMI, COMMA, DOT,
    END, ERROR
};

struct Token {
    TokType type;
    std::string text;
    int line;
    int col;
};

class Scanner {
public:
    Scanner(const std::string& src, const std::string& filename);
    Token next();
    std::vector<std::string> errors;
    static std::string typeName(TokType t);

private:
    const std::string& s;
    std::string file;
    size_t pos;
    int line, col;

    void advance();
    void skipWhitespaceAndComments();
    void errorAt(int l, int c, const std::string& msg);
    static TokType keywordType(const std::string& w);
    bool validCharEscape(char e);
    Token readString(int sl, int sc);
    Token readChar(int sl, int sc);
    Token readOperator(int sl, int sc);
};