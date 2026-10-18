#include "parser.h"
#include <iostream>

Parser::Parser(std::vector<Token> toks, std::string filename)
    : toks(std::move(toks)), file(std::move(filename)) {}

void Parser::parseProgram() {
    while (!at(TokType::END)) {
        if (!parseTopLevelDecl() && !at(TokType::END)) advance();
    }
}

const Token& Parser::cur() const { return toks[p]; }
bool Parser::at(TokType t) const { return cur().type == t; }
bool Parser::atAny(std::initializer_list<TokType> ts) const {
    for (auto t : ts) if (at(t)) return true;
    return false;
}
void Parser::advance() { if (p + 1 < toks.size()) p++; }
void Parser::error(const std::string& msg) {
    errors = true;
    std::cerr << file << ":" << cur().line << ":" << cur().col << ": syntax error: " << msg << " (near '" << cur().text << "')\n";
}

bool Parser::expect(TokType t, const std::string& what) {
    if (at(t)) { advance(); return true; }
    error("expected " + what);
    return false;
}

bool Parser::parseTopLevelDecl() {
    if (at(TokType::KW_IMPORT)) { parseImport(); return true; }
    if (isTypeStart()) {
        parseType();
        if (!at(TokType::IDENTIFIER)) { error("expected identifier"); return false; }
        advance();
        if (at(TokType::LPAREN)) { parseMethodRest(); return true; }
        while (at(TokType::COMMA)) {
            advance();
            if (!expect(TokType::IDENTIFIER, "identifier")) return false;
        }
        return expect(TokType::SEMI, "';'");
    }
    error("expected import, field or method declaration");
    return false;
}

void Parser::parseImport() {
    advance(); expect(TokType::IDENTIFIER, "identifier"); expect(TokType::SEMI, "';'");
}

bool Parser::isTypeStart() const {
    return atAny({TokType::KW_INT, TokType::KW_LONG, TokType::KW_BOOL, TokType::KW_VOID});
}

bool Parser::isVarTypeStart() const {
    return atAny({TokType::KW_INT, TokType::KW_LONG, TokType::KW_BOOL});
}

void Parser::parseType() {
    if (!isTypeStart()) { error("expected type"); return; }
    advance();
    if (at(TokType::LBRACKET)) { advance(); expect(TokType::RBRACKET, "']'"); }
}

void Parser::parseMethodRest() {
    advance();
    if (!at(TokType::RPAREN)) {
        if (!parseParam()) return;
        while (at(TokType::COMMA)) { advance(); if (!parseParam()) return; }
    }
    if (expect(TokType::RPAREN, "')'")) parseBlock();
}

bool Parser::parseParam() {
    if (!isVarTypeStart()) { error("expected parameter type"); return false; }
    parseType();
    return expect(TokType::IDENTIFIER, "parameter name");
}

void Parser::parseBlock() {
    if (!expect(TokType::LBRACE, "'{'")) return;
    while (!at(TokType::RBRACE) && !at(TokType::END)) {
        if (isVarTypeStart()) {
            parseType();
            if (!expect(TokType::IDENTIFIER, "identifier")) return;
            while (at(TokType::COMMA)) { advance(); if (!expect(TokType::IDENTIFIER, "identifier")) return; }
            if (!expect(TokType::SEMI, "';'")) return;
        } else { parseStatement(); }
    }
    expect(TokType::RBRACE, "'}'");
}

void Parser::parseStatement() {
    if (at(TokType::KW_IF)) {
        advance(); expect(TokType::LPAREN, "'('"); parseExpr(); expect(TokType::RPAREN, "')'");
        parseBlock();
        if (at(TokType::KW_ELSE)) { advance(); parseBlock(); }
        return;
    }
    if (at(TokType::KW_WHILE)) {
        advance(); expect(TokType::LPAREN, "'('"); parseExpr(); expect(TokType::RPAREN, "')'"); parseBlock(); return;
    }
    if (at(TokType::KW_FOR)) {
        advance(); 
        expect(TokType::LPAREN, "'('"); 
        expect(TokType::IDENTIFIER, "identifier");
        expect(TokType::ASSIGN, "'='"); 
        parseExpr(); 
        
        if (at(TokType::COMMA) || at(TokType::SEMI)) { 
            advance(); 
        } else {
            error("expected ',' or ';' in for loop");
            return;
        }
        
        parseExpr(); 
        
        if (at(TokType::COMMA) || at(TokType::SEMI)) { 
            advance(); 
        } else {
            error("expected ',' or ';' in for loop");
            return;
        }
        
        if (!at(TokType::RPAREN)) { 
            parseLocation(); 
            if (atAny({TokType::PLUSPLUS, TokType::MINUSMINUS})) {
                advance();
            }
        }
        expect(TokType::RPAREN, "')'"); 
        parseBlock(); 
        return;
    }
    if (at(TokType::KW_RETURN)) {
        advance(); if (!at(TokType::SEMI)) parseExpr(); expect(TokType::SEMI, "';'"); return;
    }
    if (at(TokType::KW_BREAK))    { advance(); expect(TokType::SEMI, "';'"); return; }
    if (at(TokType::KW_CONTINUE)) { advance(); expect(TokType::SEMI, "';'"); return; }
    if (at(TokType::LBRACE))      { parseBlock(); return; }

    if (at(TokType::IDENTIFIER)) {
        size_t save = p;
        parseLocation();
        if (atAny({TokType::ASSIGN, TokType::PLUS_ASSIGN, TokType::MINUS_ASSIGN, TokType::STAR_ASSIGN, TokType::SLASH_ASSIGN, TokType::PERCENT_ASSIGN})) {
            advance(); parseExpr(); expect(TokType::SEMI, "';'"); return;
        }
        if (atAny({TokType::PLUSPLUS, TokType::MINUSMINUS})) {
            advance(); expect(TokType::SEMI, "';'"); return;
        }
        p = save; parseMethodCall(); expect(TokType::SEMI, "';'"); return;
    }
    error("expected statement"); advance();
}

void Parser::parseExpr() { parseAssign(); }
void Parser::parseAssign() { parseOr(); if (at(TokType::ASSIGN)) { advance(); parseAssign(); } }
void Parser::parseOr() { parseAnd(); while (at(TokType::OROR)) { advance(); parseAnd(); } }
void Parser::parseAnd() { parseEq(); while (at(TokType::ANDAND)) { advance(); parseEq(); } }
void Parser::parseEq() { parseRel(); while (atAny({TokType::EQ, TokType::NE})) { advance(); parseRel(); } }
void Parser::parseRel() { parseAdd(); while (atAny({TokType::LT, TokType::GT, TokType::LE, TokType::GE})) { advance(); parseAdd(); } }
void Parser::parseAdd() { parseMul(); while (atAny({TokType::PLUS, TokType::MINUS})) { advance(); parseMul(); } }
void Parser::parseMul() { parseUnary(); while (atAny({TokType::STAR, TokType::SLASH, TokType::PERCENT})) { advance(); parseUnary(); } }

void Parser::parseUnary() {
    if (atAny({TokType::NOT, TokType::MINUS})) { advance(); parseUnary(); return; }
    parsePrimary();
}

void Parser::parsePrimary() {
    if (atAny({TokType::INTLITERAL, TokType::LONGLITERAL, TokType::CHARLITERAL, TokType::BOOLEANLITERAL, TokType::STRINGLITERAL, TokType::KW_TRUE, TokType::KW_FALSE})) { advance(); return; }
    
    if (at(TokType::KW_INT) || at(TokType::KW_LONG)) {
        advance(); expect(TokType::LPAREN, "'('"); parseExpr(); expect(TokType::RPAREN, "')'"); return;
    }
    if (at(TokType::KW_LEN)) {
        advance(); expect(TokType::LPAREN, "'('"); expect(TokType::IDENTIFIER, "identifier"); expect(TokType::RPAREN, "')'"); return;
    }
    if (at(TokType::LPAREN)) { advance(); parseExpr(); expect(TokType::RPAREN, "')'"); return; }
    if (at(TokType::IDENTIFIER)) {
        size_t save = p; parseLocation();
        if (at(TokType::LPAREN)) { p = save; parseMethodCall(); return; }
        return;
    }
    error("expected expression");
}

void Parser::parseLocation() {
    if (!expect(TokType::IDENTIFIER, "identifier")) return;
    if (at(TokType::LBRACKET)) { advance(); parseExpr(); expect(TokType::RBRACKET, "']'"); }
}

void Parser::parseMethodCall() {
    if (!expect(TokType::IDENTIFIER, "identifier")) return;
    if (!expect(TokType::LPAREN, "'('")) return;
    if (!at(TokType::RPAREN)) {
        parseExternArg();
        while (at(TokType::COMMA)) { advance(); parseExternArg(); }
    }
    expect(TokType::RPAREN, "')'");
}

void Parser::parseExternArg() {
    if (at(TokType::STRINGLITERAL)) { advance(); return; }
    parseExpr();
}