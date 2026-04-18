package top.pigimag.pingeo.util;

public class ColoredTextPrinter {

    // ANSI escape codes for colors
    public static final String RESET = "\033[0m";
    public static final String BLACK = "\033[30m";
    public static final String RED = "\033[31m";
    public static final String GREEN = "\033[32m";
    public static final String YELLOW = "\033[33m";
    public static final String BLUE = "\033[34m";
    public static final String MAGENTA = "\033[35m";
    public static final String CYAN = "\033[36m";
    public static final String WHITE = "\033[37m";

    /**
     * Prints the given text in the specified color.
     * @param text the text to print
     * @param color the ANSI color code
     */
    public static void printColored(String text, String color) {
        System.out.println(color + text + RESET);
    }

    /**
     * Prints the given text in red.
     * @param text the text to print
     */
    public static void printRed(String text) {
        printColored(text, RED);
    }

    /**
     * Prints the given text in green.
     * @param text the text to print
     */
    public static void printGreen(String text) {
        printColored(text, GREEN);
    }

    /**
     * Prints the given text in yellow.
     * @param text the text to print
     */
    public static void printYellow(String text) {
        printColored(text, YELLOW);
    }

    /**
     * Prints the given text in blue.
     * @param text the text to print
     */
    public static void printBlue(String text) {
        printColored(text, BLUE);
    }

    private ColoredTextPrinter(){
        throw new Error();
    }
}