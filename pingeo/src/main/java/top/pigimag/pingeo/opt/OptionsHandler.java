package top.pigimag.pingeo.opt;

import org.apache.commons.cli.*;

import top.pigimag.pingeo.Main;
import java.util.Scanner;
import java.util.ResourceBundle;
import java.util.Locale;

public class OptionsHandler {
    private String[] args;
    private ResourceBundle messages;
    
    public OptionsHandler(String[] args){
        this.args = args;
        // Default to Chinese, fallback to English
        Locale locale = Locale.getDefault();
        if (locale.getLanguage().equals("zh")) {
            this.messages = ResourceBundle.getBundle("messages", Locale.CHINA);
        } else {
            this.messages = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        }
    }

    public void ParseIt(){
        Options options = new Options();
        options.addOption(new Option("a","about",false,messages.getString("about")));
        options.addOption(new Option("v", "version",false,messages.getString("version")));
        options.addOption(new Option("je","javaedition",false,messages.getString("je")));
        options.addOption(new Option("be", "bedrockedition", false, messages.getString("be")));
        CommandLineParser clp = new DefaultParser();
        try {
            CommandLine cl = clp.parse(options, args);
            if (cl.hasOption("a")) {
                about();
            }
            if (cl.hasOption("v")) {
                version();
            }
            String[] remaining = cl.getArgs();
            String host;
            int port;
            @SuppressWarnings("resource")
            Scanner scanner = new Scanner(System.in);
            if (remaining.length >= 2) {
                host = remaining[0];
                try {
                    port = Integer.parseInt(remaining[1]);
                } catch (NumberFormatException e) {
                    System.err.println(messages.getString("invalidPort"));
                    System.exit(1);
                    return;
                }
            } else {
                System.out.print(messages.getString("enterHost"));
                host = scanner.nextLine();
                System.out.print(messages.getString("enterPort"));
                try {
                    port = Integer.parseInt(scanner.nextLine());
                } catch (NumberFormatException e) {
                    System.err.println(messages.getString("invalidPort"));
                    System.exit(1);
                    return;
                }
            }
            if (cl.hasOption("je")) {
                Main.pingJE(host, port);
            } else if (cl.hasOption("be")) {
                Main.pingBE(host, port);
            } else {
                System.out.print(messages.getString("specifyType"));
                String type = scanner.nextLine().toLowerCase();
                if ("je".equals(type)) {
                    Main.pingJE(host, port);
                } else if ("be".equals(type)) {
                    Main.pingBE(host, port);
                } else {
                    System.err.println(messages.getString("invalidType"));
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }
    private static void about(){
        System.out.println("--------------------");//my coding is bad...
        System.out.println("Version:\t"+Main.getVersion());
        System.out.println("Author:\t\t"+"Usccc");
        System.out.println("--------------------");
        System.exit(0);
    }
    private static void version(){
        System.out.println(Main.getVersion());
        System.exit(0);
    }
}
