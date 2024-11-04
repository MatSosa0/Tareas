
import java.io.*;
import java.util.regex.*;

public class JsonLexer {

    // Definimos las expresiones regulares para cada token
    private static final String L_CORCHETE = "\\[";
    private static final String R_CORCHETE = "\\]";
    private static final String L_LLAVE = "\\{";
    private static final String R_LLAVE = "\\}";
    private static final String COMA = ",";
    private static final String DOS_PUNTOS = ":";
    private static final String LITERAL_CADENA = "\".*?\"";  // Cadenas entre comillas dobles
    private static final String LITERAL_NUM = "[0-9]+(\\.[0-9]+)?((e|E)(\\+|-)?[0-9]+)?";  // Números, enteros o con decimales
    private static final String PR_TRUE = "(true|TRUE)";
    private static final String PR_FALSE = "(false|FALSE)";
    private static final String PR_NULL = "(null|NULL)";
    private static final String EOF = "$";  // Token para el fin de archivo

    // Compilamos la expresión regular para los tokens válidos
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
        String.join("|", L_CORCHETE, R_CORCHETE, L_LLAVE, R_LLAVE, COMA, DOS_PUNTOS, LITERAL_CADENA, LITERAL_NUM, PR_TRUE, PR_FALSE, PR_NULL)
    );

    private BufferedReader reader;
    private String line;

    public JsonLexer(Reader input) {
        reader = new BufferedReader(input);
        line = "";
    }

    public String getNextToken() throws IOException {
        if (line == null) {
            return "EOF";
        }
        
        while (line.isEmpty()) {
            line = reader.readLine();
            if (line == null) {
                return "EOF";
            }
        }

        Matcher matcher = TOKEN_PATTERN.matcher(line);
        if (matcher.find()) {
            String token = matcher.group();
            line = line.substring(matcher.end()).trim();
            return getTokenName(token);
        }

        throw new IOException("Token inválido encontrado: " + line);
    }

    // Método para obtener el nombre del componente léxico
    private String getTokenName(String token) {
        if (token.matches(L_CORCHETE)) return "L_CORCHETE";
        if (token.matches(R_CORCHETE)) return "R_CORCHETE";
        if (token.matches(L_LLAVE)) return "L_LLAVE";
        if (token.matches(R_LLAVE)) return "R_LLAVE";
        if (token.matches(COMA)) return "COMA";
        if (token.matches(DOS_PUNTOS)) return "DOS_PUNTOS";
        if (token.matches(LITERAL_CADENA)) return "LITERAL_CADENA";
        if (token.matches(LITERAL_NUM)) return "LITERAL_NUM";
        if (token.matches(PR_TRUE)) return "PR_TRUE";
        if (token.matches(PR_FALSE)) return "PR_FALSE";
        if (token.matches(PR_NULL)) return "PR_NULL";
        return "UNKNOWN";
    }
}


import java.io.IOException;

public class JsonParser {
    private JsonLexer lexer;
    private String currentToken;

    public JsonParser(JsonLexer lexer) throws IOException {
        this.lexer = lexer;
        advance(); // Avanzamos al primer token
    }

    // Avanza al siguiente token
    private void advance() throws IOException {
        currentToken = lexer.getNextToken();
    }

    // Parseo inicial para cualquier entrada JSON
    public void parse() throws IOException {
        parseValue(); // Un JSON válido empieza con un valor
        if (!"EOF".equals(currentToken)) {
            throw new SyntaxException("Extra data after JSON value");
        }
    }

    // Método para analizar un valor JSON (objeto, arreglo, literal)
    private void parseValue() throws IOException {
        switch (currentToken) {
            case "L_LLAVE":
                parseObject();
                break;
            case "L_CORCHETE":
                parseArray();
                break;
            case "LITERAL_CADENA":
            case "LITERAL_NUM":
            case "PR_TRUE":
            case "PR_FALSE":
            case "PR_NULL":
                advance();
                break;
            default:
                throw new SyntaxException("Unexpected token: " + currentToken);
        }
    }

    // Método para analizar un objeto JSON
    private void parseObject() throws IOException {
        advance(); // Avanza desde '{'
        if ("R_LLAVE".equals(currentToken)) {
            advance(); // Objeto vacío '{}'
            return;
        }
        while (true) {
            if (!"LITERAL_CADENA".equals(currentToken)) {
                throw new SyntaxException("Expected key as string");
            }
            advance();
            if (!"DOS_PUNTOS".equals(currentToken)) {
                throw new SyntaxException("Expected ':' after key");
            }
            advance();
            parseValue();
            if ("COMA".equals(currentToken)) {
                advance();
            } else if ("R_LLAVE".equals(currentToken)) {
                advance();
                break;
            } else {
                throw new SyntaxException("Expected ',' or '}' in object");
            }
        }
    }

    // Método para analizar un arreglo JSON
    private void parseArray() throws IOException {
        advance(); // Avanza desde '['
        if ("R_CORCHETE".equals(currentToken)) {
            advance(); // Arreglo vacío '[]'
            return;
        }
        while (true) {
            parseValue();
            if ("COMA".equals(currentToken)) {
                advance();
            } else if ("R_CORCHETE".equals(currentToken)) {
                advance();
                break;
            } else {
                throw new SyntaxException("Expected ',' or ']' in array");
            }
        }
    }

    // Excepción personalizada para errores de sintaxis
    private static class SyntaxException extends RuntimeException {
        public SyntaxException(String message) {
            super("Syntax Error: " + message);
        }
    }
}
