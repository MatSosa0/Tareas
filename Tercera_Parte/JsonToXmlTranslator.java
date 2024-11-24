import java.io.*;
import java.util.*;

public class JsonToXmlTranslator {
    private static final Set<String> VALID_TOKENS = Set.of(
        "[", "]", "{", "}", ",", ":", "true", "false", "null"
    );

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Uso: java JsonToXmlTranslator <input.json> <output.xml>");
            return;
        }
        String inputFile = args[0];
        String outputFile = args[1];

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            String jsonContent = reader.lines().reduce("", (acc, line) -> acc + line.trim());
            Parser parser = new Parser(jsonContent);
            String xmlResult = parser.parse();

            if (!parser.hasErrors()) {
                writer.write(xmlResult);
                System.out.println("Traducción exitosa. Archivo generado: " + outputFile);
            } else {
                System.err.println("Errores encontrados durante la traducción:");
                parser.getErrors().forEach(System.err::println);
            }

        } catch (IOException e) {
            System.err.println("Error al manejar archivos: " + e.getMessage());
        }
    }
}

class Parser {
    private final String input;
    private int index = 0;
    private final List<String> errors = new ArrayList<>();

    public Parser(String input) {
        this.input = input;
    }

    public String parse() {
        StringBuilder xml = new StringBuilder();
        try {
            xml.append(parseElement());
            if (index < input.length()) {
                throw new RuntimeException("Contenido inesperado al final del archivo.");
            }
        } catch (RuntimeException e) {
            errors.add("Error en posición " + index + ": " + e.getMessage());
        }
        return xml.toString();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<String> getErrors() {
        return errors;
    }

    private String parseElement() {
        skipWhitespace();
        if (match("{")) {
            return parseObject();
        } else if (match("[")) {
            return parseArray();
        } else if (input.startsWith("\"", index)) {
            return "<string>" + parseString() + "</string>";
        } else if (input.startsWith("true", index) || input.startsWith("false", index)) {
            return "<boolean>" + parseLiteral() + "</boolean>";
        } else if (input.startsWith("null", index)) {
            return "<null/>";
        } else if (Character.isDigit(input.charAt(index)) || input.charAt(index) == '-') {
            return "<number>" + parseNumber() + "</number>";
        } else {
            throw new RuntimeException("Elemento inválido.");
        }
    }

    private String parseObject() {
        StringBuilder xml = new StringBuilder("<object>");
        skipWhitespace();
        if (!match("}")) {
            do {
                skipWhitespace();
                String name = parseString();
                skipWhitespace();
                if (!match(":")) {
                    throw new RuntimeException("Se esperaba ':' después de la clave.");
                }
                xml.append("<").append(name).append(">");
                xml.append(parseElement());
                xml.append("</").append(name).append(">");
                skipWhitespace();
            } while (match(","));
            if (!match("}")) {
                throw new RuntimeException("Se esperaba '}' al final del objeto.");
            }
        }
        xml.append("</object>");
        return xml.toString();
    }

    private String parseArray() {
        StringBuilder xml = new StringBuilder("<array>");
        skipWhitespace();
        if (!match("]")) {
            do {
                xml.append(parseElement());
                skipWhitespace();
            } while (match(","));
            if (!match("]")) {
                throw new RuntimeException("Se esperaba ']' al final del arreglo.");
            }
        }
        xml.append("</array>");
        return xml.toString();
    }

    private String parseString() {
        int start = ++index;
        while (index < input.length() && input.charAt(index) != '"') {
            index++;
        }
        if (index >= input.length()) {
            throw new RuntimeException("Cadena sin cerrar.");
        }
        String result = input.substring(start, index);
        index++; // Saltar cierre de comillas
        return result;
    }

    private String parseLiteral() {
        if (input.startsWith("true", index)) {
            index += 4;
            return "true";
        } else if (input.startsWith("false", index)) {
            index += 5;
            return "false";
        } else if (input.startsWith("null", index)) {
            index += 4;
            return "null";
        } else {
            throw new RuntimeException("Literal inválido.");
        }
    }

    private String parseNumber() {
        int start = index;
        while (index < input.length() && (Character.isDigit(input.charAt(index)) || input.charAt(index) == '.')) {
            index++;
        }
        return input.substring(start, index);
    }

    private boolean match(String expected) {
        if (input.startsWith(expected, index)) {
            index += expected.length();
            return true;
        }
        return false;
    }

    private void skipWhitespace() {
        while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
            index++;
        }
    }
}
