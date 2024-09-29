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

    // Método para obtener el nombre del componente léxico
    private static String getTokenName(String token) {
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
        return "ERROR_LEXICO";  // Si el token no coincide con ningún patrón
    }

    // Analizador léxico para procesar el JSON de entrada
    public static void analyze(String outputFile) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                // Usamos el Matcher para buscar tokens en la línea
                Matcher matcher = TOKEN_PATTERN.matcher(line);
                StringBuilder result = new StringBuilder();

                int lastEnd = 0;  // Posición del último token reconocido
                boolean errorFlag = false;  // Bandera para registrar si hay errores en la línea

                while (matcher.find()) {
                    // Si hay texto entre el último token válido y el nuevo token, es un error léxico
                    if (matcher.start() != lastEnd) {
                        String errorPart = line.substring(lastEnd, matcher.start()).trim();
                        if (!errorPart.isEmpty()) {
                            writer.write("Error léxico en la línea " + lineNumber + ": '" + errorPart + "'");
                            writer.newLine();
                            errorFlag = true;
                        }
                    }

                    // Procesar el token encontrado
                    String token = matcher.group();
                    result.append(getTokenName(token)).append(" ");
                    lastEnd = matcher.end();
                }

                // Si hay texto al final de la línea que no fue reconocido, también es un error léxico
                if (lastEnd != line.length()) {
                    String errorPart = line.substring(lastEnd).trim();
                    if (!errorPart.isEmpty()) {
                        writer.write("Error léxico en la línea " + lineNumber + ": '" + errorPart + "'");
                        writer.newLine();
                        errorFlag = true;
                    }
                }

                // Escribimos los tokens reconocidos si no hubo errores léxicos
                if (!errorFlag) {
                    writer.write(result.toString().trim());
                    writer.newLine();
                }

                lineNumber++;
            }

            // Al final del archivo, agregamos el token EOF
            writer.write("EOF");
            writer.newLine();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String outputFile = "output.txt";  // Nombre del archivo de salida
        System.out.println("Ingrese el JSON (finalice con Ctrl+D en Linux o Ctrl+Z en Windows):");
        // Ejecutar el análisis léxico
        analyze(outputFile);
        System.out.println("Análisis completado. Resultados guardados en " + outputFile);
    }
}

