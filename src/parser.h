#pragma once
#include <vector>
#include <string>
#include <initializer_list>
#include "scanner.h"

class Parser {
public:
    Parser(std::vector<Token> toks, std::string filename);
    void parseProgram();
    bool errors = false;

private:
    std::vector<Token> toks;
    std::string file;
    size_t p = 0;

    const Token& cur() const;
    bool at(TokType t) const;
    bool atAny(std::initializer_list<TokType> ts) const;
    void advance();
    void error(const std::string& msg);
    bool expect(TokType t, const std::string& what);

    bool parseTopLevelDecl();
    void parseImport();
    bool isTypeStart() const;
    bool isVarTypeStart() const; 
    void parseType();
    void parseMethodRest();
    bool parseParam();
    void parseBlock();
    void parseStatement();
    
    void parseExpr();
    void parseAssign();
    void parseOr();
    void parseAnd();
    void parseEq();
    void parseRel();
    void parseAdd();
    void parseMul();
    void parseUnary();
    void parsePrimary();
    void parseLocation();
    void parseMethodCall();
    void parseExternArg();
};