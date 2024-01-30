/**
 * @file Project2.cpp
 * @author Christopher Dube
 * 
 * COSC 455 - 001
 * Project 2
 * 
 * NOTE ON VARIABLES:
 * You can change the following variables to display results desired:
 * >>> displayProc : 'true' will display when every procedure begings and ends. Useful for errors/debugging.
 * >>> displayLexical : 'true' will display lexical information as it is scanned. Useful for errors/debugging.
 * >>> displayAST : 'true' will display the complete abstract-syntax-tree after the syntax scan.
 * >>> analyzerType : choose whether to use Lexical scan or Syntax scan.
 * 
 * NOTE ON AST EXTRA-CREDIT:
 * I have implemented a complete abstract-syntax-tree and tested it vigorously.
 * It displays its nodes using in-order traversal.
 * 
 * NOTE ON ERROR DIAGNOSTICS EXTRA-CREDIT:
 * I'm unsure if I have implemented error diagnostics correctly, but
 * confirm that I have.
 * 
 * The command line parameter accepts one or multiple filenames, for example:
 * "java Project1 test1.txt"
 * "java Project1 test1.txt test2.txt test3.txt"
 * 
 * Also note that I used try/catch in each procedure to avoid null return values
 * from sub-ASTs that happens when the syntax analyzer fails.
 * 
 * This code contains the following classes:
 * AST              Abstract-syntax-tree data structure.
 * ASTNode          Nodes for the AST.
 * Position         Object that contains line and character positions.
 * Token            Object that contains kind, position, and value values.
 * ParseScanner     Wrapper for Scanner class to allow peeking.
 * Project2         Assignment.
 * 
 * 
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * class AST
 * Data structure for parser
 */
class AST {
    static List<String> treeNodes = new ArrayList<>(List.of(
        "DECL",
        "INT_LITERAL",
        "BOOL_LITERAL",
        "VARIABLE",
        "SEQ",
        "ASSIGNMENT",
        "IF",
        "WHILE",
        "OP_NOT",
        "OP_LT",
        "OP_EQLT",
        "OP_EQ",
        "OP_NOTEQ",
        "OP_GTEQ",
        "OP_GT",
        "OP_PLUS",
        "OP_MINUS",
        "OP_OR",
        "OP_MULT",
        "OP_DIV",
        "OP_AND",
        "PRINT",
        "PRINT_INT",
        "PRINT_BOOL"
        ));

    ASTNode root; // Root node of tree
    ASTNode currentNode; // Current node for sub-AST to link to (usually root of sub-tree)

    // Constructor
    public AST() {
        root = null;
        currentNode = null;
    }
    
    /**
     * add()
     * Adds either a node or a sub-tree to the existing AST.
     */
    public void add(ASTNode node) {
        // If AST is empty, set the root to given node
        if(root == null) {
            root = node;
            currentNode = node;
        } else {
            // Otherwise, determine where to link the node
            if(isTreeNode(node.kind)) {
                if(currentNode.child0 == null)
                    currentNode.child0 = node;
                else
                    currentNode.child1 = node;
                // Excluding IF's child2 'else' case
            }
        }
    }
    public void add(AST sub) {
        // If AST is empty, set the root to given node
        if(root == null) {
            root = sub.root;
            currentNode = sub.currentNode;
        } else {
            // Otherwise, link the sub-tree to the middle child of current node
            currentNode.child1 = sub.root;
            currentNode = sub.currentNode;
        }
    }
    
    /**
     * isTreeNode()
     * Determines if the node is derived from non-terminal symbol
     * by checking the static list of symbols.
     */
    boolean isTreeNode(String sym) {
        for(String s : treeNodes) // is a non-terminal symbol (VARIABLE, DECL, SEQ, IF, ...)
            if(s.equals(sym))
                return true;
        return false;
    }
}

/**
 * class ASTNode
 * Node object for the AST data structure
 * For simplicity and time constraints,
 * I did not encapsulate this class.
 */
class ASTNode {
    public String kind;
    public String pos;
    public ASTNode  child0, child1, child2;

    // Constructor
    public ASTNode() {
        kind = "";
        pos = "";
        child0 = null;
        child1 = null;
        child2 = null;
    }
    public ASTNode(String name, String apos) {
        kind = name;
        pos = apos;
        child0 = null;
        child1 = null;
        child2 = null;
    }
}

/**
 * class Position
 * 
 * Position stores the line and letter position
 * and overrides toString() to convert line and letter
 * integers to a string.
 */
class Position {
    int line;
    int letter;

    // Constructor
    public Position() {
        this.line = 1;
        this.letter = 0;
    }
    public Position(int line, int letter) {
        this.line = line;
        this.letter = letter;
    }

    @Override
    public String toString() {
        return "" + line + ":" + letter;
    }
};

/**
 * class Token
 * 
 * Token holds the position, kind, and value
 * of the current token being processed.
 */
class Token {
    Position position;
    String kind;
    String value;

    // Constructor
    public Token() {
        this.position = new Position();
    }
    public Token(Position position, String kind, String value) {
        this.position = position;
        this.kind = kind;
        this.value = value;
    }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getValue() { return value; }
    public Integer getValueInt() { return Integer.parseInt(value); }
    public void setValue(String value) { this.value = value; }
    public String getPosition() { return position.toString(); }
    public void setPositionPtr(Position position) { this.position = position; }
    public void setPosition(int line, int letter) { this.position.line = line; this.position.letter = letter; }
    public void incrementLine(int n) { this.position.line += n; }
    public void incrementLetter(int n) { this.position.letter += n; }
    public void setLine(int n) { this.position.line = n; }
    public void setLetter(int n) { this.position.letter = n; }
    public void decrementLine(int n) { this.position.line += n; }
    public void decrementLetter(int n) { this.position.letter += n; }
};


/**
 * class ParseScanner
 * 
 * ParseScanner is a wrapper for the Scanner class.
 * Scanner does not have a peek() method, so I made
 * a wrapper that allows it.
 * A StringBuilder is used as a buffer to hold characters
 * that were put back, usually be the peek() method.
 */
class ParseScanner {
    final Scanner scanner;
    StringBuilder buffer;

    // Constructor
    public ParseScanner(Scanner scan) {
        this.scanner = scan;
        this.scanner.useDelimiter("");
        this.buffer = new StringBuilder();
    }

    /**
     * next()
     * Checks the buffer first for characters,
     * then scans the input.
     */
    public char next() {
        // If buffer is empty, read from scanner
        // if(buffer.isEmpty()) {
        if(buffer.length() == 0) {
            return scanner.next().charAt(0);
        } else {
            // Otherwise read last char in buffer
            char c = buffer.charAt(buffer.length() - 1);
            buffer.setLength(buffer.length() - 1); // Clear buffer
            return c;
        }
    }
    
    /**
     * hasNext()
     * Checks the buffer first, then the input
     * for an available character.
     */
    public boolean hasNext() {
        // if(buffer.isEmpty()) {
        if(buffer.length() == 0) {
            // Buffer is empty, so check scanner
            return scanner.hasNext();
        // } else if(!buffer.isEmpty()) {
        } else if(buffer.length() != 0) {
            // Buffer not empty
            return true;
        }
        // Buffer and scanner are both empty
        return false;
    }
    
    /**
     * peek()
     * Returns the next character in the input
     * and puts it back, which goes into the buffer.
     * Peek() does not check that a character exists,
     * this must be done prior to calling peek().
     */
    public char peek() {
        // Read from scanner
        char c = this.next();

        // Putback (into buffer)
        putback(c);

        // Return char
        return c;
    }
    
    /**
     * putback()
     * Appends the given character to the buffer.
     */
    public void putback(char c) {
        // Put char at end of string
        buffer.append(c);
    }
    
    /**
     * skipLine()
     * Clears the buffer and reads the next line
     * without returning it.
     */
    public void skipLine() {
        // Clear buffer and nextline
        buffer.setLength(0);
        scanner.nextLine();
    }
    
    /**
     * close()
     * Closes the scanner object.
     */
    public void close() {
        scanner.close();
    }
}

/**
 * class Project2
 * 
 */
public class Project2 {
    // Main
    public static void main(String[] args) {
        for(String source : args) {
            new Project2().run(source);
        }
    }

    // Enumeration
    enum AnalyzerType {
        Lexical,
        Syntax
    }

    // Variables
    public String filename;
    public final String DISP_FORMAT = "%-9s%-9s%-8s";
    public final String AST_DISP_FORMAT = "%-13s%-8s%-13s%-13s%-13s";
    public final String EOF = "end-of-text";
    public ParseScanner scan;
    public char c;
    public Position pos;
    public Token token;
    public ArrayList<String> symbolTable;
    public boolean hasError = false;

    // Optional variables for debugging and information (hard-coded)
    public boolean displayLexical = false; // Displays lexical symbols (position, kind, value)
    public boolean displayProc = false; // Display Begin/End procedure visits (for tracing)
    public boolean displayAST = true; // Display full AST after syntax analyzer scan
    public AnalyzerType analyzerType = AnalyzerType.Syntax;

    // Constructor
    public Project2() {}

    /**
     * run()
     * Primary function of the class.
     */
    public void run(String filename) {
        // Setup scanner
        try { scan = new ParseScanner(new Scanner(new File(filename)));}
        catch(FileNotFoundException e) { System.out.println("Error: File not found."); return; }

        // Create variables
        createSymbolTable();
        pos = new Position();
        token = new Token();

        // Determine which analyzer to run
        switch(analyzerType) {
            case Lexical: lexical(filename); break;
            case Syntax: syntax(filename); break;
            default: System.out.println("No analyzer selected. Set 'analyzerType' variable");
        }

        scan.close();
    } // End run()
    
    /**
     * lexical()
     * Performs lexical analysis scan on file.
     * Creates 
     */
    public void lexical(String filename) {
        System.out.println("Starting lexical analysis scan on \'" + filename + "\'...");
        
        // Output heading for tokens
        System.out.format(DISP_FORMAT, "Position", "Kind", "Value");
        System.out.println("");

        // Main loop
        while(kind() != EOF) {
            // Get next token
            next();
            if(hasError) break;
            print(position(), kind(), value());
        }

        if(!hasError)
            System.out.println("Lexical scan completed successfully.\n");
    }

    public void syntax(String filename) {
        System.out.println("Starting syntax analysis scan on \'" + filename + "\'...");

        AST ast = new AST();

        next();
        ast = program(EOF);

        if(!hasError)
            System.out.println("Syntax scan completed successfully.\n");

        // Display AST
        if(!hasError && displayAST) {
            System.out.println("Abstract Syntax Tree (Using In-Order Traversal):");
            System.out.format(AST_DISP_FORMAT, "KIND", "POS", "CHILD0", "CHILD1", "CHILD2");
            System.out.println("");
            inorderTraversal(ast.root);
        }

        // Display true or false per instructions
        if(!hasError)
            System.out.println("\n\n   TRUE\n\n");
        else
            System.out.println("\n\n   FALSE\n\n");
    }
    
    /**
     * inorderTraversal()
     * Traverses the AST beginning at specified node (root for entire AST)
     * and displays the elements by in-order traversal.
     */
    void inorderTraversal(ASTNode node) {
        if(node == null) return;

        // Travel child0
        if(node.child0 != null)
            inorderTraversal(node.child0);
        
        // Print kind of node
        System.out.format(AST_DISP_FORMAT,
                            node.kind,
                            node.pos,
                            ((node.child0 != null) ? node.child0.kind : "NIL"),
                            ((node.child1 != null) ? node.child1.kind : "NIL"),
                            ((node.child2 != null) ? node.child2.kind : "NIL"));
        System.out.println("");
        //System.out.println(
                            // "Node: Kind: " +
                            // node.kind +
                            // " Pos: " +
                            // node.pos +
                            // " Child0: " + 
                            // ((node.child0 != null) ? node.child0.kind : "NIL") +
                            // " Child1: " +
                            // ((node.child1 != null) ? node.child1.kind : "NIL") +
                            // " Child2: " +
                            // ((node.child2 != null) ? node.child2.kind : "NIL"));

        // Travel child1
        if(node.child1 != null)
            inorderTraversal(node.child1);

        // Travel child2
        if(node.child2 != null)
            inorderTraversal(node.child2);
    }
    
// -----------------------------------------------------------------------------------------------
// --------------------------- Syntax Analysis Scan Functions ------------------------------------
// -----------------------------------------------------------------------------------------------
// ------------------------------------ Project 2 Code -------------------------------------------
// -----------------------------------------------------------------------------------------------

    void match(String... symbols) {
        // Check if current symbol is in list of required symbols
        for(String s : symbols) {
            if(kind().equals(s)) {
                print(position(), kind(), value());
                next();
                return; // Found, so return. Testing no longer needed.
            }
        }

        // Otherwise, generate an error
        if(!hasError) {
            genError(position(), "Expected " + getElements(symbols) + ", but found \'" + kind() + "\'");
            hasError = true;
        }
    }
    AST program(String... follow) { // "program" Identifier ":" Body "end"
        if(hasError) return null;
        outputProc("Begin: Program");
        AST ast = new AST();
        try {

            match("program");
            match("ID");
            match(":");
            ast = body("end");
            match("end");

        } catch(Exception e) {} // Exception will be null return of sub-ast due to syntax error
        outputProc("End: Program");
        return ast;
    }
    AST body(String... follow) { // [ Declarations ] Statements
        if(hasError) return null;
        outputProc("Begin: Body");
        AST ast = new AST();
        try {

            if(csymBelongsTo("bool", "int")) {
                ast = declarations();
            }

            ast.add(statements());

        } catch(Exception e) {} // Exception will be null return of sub-ast due to syntax error
        outputProc("End: Body");
        return ast;
    }
    AST declarations(String... follow) { // Declaration { Declaration }
        if(hasError) return null;
        outputProc("Begin: Declarations");
        AST ast = new AST();
        try {

            ast = declaration();

            while(csymBelongsTo("bool", "int")) {
                ast.add(declaration());
            }

        } catch(Exception e) {} // Exception will be null return of sub-ast due to syntax error
        outputProc("End: Declarations");
        return ast;
    }
    AST declaration(String... follow) { // ( "bool" | "int" ) Identifier ";"
        if(hasError) return null;
        outputProc("Begin: Declaration");
        AST ast = new AST();
        try {

            print(position(), kind(), value());

            ASTNode declNode = new ASTNode("DECL", position());
            ASTNode litNode;

            if(kind().equals("bool"))
                litNode = new ASTNode("bool", position());
            else if(kind().equals("int"))
                litNode = new ASTNode("int", position());
            else
                litNode = null; // Should not happen
            
            next();

            ASTNode idNode = new ASTNode(value(), position());
            match("ID");
            
            ASTNode seqNode = new ASTNode("SEQ", position());
            match(";");
            
            declNode.child0 = idNode;
            declNode.child1 = litNode;
            seqNode.child0 = declNode;
            ast.add(seqNode);

        } catch(Exception e) {} // Exception will be null return of sub-ast due to syntax error
        outputProc("End: Declaration");
        return ast;
    }
    AST statements(String... follow) { // Statement { ";" Statement }
        if(hasError) return null;
        outputProc("Begin: Statements");
        AST ast = new AST();
        try {

            ASTNode seqNode = null;
            ast = statement();

            while(csymBelongsTo(";")) {
                print(position(), kind(), value());

                seqNode = new ASTNode("SEQ", position());
                seqNode.child0 = ast.root;
                ast.root = seqNode;
                ast.currentNode = seqNode;

                next();
                ast.add(statement());
            }

        } catch(Exception e) {} // Exception will be null return of sub-ast due to syntax error
        outputProc("End: Statements");
        return ast;
    }
    AST statement(String... follow) { // AssignmentStatement | ConditionalStatement | IterativeStatement | PrintStatement
        if(hasError) return null;
        outputProc("Begin: Statement");
        AST ast = new AST();
        try {
            if(csymBelongsTo("ID")) {
                ast = assignmentStatement();
            }
            else if(csymBelongsTo("if")) {
                ast = conditionalStatement();
            }
            else if(csymBelongsTo("while")) {
                ast = iterativeStatement();
            }
            else if(csymBelongsTo("print")) {
                ast = printStatement();
            }
            else
                genError(position(), "Expected {ID, if, while, print}, but found \'" + kind() + "\'");
        
            } catch(Exception e) {} // Exception will be null return of sub-ast due to syntax error
        outputProc("End: Statement");
        return ast;
    }
    AST assignmentStatement(String... follow) { // Identifier ":=" Expression
        if(hasError) return null;
        outputProc("Begin: AssignmentStatement");
        AST ast = new AST();
        try {

            ASTNode varNode = new ASTNode("VARIABLE", position());
            ASTNode idNode = new ASTNode(value(), position());
            varNode.child0 = idNode;

            match("ID");
            ASTNode asmtNode = new ASTNode("ASSIGNMENT", position());
            asmtNode.child0 = varNode;
            match(":=");

            asmtNode.child1 = expression().root;
            ast.root = asmtNode;
            ast.currentNode = asmtNode;

        } catch(Exception e) {} // Exception will be null return of sub-ast due to syntax error
        outputProc("End: AssignmentStatement");
        return ast;
    }
    AST conditionalStatement(String... follow) { // "if" Expression "then" Body [ "else" Body ] "fi"
        if(hasError) return null;
        outputProc("Begin: ConditionalStatement");
        AST ast = new AST();
        try {

            ASTNode ifNode = new ASTNode("IF", position());
            match("if");
            ifNode.child0 = expression().root; // Condition

            match("then");
            ifNode.child1 = body().root; // True branch

            if(csymBelongsTo("else")) {
                match("else");
                ifNode.child2 = body().root; // False branch
            }

            match("fi");
            ast.root = ifNode;
            ast.currentNode = ifNode;

        } catch(Exception e) {} // Exception will be null return of sub-ast due to syntax error
        outputProc("End: ConditionalStatement");
        return ast;
    }
    AST iterativeStatement(String... follow) { // "while" Expression "do" Body "od"
        if(hasError) return null;
        outputProc("Begin: IterativeStatement");
        AST ast = new AST();
        try {

            ASTNode whileNode = new ASTNode("WHILE", position());

            match("while");
            whileNode.child0 = expression("do").root; // Condition
            match("do");

            whileNode.child1 = body("od").root; // True branch (iteration)
            match("od");

            ast.root = whileNode;
            ast.currentNode = whileNode;
        
        } catch(Exception e) {} // Exception will be null return of sub-ast due to syntax error
        outputProc("End: IterativeStatement");
        return ast;
    }
    AST printStatement(String... follow) { // "print" Expression
        if(hasError) return null;
        outputProc("Begin: PrintStatement");
        AST ast = new AST();
        try {

            ASTNode printNode = new ASTNode("PRINT", position());
            match("print");
            printNode.child0 = expression().root;
            ast.root = printNode;
            ast.currentNode = printNode;

        } catch(Exception e) {} // Exception will be null return of sub-ast due to syntax error
        outputProc("End: PrintStatement");
        return ast;
    }
    AST expression(String... follow) { // SimpleExpression [ RelationalOperator SimpleExpression ]
        if(hasError) return null;
        outputProc("Begin: Expression");
        AST ast = new AST();
        try {

            ASTNode opNode = null;
            AST tempSimpExpr = simpleExpression();

            if(csymBelongsTo("<", "=<", "=", "!=", ">=", ">")) {
                print(position(), kind(), value());
                switch(kind()) {
                    case "<": opNode = new ASTNode("OP_LT", position()); break;
                    case "=<": opNode = new ASTNode("OP_EQLT", position()); break;
                    case "=": opNode = new ASTNode("OP_EQ", position()); break;
                    case "!=": opNode = new ASTNode("OP_NOTEQ", position()); break;
                    case ">=": opNode = new ASTNode("OP_GTEQ", position()); break;
                    case ">": opNode = new ASTNode("OP_GT", position()); break;
                }
                next();

                opNode.child0 = tempSimpExpr.root;
                opNode.child1 = simpleExpression().root;
                ast.root = opNode;
                ast.currentNode = opNode;
            }
            if(opNode == null) {
                ast = tempSimpExpr;
            }

        } catch(Exception e) {} // Exception will be null return of sub-ast due to syntax error
        outputProc("End: Expression");
        return ast;
    }
    AST simpleExpression(String... follow) { // Term { AdditiveOperator Term }
        if(hasError) return null;
        outputProc("Begin: SimpleExpression");
        AST ast = new AST();
        try {

            ASTNode addNode = null;
            AST tempTermAST = term();

            while(csymBelongsTo("+", "-", "or")) {
                print(position(), kind(), value());

                switch(kind()) {
                    case "+": addNode = new ASTNode("OP_PLUS", position()); break;
                    case "-": addNode = new ASTNode("OP_MINUS", position()); break;
                    case "or": addNode = new ASTNode("OP_OR", position()); break;
                    default: addNode = null;
                }

                addNode.child0 = tempTermAST.root;
                next();

                addNode.child1 = term().root;
                ast.root = addNode;
                ast.currentNode = addNode;
            }
            if(addNode == null) {
                ast = tempTermAST;
            }

        } catch(Exception e) {} // Exception will be null return of sub-ast due to syntax error
        outputProc("End: SimpleExpression");
        return ast;
    }
    AST term(String... follow) { // Factor { MultiplicativeOperator Factor }
        if(hasError) return null;
        outputProc("Begin: Term");
        AST ast = new AST();
        try {

            ASTNode mulNode = null;
            AST tempFactor = factor();

            while(csymBelongsTo("*", "/", "and")) {
                print(position(), kind(), value());

                switch(kind()) {
                    case "*": mulNode = new ASTNode("OP_MULT", position()); break;
                    case "/": mulNode = new ASTNode("OP_DIV", position()); break;
                    case "and": mulNode = new ASTNode("OP_AND", position()); break;
                    default: mulNode = null;
                }
                mulNode.child0 = tempFactor.root;
                next();

                mulNode.child1 = factor().root;
                ast.root = mulNode;
                ast.currentNode = mulNode;
            }
            if(mulNode == null) {
                ast = tempFactor;
            }

        } catch(Exception e) {} // Exception will be null return of sub-ast due to syntax error
        outputProc("End: Term");
        return ast;
    }
    AST factor(String... follow) { // [ UnaryOperator ] ( Literal | Identifier | "(" Expression ")" )
        if(hasError) return null;
        outputProc("Begin: Factor");
        AST ast = new AST();
        try {

            ASTNode notNode = null;
            ASTNode varNode = null;
            ASTNode idNode = null;
            if(csymBelongsTo("-", "not")) {
                print(position(), kind(), value());

                notNode = new ASTNode("OP_NOT", position());
                ast.root = notNode;
                ast.currentNode = notNode;
                next();
            }
            if(csymBelongsTo("true", "false", "NUM")) {
                if(notNode == null)
                    ast = literal();
                else
                    notNode.child0 = literal().root;
            } else if(csymBelongsTo("ID")) {
                print(position(), kind(), value());

                varNode = new ASTNode("VARIABLE", position());
                idNode = new ASTNode(value(), position());
                varNode.child0 = idNode;

                if(notNode == null) { // NOT does not exist
                    ast.root = varNode;
                    ast.currentNode = varNode;
                } else { // NOT exists, so attach as child
                    notNode.child0 = varNode;
                }
                next();
            } else if(csymBelongsTo("(")) {
                print(position(), kind(), value());

                next();
                if(ast.root == null) { // NOT does not exist
                    ast = expression();
                } else { // NOT exists, so attach as child
                    notNode.child0 = expression().root;
                }

                match(")", "+", "-", "or", "*", "/", "and");
            } else
                genError(position(), "Expected { true, false, NUM, ID, ( , -, not }, but found \'" + kind() + "\'");

        } catch(Exception e) {} // Exception will be null return of sub-ast due to syntax error
        outputProc("End: Factor");
        return ast;
    }
    AST literal(String... follow) { // BooleanLiteral | IntegerLiteral
        if(hasError) return null;
        outputProc("Begin: Literal");
        AST ast = new AST();
        try {

            ASTNode litNode = null;
            ASTNode valNode = null;

            if(csymBelongsTo("true", "false")) {
                litNode = new ASTNode("BOOL_LITERAL", position());
                valNode = new ASTNode(kind(), position());
            }
            else if(csymBelongsTo("NUM")) {
                litNode = new ASTNode("INT_LITERAL", position());
                valNode = new ASTNode(value(), position());
            }

            litNode.child0 = valNode;
            ast.root = litNode;
            ast.currentNode = litNode;
            match("true", "false", "NUM");

        } catch(Exception e) {} // Exception will be null return of sub-ast due to syntax error
        outputProc("End: Literal");
        return ast;
    }
    // void relationalOperator() { } // "<" | "=<" | "=" | "!=" | ">=" | ">"         NOT NEEDED
    // void additiveOperator() { } // "+" | "-" | "or"                               NOT NEEDED
    // void multiplicativeOperator() { } // "*" | "/" | "and"                        NOT NEEDED
    // void unaryOperator() { } // "-" | "not"                                       NOT NEEDED
    // void booleanLiteral(String... follow) { } // "false" | "true"                 NOT NEEDED
    // void integerLiteral(String... follow) { } // "NUM"                            NOT NEEDED
    // void identifier(String... follow) { } // "ID"                                 NOT NEEDED
    // void digit() { } //                                                           NOT NEEDED
    // void letter() { } //                                                          NOT NEEDED


// -----------------------------------------------------------------------------------------------
// -------------------------------- End Syntax Scan ----------------------------------------------
// -----------------------------------------------------------------------------------------------


    // Methods (from Project 1)
    
    /**
     * next()
     * Method required by assignment.
     * Processes the next token and stores the
     * information in a Token object.
     */
    void next() {
        // If no more characters to read, generate EOF token and return
        if(!scan.hasNext()) {
            token.setKind(EOF);
            token.setValue("");
            token.setPosition(pos.line, pos.letter + 1);
            return;
        }

        // Remove any whtiespace before reading token
        remWS();

        // Scan next character
        c = scan.next();
        pos.letter ++;

        if(isLetter(c)) { // Letter (keyword or identifier)
            StringBuilder str = new StringBuilder();
            int len = 0;
            token.setPosition(pos.line, pos.letter);

            // Scan until not letter | digit | _
            while(isLetter(c) || isDigit(c) || c == '_') {
                str.append(c);
                
                if(!scan.hasNext()) break;

                c = scan.next();
                len ++;
            }

            // Put character back if its not part of the identifier
            if(!(isLetter(c) || isDigit(c) || c == '_')) {
                scan.putback(c);
                len --;
            }
            
            // Check if token is identifier or keyword and generate the token
            String s = str.toString();
            if(symbolTable.contains(s)) { // Keyword
                token.setKind(s);
                token.setValue("");

            } else { // Identifier
                token.setKind("ID");
                token.setValue(s);
            }

            pos.letter += len;
        }
        
        else if(isDigit(c)) { // Digit
            StringBuilder str = new StringBuilder();
            int len = 0;
            token.setPosition(pos.line, pos.letter);

            while(isDigit(c)) {
                str.append(c);
                
                if(!scan.hasNext()) break;
                
                c = scan.next();
                len ++;
            }

            // Put character back if its not part of the integer
            if(!isDigit(c)) {
                scan.putback(c);
                len --;
            }

            // Generate the token
            token.setKind("NUM");
            token.setValue(str.toString());
            pos.letter += len;
        }
        
        else if(c == '/') { // Divide Symbol (comment or division)
            if(scan.hasNext() && (scan.peek() == '/')) { // Comment
                scan.skipLine(); // Get rid of remainder of line
                pos.line ++;
                pos.letter = 0;
                next(); // Found a comment, not a token. So continue looking for next token

            } else { // Division
                token.setKind("/");
                token.setValue("");
                token.setPosition(pos.line, pos.letter);
            }
        }

        else if(c == '<') { // <
            token.setKind("<");
            token.setValue("");
            token.setPosition(pos.line, pos.letter);
        }

        else if(c == '>') { // > or >=
            token.setPosition(pos.line, pos.letter);

            // Check if token is > or >= and generate it
            if(scan.peek() == '=') { // >=
                // must be after and followed by letter or digit
                token.setKind(">=");
                token.setValue("");
                scan.next(); // Skip this char, we know its '='
                pos.letter ++;

            } else { // >
                // must be after and followed by letter or digit
                token.setKind(">");
                token.setValue("");
            }
        }

        else if(c == '=') { // =, =<
            token.setPosition(pos.line, pos.letter);

            // Check if token is = or =< and generate it
            if(scan.hasNext() && ( scan.peek() == '<') ){ // =<
                token.setKind("=<");
                token.setValue("");
                scan.next(); // Skip this char
                pos.letter ++;

            } else { // =
                token.setKind("=");
                token.setValue("");
            }
        }

        else if(c == '!') { // !=
            // Check if token is !=, otherwise generate error
            if(scan.hasNext() && ( scan.peek() == '=') ) {
                token.setKind("!=");
                token.setValue("");
                token.setPosition(pos.line, pos.letter);
                scan.next();
            } else {
                genError(pos.toString(), "Illegal token: \'!\', missing \'=\'?");
            }
        }

        else if(c == '_') { // Nothing will begin with _
            genError(pos.toString(), "Illegal token: \'_\', used in identifier?");
        }

        else if(c == '+') {
            token.setKind("+");
            token.setValue("");
            token.setPosition(pos.line, pos.letter);
        }

        else if(c == '-') { // AdditiveOperator or UnaryOperator
            token.setKind("-");
            token.setValue("");
            token.setPosition(pos.line, pos.letter);
        }

        else if(c == '*') {
            token.setKind("*");
            token.setValue("");
            token.setPosition(pos.line, pos.letter);
        }

        else if(c == '(') {
            token.setKind("(");
            token.setValue("");
            token.setPosition(pos.line, pos.letter);
        }

        else if(c == ')') {
            token.setKind(")");
            token.setValue("");
            token.setPosition(pos.line, pos.letter);
        }

        else if(c == ';') {
            token.setKind(";");
            token.setValue("");
            token.setPosition(pos.line, pos.letter);
        }

        else if(c == ':') { // : or :=
            token.setPosition(pos.line, pos.letter);

            // Check if token is : or := and generate it
            if(scan.hasNext() && ( scan.peek() == '=') ) { // :=
                token.setKind(":=");
                token.setValue("");
                scan.next(); // Skip this char, we know it's '='
                pos.letter ++;

            } else { // :
                token.setKind(":");
                token.setValue("");
            }
        }

        else { // Symbol not in language
            genError(pos.toString(), "Symbol \'" + c + "\' not allowed");
        }

        // Remove whitespace after this token
        remWS();
    } // End next()
    
    /**
     * kind(), value(), position()
     * As required by rubric.
     * Also includes value_int() for case kind == "NUM"
     */
    String kind() { return token.getKind(); }

    String value() { return token.getValue(); } // Return String value

    Integer value_int() { return token.getValueInt(); } // Return Int value

    String position() { return token.getPosition(); }
    
    /**
     * print()
     * Displays the current token's information
     * Only display if no error
     */
    void print(String position, String kind , String value) {
        if(hasError || !displayLexical) return;
        System.out.format(DISP_FORMAT, position, kind, value);
        System.out.println("");
    }

    /**
     * print()
     * Displays the current token's information, from the Token object
     * Only display if no error
     */
    void print(Token t) {
        //if(hasError || !displayLexical) return;
        System.out.format(DISP_FORMAT, t.getPosition(), t.getKind(), t.getValue());
        System.out.println("");
    }
    
    /**
     * isLetter()
     * Checks if the char is a letter (uppercase or lowercase)
     */
    public boolean isLetter(char c) {
        return ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) ? true : false;
    }
    
    /**
     * isDigit()
     * Checks if the char is a digit
     */
    public boolean isDigit(char c) {
        return (c >= '0' && c <= '9') ? true : false;
    }
    
    /**
     * createSymbolTable()
     * Adds all keywords to an array that will be checked
     * to see if current token is a keyword.
     */
    public void createSymbolTable() {
        symbolTable = new ArrayList<String>();
        symbolTable.add("program");
        symbolTable.add("bool");
        symbolTable.add("end");
        symbolTable.add("int");
        symbolTable.add("if");
        symbolTable.add("then");
        symbolTable.add("else");
        symbolTable.add("fi");
        symbolTable.add("while");
        symbolTable.add("do");
        symbolTable.add("od");
        symbolTable.add("print");
        symbolTable.add("or");
        symbolTable.add("and");
        symbolTable.add("not");
        symbolTable.add("false");
        symbolTable.add("true");
    }
    
    /**
     * genError()
     * Outputs text describing an error, including the position and possible token
     */
    public void genError(String position, String message) {
        System.out.println("Error: at " + position + ", " + message);
        System.out.println("Task ended due to error.\n");
        hasError = true;
    }

    /**
     * remWS()
     * Removes all while space between current location in file to next non-whitespace location.
     */
    void remWS() {
        if(!scan.hasNext()) return;
        char w = scan.peek();
        while(scan.hasNext() && (w == '\n' || w == '\t' || w == '\r' || w == ' ')) {
            scan.next();
            pos.letter ++;

            if(w == '\n') { // || w == '\r'
                pos.line ++;
                pos.letter = 0;
            }

            if(!scan.hasNext()) return;

            w = scan.peek();
        }
    }
    
    /**
     * getElements()
     * Returns array of strings as a single string.
     */
    String getElements(String[] elements) {
        StringBuilder s = new StringBuilder("{ ");
        for(String e : elements)
            s.append(e + ", ");
        //s = s.substring(0, s.length() - 2); // Get rid of last ", "
        s.delete(s.length() - 2, s.length() - 1);
        s.append("}");
        return s.toString();
    }
    
    /**
     * belongsTo()
     * Verifies that the symbol belongs to an array of symbols.
     */
    boolean csymBelongsTo(String... symbols) {
        for(String s : symbols) {
            if(kind().equals(s)) {
                return true; // Found, so return. Testing no longer needed.
            }
        }
        return false;
    }
    
    /**
     * outputProc()
     * Displays the current state of execution for syntax
     * only if there is no error.
     */
    void outputProc(String info) {
        if(!hasError && displayProc) // Prevents displaying procedures after error is found
            System.out.println(info);
    }
}
