package org.obolibrary.robot;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Convenience methods for working with command line options.
 */
public class CommandLineHelper {
    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(CommandLineHelper.class);

    /**
     * Given a single string, return a list of strings split at whitespace
     * but allowing for quoted values, as a command-line parser does.
     *
     * @param toProcess the string to parse
     * @return a string list, split at whitespace, with quotation marks removed
     * @throws Exception on parsing problems
     */
    public static List<String> parseArgList(String toProcess) throws Exception {
        return new ArrayList<String>(Arrays.asList(parseArgs(toProcess)));
    }

    /**
     * Given a single string, return an array of strings split at whitespace
     * but allowing for quoted values, as a command-line parser does.
     * Adapted from org.apache.tools.ant.types.Commandline
     *
     * @param toProcess the string to parse
     * @return a string array, split at whitespace, with quotation marks removed
     * @throws Exception on parsing problems
     */
    public static String[] parseArgs(String toProcess) throws Exception {
        if (toProcess == null || toProcess.length() == 0) {
            return new String[0];
        }

        // parse with a simple finite state machine
        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        int state = normal;
        StringTokenizer tok = new StringTokenizer(toProcess, "\"\' ", true);
        Vector v = new Vector();
        StringBuffer current = new StringBuffer();
        boolean lastTokenHasBeenQuoted = false;

        while (tok.hasMoreTokens()) {
            String nextTok = tok.nextToken();
            switch (state) {
                case inQuote:
                    if ("\'".equals(nextTok)) {
                        lastTokenHasBeenQuoted = true;
                        state = normal;
                    } else {
                        current.append(nextTok);
                    }
                    break;
                case inDoubleQuote:
                    if ("\"".equals(nextTok)) {
                        lastTokenHasBeenQuoted = true;
                        state = normal;
                    } else {
                        current.append(nextTok);
                    }
                    break;
                default:
                    if ("\'".equals(nextTok)) {
                        state = inQuote;
                    } else if ("\"".equals(nextTok)) {
                        state = inDoubleQuote;
                    } else if (" ".equals(nextTok)) {
                        if (lastTokenHasBeenQuoted || current.length() != 0) {
                            v.addElement(current.toString());
                            current = new StringBuffer();
                        }
                    } else {
                        current.append(nextTok);
                    }
                    lastTokenHasBeenQuoted = false;
                    break;
            }
        }

        if (lastTokenHasBeenQuoted || current.length() != 0) {
            v.addElement(current.toString());
        }

        if (state == inQuote || state == inDoubleQuote) {
            throw new Exception("unbalanced quotes in " + toProcess);
        }

        String[] args = new String[v.size()];
        v.copyInto(args);
        return args;
    }

    /**
     * Given a command line and an index, return the argument at that index.
     *
     * @param line the command line to use
     * @param index the index of the argument
     * @return the argument at the given index
     */
    public static String getIndexValue(CommandLine line, int index) {
        String result = null;
        List<String> arguments = Arrays.asList(line.getArgs());
        if (arguments.size() > index) {
            result = arguments.get(index);
        }
        return result;
    }

    /**
     * Given a command line, return its first argument,
     * which should be the name of the command to execute.
     *
     * @param line the command line to use
     * @return the name of the command to execute
     */
    public static String getCommand(CommandLine line) {
        return getIndexValue(line, 0);
    }

    /**
     * Return true if a command line include the option with the given name,
     * or the command with the given name.
     *
     * @param line the command line to use
     * @param name the name of the flag or argument to check for
     * @return true if a flag or first argument match the given name,
     *     false otherwise
     */
    public static boolean hasFlagOrCommand(CommandLine line, String name) {
        if (line.hasOption(name)) {
            return true;
        }
        String command = getCommand(line);
        if (command != null && command.equals(name)) {
            return true;
        }
        return false;
    }

    /**
     * Get the value of the command-line option with the given name.
     *
     * @param line the command line to use
     * @param name the name of the option to find
     * @return the option value as a string, or null if not found
     */
    public static String getOptionalValue(CommandLine line, String name) {
        return getDefaultValue(line, name, null);
    }

    /**
     * Get the value of the command-line options with the given name.
     *
     * @param line the command line to use
     * @param name the name of the option to find
     * @return the option value as a list of strings, maybe empty
     */
    public static List<String> getOptionalValues(CommandLine line,
            String name) {
        if (line.hasOption(name)) {
            return new ArrayList<String>(
                    Arrays.asList(line.getOptionValues(name)));
        } else {
            return new ArrayList<String>();
        }
    }

    /**
     * Get the value of the command-line option with the given name,
     * or return a default value if the option is not found.
     *
     * @param line the command line to use
     * @param name the name of the option to find
     * @param defaultValue the default value to use
     * @return the option value as a string, or the default
     *     if the option not found
     */
    public static String getDefaultValue(CommandLine line, String name,
            String defaultValue) {
        String result = defaultValue;
        if (line.hasOption(name)) {
            result = line.getOptionValue(name);
        }
        return result;
    }

    /**
     * Get the value of the command-line option with the given name,
     * or throw an IllegalArgumentException with a given message
     * if the option is not found.
     *
     * @param line the command line to use
     * @param name the name of the option to find
     * @param message the message for the exception
     * @return the option value as a string
     * @throws IllegalArgumentException if the option is not found
     */
    public static String getRequiredValue(CommandLine line, String name,
            String message) throws IllegalArgumentException {
        String result = getOptionalValue(line, name);
        if (result == null) {
            throw new IllegalArgumentException(message);
        }
        return result;
    }

    /**
     * Given a command line, return an initialized IOHelper.
     * The --prefix, --prefixes, and --noprefixes options are handled.
     *
     * @param line the command line to use
     * @return an initialized IOHelper
     */
    public static IOHelper getIOHelper(CommandLine line) {
        IOHelper ioHelper;
        String prefixes = getOptionalValue(line, "prefixes");
        if (prefixes != null) {
            ioHelper = new IOHelper(prefixes);
        } else {
            ioHelper = new IOHelper(!line.hasOption("noprefixes"));
        }

        for (String prefix: getOptionalValues(line, "prefix")) {
            ioHelper.addPrefix(prefix);
        }

        return ioHelper;
    }

    /**
     * Given an IOHelper and a command line, check for required options
     * and return a loaded input ontology.
     * Currently handles --input options.
     *
     * @param ioHelper the IOHelper to load the ontology with
     * @param line the command line to use
     * @return the input ontology
     * @throws IllegalArgumentException if requires options are missing
     * @throws IOException if the ontology cannot be loaded
     */
    public static OWLOntology getInputOntology(IOHelper ioHelper,
            CommandLine line) throws IllegalArgumentException, IOException {
        OWLOntology inputOntology = null;
        String inputOntologyPath = getOptionalValue(line, "input");
        if (inputOntologyPath != null) {
            inputOntology = ioHelper.loadOntology(inputOntologyPath);
        } else {
            throw new IllegalArgumentException(
                    "inputOntology must be specified");
        }
        return inputOntology;
    }

    /**
     * Given an IOHelper and a command line, check for required options
     * and return a list of loaded input ontologies.
     * Currently handles --input options.
     *
     * @param ioHelper the IOHelper to load the ontology with
     * @param line the command line to use
     * @return the list of input ontologies
     * @throws IllegalArgumentException if requires options are missing
     * @throws IOException if the ontology cannot be loaded
     */
    public static List<OWLOntology> getInputOntologies(IOHelper ioHelper,
            CommandLine line) throws IllegalArgumentException, IOException {
        List<OWLOntology> inputOntologies = new ArrayList<OWLOntology>();
        List<String> inputOntologyPaths = getOptionalValues(line, "input");
        for (String inputOntologyPath: inputOntologyPaths) {
            inputOntologies.add(ioHelper.loadOntology(inputOntologyPath));
        }
        return inputOntologies;
    }

    /**
     * Given a command line, check for the required options and return a File
     * for saving data.
     *
     * @param line the command line to use
     * @return the File for output; may be null; may not exist!
     * @throws IllegalArgumentException if required options are not found
     */
    public static File getOutputFile(CommandLine line)
            throws IllegalArgumentException {
        String outputPath = getOptionalValue(line, "output");
        File outputFile = null;
        if (outputPath != null) {
            outputFile = new File(outputPath);
        }
        return outputFile;
    }

    /**
     * Given a command line, check for the required options and return an IRI
     * to be used as the OntologyIRI for the output ontology.
     *
     * @param line the command line to use
     * @return the IRI for the output ontology, or null
     */
    public static IRI getOutputIRI(CommandLine line) {
        String outputIRIString = getOptionalValue(line, "output-iri");
        IRI outputIRI = null;
        if (outputIRIString != null) {
            outputIRI = IRI.create(outputIRIString);
        }
        return outputIRI;
    }

    /**
     * Given a command line and an ontology,
     * for each `--output` option (if any),
     * save a copy of the ontology to the specified path.
     *
     * @param line the command lien to use
     * @param ontology the ontology to save
     * @throws IOException on any problem
     */
    public static void maybeSaveOutput(CommandLine line, OWLOntology ontology)
            throws IOException {
        IOHelper ioHelper = getIOHelper(line);
        for (String path: line.getOptionValues("output")) {
            ioHelper.saveOntology(ontology, path);
        }
    }

    /**
     * Given an IOHelper and a command line, check for the required options
     * and return a set of IRIs for terms.
     * Handles --terms string and --term-file file options.
     *
     * @param ioHelper the IOHelper to use for loading the terms
     * @param line the command line to use
     * @return a set of term IRIs
     * @throws IllegalArgumentException if the required options are not found
     * @throws IOException if the term file cannot be loaded
     */
    public static Set<IRI> getTerms(IOHelper ioHelper, CommandLine line)
        throws IllegalArgumentException, IOException {
        Set<IRI> terms = null;
        if (line.hasOption("terms")) {
            terms = ioHelper.parseTerms(line.getOptionValue("terms"));
        } else if (line.hasOption("term-file")) {
            terms = ioHelper.loadTerms(line.getOptionValue("term-file"));
        }
        if (terms == null) {
            throw new IllegalArgumentException(
                    "The terms to extract must be specified.");
        }
        return terms;
    }

    /**
     * Print a help message for a command.
     *
     * @param usage the usage information for the command
     * @param options the command line options for the command
     */
    public static void printHelp(String usage, Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(usage, options);
    }

    /**
     * Print the ROBOT version.
     */
    public static void printVersion() {
        System.out.println("TODO: command version 0.0.1");
    }

    /**
     * Create a new Options object with shared options for 'help' and 'version'.
     *
     * @return a new Options object with some options added
     */
    public static Options getCommonOptions() {
        Options o = new Options();
        o.addOption("h", "help",    false, "print usage information");
        o.addOption("v", "version", false, "print version information");
        o.addOption("p", "prefix",  true,  "add a prefix 'foo: http://bar'");
        o.addOption("P", "prefixes", true, "use prefixes from JSON-LD file");
        o.addOption("noprefixes", false, "do not use default prefixes");
        return o;
    }

    /**
     * Parse the command line, handle help and other common options,
     * (May exit!) and return a CommandLine.
     *
     * @param usage the usage string for this command
     * @param options the command-line options for this command
     * @param args the command-line arguments provided
     * @return a new CommandLine object
     * @throws ParseException if the arguments cannot be parsed
     */
    public static CommandLine getCommandLine(String usage, Options options,
            String[] args) throws ParseException {
        CommandLineParser parser = new PosixParser();
        CommandLine line = parser.parse(options, args);

        if (hasFlagOrCommand(line, "help")) {
            printHelp(usage, options);
            System.exit(0);
        }
        if (hasFlagOrCommand(line, "version")) {
            printVersion();
            System.exit(0);
        }

        return line;
    }

    /**
     * Shared method for dealing with exceptions, printing help, and exiting.
     * Currently prints the stack trace, then the help, then exits.
     *
     * @param usage the usage string for this command
     * @param options the command-line options for this command
     * @param exception the exception to handle
     */
    public static void handleException(String usage, Options options,
            Exception exception) {
        exception.printStackTrace();
        printHelp(usage, options);
        System.exit(1);
    }
}
