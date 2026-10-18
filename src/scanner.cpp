#include "scanner.h"
#include <cctype>
#include <iostream>

Scanner::Scanner(const std::string& src, const std::string& filename)
    : s(src), file(filename), pos(0), line(1), col(1) {}

std::string Scanner::typeName(TokType t) {
    switch (t) {
        case TokType::IDENTIFIER:     return "IDENTIFIER";
        case TokType::INTLITERAL:     return "INTLITERAL";
        case TokType::LONGLITERAL:    return "LONGLITERAL";
        case TokType::CHARLITERAL:    return "CHARLITERAL";
        case TokType::BOOLEANLITERAL: return "BOOLEANLITERAL";
        case TokType::STRINGLITERAL:  return "STRINGLITERAL";
        default:                      return "";
    }
}

Token Scanner::next() {
    skipWhitespaceAndComments();
    if (pos >= s.size()) return Token{TokType::END, "", line, col};
    int startLine = line, startCol = col;
    char c = s[pos];

    if (isalpha((unsigned char)c) || c == '_') {
        std::string text;
        while (pos < s.size() && (isalnum((unsigned char)s[pos]) || s[pos] == '_')) {
            text += s[pos]; advance();
        }
        return Token{keywordType(text), text, startLine, startCol};
    }
    if (isdigit((unsigned char)c)) {
        std::string text;
        bool isHex = false;
        if (c == '0' && pos + 1 < s.size() && (s[pos+1] == 'x' || s[pos+1] == 'X')) {
            text += s[pos]; advance(); text += s[pos]; advance();
            isHex = true;
        }
        while (pos < s.size()) {
            char d = s[pos];
            bool ok = isHex ? isxdigit((unsigned char)d) : isdigit((unsigned char)d);
            if (!ok) break;
            text += d; advance();
        }
        if (pos < s.size() && s[pos] == 'L') {
            text += 'L'; advance();
            return Token{TokType::LONGLITERAL, text, startLine, startCol};
        }
        return Token{TokType::INTLITERAL, text, startLine, startCol};
    }
    if (c == '"') return readString(startLine, startCol);
    if (c == '\'') return readChar(startLine, startCol);
    return readOperator(startLine, startCol);
}

void Scanner::advance() {
    if (pos < s.size()) {
        if (s[pos] == '\n') { line++; col = 1; }
        else col++;
        pos++;
    }
}

void Scanner::skipWhitespaceAndComments() {
    while (pos < s.size()) {
        char c = s[pos];
        if (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\f') {
            advance();
        } else if (c == '/' && pos + 1 < s.size() && s[pos+1] == '/') {
            while (pos < s.size() && s[pos] != '\n') advance();
        } else if (c == '/' && pos + 1 < s.size() && s[pos+1] == '*') {
            int sl = line, sc = col;
            advance(); advance();
            bool closed = false;
            while (pos < s.size()) {
                if (s[pos] == '*' && pos + 1 < s.size() && s[pos+1] == '/') {
                    advance(); advance(); closed = true; break;
                }
                advance();
            }
            if (!closed) errorAt(sl, sc, "unterminated block comment");
        } else break;
    }
}

void Scanner::errorAt(int l, int c, const std::string& msg) {
    errors.push_back(file + ":" + std::to_string(l) + ":" + std::to_string(c) + ": error: " + msg);
}

TokType Scanner::keywordType(const std::string& w) {
    if (w == "if") return TokType::KW_IF; if (w == "bool") return TokType::KW_BOOL;
    if (w == "break") return TokType::KW_BREAK; if (w == "import") return TokType::KW_IMPORT;
    if (w == "continue") return TokType::KW_CONTINUE; if (w == "else") return TokType::KW_ELSE;
    if (w == "false") return TokType::KW_FALSE; if (w == "for") return TokType::KW_FOR;
    if (w == "while") return TokType::KW_WHILE; if (w == "int") return TokType::KW_INT;
    if (w == "long") return TokType::KW_LONG; if (w == "return") return TokType::KW_RETURN;
    if (w == "len") return TokType::KW_LEN; if (w == "true") return TokType::KW_TRUE;
    if (w == "void") return TokType::KW_VOID;
    return TokType::IDENTIFIER;
}

bool Scanner::validCharEscape(char e) {
    return e == '"' || e == '\'' || e == '\\' || e == 't' || e == 'n' || e == 'r' || e == 'f';
}

Token Scanner::readString(int sl, int sc) {
    std::string text = "\""; advance();
    while (pos < s.size()) {
        char c = s[pos];
        if (c == '"') { text += c; advance(); return {TokType::STRINGLITERAL, text, sl, sc}; }
        if (c == '\\') {
            text += c; advance();
            if (pos >= s.size()) break;
            char e = s[pos];
            if (!validCharEscape(e)) errorAt(line, col, std::string("invalid escape: \\") + e);
            text += e; advance(); continue;
        }
        if (c == '\n') break;
        text += c; advance();
    }
    errorAt(sl, sc, "unterminated string literal");
    return {TokType::ERROR, text, sl, sc};
}

Token Scanner::readChar(int sl, int sc) {
    std::string text = "'"; advance();
    if (pos >= s.size()) { errorAt(sl, sc, "unterminated char"); return {TokType::ERROR, text, sl, sc}; }
    char c = s[pos];
    if (c == '\\') {
        text += c; advance();
        if (pos >= s.size()) { errorAt(sl, sc, "unterminated char"); return {TokType::ERROR, text, sl, sc}; }
        char e = s[pos];
        if (!validCharEscape(e)) errorAt(line, col, std::string("invalid escape: \\") + e);
        text += e; advance();
    } else if (c == '\'') {
        errorAt(line, col, "empty char"); advance(); text += '\''; return {TokType::ERROR, text, sl, sc};
    } else { text += c; advance(); }
    
    if (pos < s.size() && s[pos] == '\'') {
        text += '\''; advance(); return {TokType::CHARLITERAL, text, sl, sc};
    }
    errorAt(sl, sc, "unterminated char");
    return {TokType::ERROR, text, sl, sc};
}

Token Scanner::readOperator(int sl, int sc) {
    auto two = [&](char a, char b) { return pos + 1 < s.size() && s[pos] == a && s[pos+1] == b; };
    char c = s[pos];
    if (two('+','+')) { advance(); advance(); return {TokType::PLUSPLUS, "++", sl, sc}; }
    if (two('-','-')) { advance(); advance(); return {TokType::MINUSMINUS, "--", sl, sc}; }
    if (two('<','=')) { advance(); advance(); return {TokType::LE, "<=", sl, sc}; }
    if (two('>','=')) { advance(); advance(); return {TokType::GE, ">=", sl, sc}; }
    if (two('=','=')) { advance(); advance(); return {TokType::EQ, "==", sl, sc}; }
    if (two('!','=')) { advance(); advance(); return {TokType::NE, "!=", sl, sc}; }
    if (two('&','&')) { advance(); advance(); return {TokType::ANDAND, "&&", sl, sc}; }
    if (two('|','|')) { advance(); advance(); return {TokType::OROR, "||", sl, sc}; }
    if (two('+','=')) { advance(); advance(); return {TokType::PLUS_ASSIGN, "+=", sl, sc}; }
    if (two('-','=')) { advance(); advance(); return {TokType::MINUS_ASSIGN, "-=", sl, sc}; }
    if (two('*','=')) { advance(); advance(); return {TokType::STAR_ASSIGN, "*=", sl, sc}; }
    if (two('/','=')) { advance(); advance(); return {TokType::SLASH_ASSIGN, "/=", sl, sc}; }
    if (two('%','=')) { advance(); advance(); return {TokType::PERCENT_ASSIGN, "%=", sl, sc}; }

    TokType t;
    switch (c) {
        case '+': t = TokType::PLUS; break; case '-': t = TokType::MINUS; break;
        case '*': t = TokType::STAR; break; case '/': t = TokType::SLASH; break;
        case '%': t = TokType::PERCENT; break; case '<': t = TokType::LT; break;
        case '>': t = TokType::GT; break; case '!': t = TokType::NOT; break;
        case '=': t = TokType::ASSIGN; break; case '(': t = TokType::LPAREN; break;
        case ')': t = TokType::RPAREN; break; case '[': t = TokType::LBRACKET; break;
        case ']': t = TokType::RBRACKET; break; case '{': t = TokType::LBRACE; break;
        case '}': t = TokType::RBRACE; break; case ';': t = TokType::SEMI; break;
        case ',': t = TokType::COMMA; break; case '.': t = TokType::DOT; break;
        default: {
            std::string bad(1, c); errorAt(line, col, "unexpected char '" + bad + "'");
            advance(); return {TokType::ERROR, bad, sl, sc};
        }
    }
    advance(); return {t, std::string(1, c), sl, sc};
}