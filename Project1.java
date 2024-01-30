/**
 * @file Project1.cpp
 * @author Christopher Dube
 * 
 * COSC 455 - 001
 * Project 1
 * 
 * The command line parameter accepts one or multiple filenames, for example:
 * "java Project1 test1.txt"
 * "java Project1 test1.txt test2.txt test3.txt"
 * 
 * This code has the following classes:
 * class Position       (starts at line 31)     Holds position of line and letter
 * class Token          (starts at line 57)     Holds position, kind, and value
 * class ParseScanner   (starts at line 98)     Wrapper for Scanner to allow peeking
 * class Project1       (starts at line 194)    Code for project
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.ArrayList;

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
 * class Project1
 * 
 */
public class Project1 {
    // Main
    public static void main(String[] args) {
        for(String source : args) {
            new Project1().run(source);
        }
        //new Project1().run();
    }
    
    // Variables
    public String filename;
    public final String DISP_FORMAT = "%-9s%-9s%-8s";
    public final String EOF = "end-of-text";
    public ParseScanner scan;
    public char c;
    public Position pos;
    public Token token;
    public ArrayList<String> symbolTable;
    
    // Constructor
    public Project1() {}

    /**
     * run()
     * Primary function of the class.
     */
    public void run(String filename) {
        System.out.println("Starting scan on \'" + filename + "\'...");
        // Setup scanner
        try { scan = new ParseScanner(new Scanner(new File(filename)));}
        catch(FileNotFoundException e) { System.out.println("Error: File not found."); return; }

        // Create variables
        createSymbolTable();
        pos = new Position();
        token = new Token();
        
        // Output heading for tokens
        System.out.format(DISP_FORMAT, "Position", "Kind", "Value");
        System.out.println("");

        // Main loop
        while(kind() != EOF) {
            // Get next token
            next();
            print(position(), kind(), value());
        }

        System.out.println("Completed successfully.\n");
        scan.close();
    } // End run()
    
    // Methods
    
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
                next();

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
     */
    void print(String position, String kind , String value) {
        System.out.format(DISP_FORMAT, position, kind, value);
        System.out.println("");
    }

    /**
     * print()
     * Displays the current token's information, from the Token object
     */
    void print(Token t) {
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
        System.out.println("\nTask ended due to error.");
        scan.close();
        System.exit(0);
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
}
