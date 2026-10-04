#pragma once
#include <memory>
#include <string>
#include <vector>

enum class TypeName { Int, Long, Bool };

enum class AssignOp { Assign, PlusAssign, MinusAssign, StarAssign, SlashAssign, PercentAssign };

enum class BinOp {
    Add,
    Sub,
    Mul,
    Div,
    Rem,
    Less,
    LessEqual,
    Greater,
    GreaterEqual,
    Equal,
    NotEqual,
    LogicalAnd,
    LogicalOr,
};

enum class UnaryOp { Negate, LogicalNot };

struct Type {
    TypeName    name    = TypeName::Int;
    bool        isArray = false;
    std::string arraySize;
};

struct Expression {
    virtual ~Expression() = default;
};

struct IntExpression : Expression {
    std::string raw;
};

struct LongExpression : Expression {
    std::string raw;
};

struct BoolExpression : Expression {
    bool value;
};

struct CharExpression : Expression {
    std::string raw;
};

struct StringExpression : Expression {
    std::string raw;
};

struct Location {
    std::string                 name;
    std::unique_ptr<Expression> index;
};

struct LocationExpression : Expression {
    Location location;
};

struct CallExpression : Expression {
    std::string                              name;
    std::vector<std::unique_ptr<Expression>> arguments;
};

struct CastExpression : Expression {
    TypeName                    type;
    std::unique_ptr<Expression> argument;
};

struct LenExpression : Expression {
    std::string name;
};

struct UnaryExpression : Expression {
    UnaryOp                     op;
    std::unique_ptr<Expression> operand;
};

struct BinaryExpression : Expression {
    BinOp                       op;
    std::unique_ptr<Expression> left;
    std::unique_ptr<Expression> right;
};

struct VariableDeclaration {
    Type        type;
    std::string name;
};

struct FieldDeclaration {
    Type        type;
    std::string name;
};

struct Parameter {
    Type        type;
    std::string name;
};

struct Statement {
    virtual ~Statement() = default;
};

struct Block {
    std::vector<VariableDeclaration>        variables;
    std::vector<std::unique_ptr<Statement>> statements;
};

struct AssignStatement : Statement {
    Location                    target;
    AssignOp                    op = AssignOp::Assign;
    std::unique_ptr<Expression> value;
};

struct IncrementStatement : Statement {
    Location target;
    bool     increment = true;
};

struct CallStatement : Statement {
    std::string                              name;
    std::vector<std::unique_ptr<Expression>> arguments;
};

struct IfStatement : Statement {
    std::unique_ptr<Expression> condition;
    std::unique_ptr<Block>      thenBlock;
    std::unique_ptr<Block>      elseBlock;
};

struct Update {
    Location                    location;
    AssignOp                    op = AssignOp::Assign;
    std::unique_ptr<Expression> value;
    bool                        increment = false;
    bool                        isUp      = true;
};

struct ForStatement : Statement {
    std::string                 index;
    std::unique_ptr<Expression> init;
    std::unique_ptr<Expression> condition;
    Update                      update;
    std::unique_ptr<Block>      body;
};

struct WhileStatement : Statement {
    std::unique_ptr<Expression> condition;
    std::unique_ptr<Block>      body;
};

struct ReturnStatement : Statement {
    std::unique_ptr<Expression> value;
};

struct BreakStatement : Statement {};

struct ContinueStatement : Statement {};

struct MethodDeclaration {
    bool                   isVoid = false;
    Type                   returnType;
    std::string            name;
    std::vector<Parameter> parameters;
    std::unique_ptr<Block> body;
};

struct Program {
    std::vector<std::string>       imports;
    std::vector<FieldDeclaration>  fields;
    std::vector<MethodDeclaration> methods;
};
