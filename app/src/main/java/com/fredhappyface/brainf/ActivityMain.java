package com.fredhappyface.brainf;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

/*
Developed by Kieran W on the 01/02/2019
*/
/*
 * The aim of this android app is to parse a file
 * and to produce an interpreter for the 'Brainfuck' programming language
 */

public class ActivityMain extends ActivityAbstract {

    // Errors
    private static final String ERR_FILE_NOT_LOADED = "ERROR: Load a file before running";
    private static final String ERR_FILE_IS_INVALID = "ERROR: The specified file " +
            "does not exist (filename: %s)";
    private static final String ERR_FILE_IS_NULL = "ERROR: The specified file does " +
            "not contain any text (filename: %s)";
    private static final String ERR_POINTER_LT_ZERO = "ERROR: The pointer cannot be " +
            "less than zero (instruction: %d, %s)";
    private static final String ERR_POINTER_GT_MAX = "ERROR: The pointer cannot be "
            + "greater than the size of the array (array_size: %d instruction: %d, %s)";
    private static final String ERR_VALUE_LT_MIN = "ERROR: The value cannot be "
            + "less than the size of the minimum integer (instruction: %d, %s " +
            "pointer: %d)";
    private static final String ERR_VALUE_GT_MAX = "ERROR: The value cannot be "
            + "greater than the size of the maximum integer (instruction: %d, %s " +
            "pointer: %d)";
    private static final String ERR_EXCEEDED_INPUT = "ERROR: This program requests " +
            "too much input from the user (instruction: %d, %s pointer: %d limit: %d)";
    private static final String ERR_INPUT_REQUIRED = "ERROR: Input is required here";
    private static final String ERR_INPUT_INVALID = "ERROR: Input is too short or in the incorrect format: " +
            "must be a comma separated list or a string of characters";

    // Information
    private static final String INFO_EXECUTION_COMPLETE = "INFO: Code executed " +
            "successfully";

    // Constants
    private static final int MAX_SIZE = 30000;
    private static final int MAX_INPUT = 20;
    private static final Locale locale = Locale.ENGLISH;

    private static final int READ_REQUEST_CODE = 42;

    /*
    Create the activity and apply the activity_main view
     */
    @Override
    protected final void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /*
    Create the overflow menu
     */
    @Override
    public final boolean onCreateOptionsMenu(final @NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /*
    What to do when an option has been selected
     */
    @Override
    public final boolean onOptionsItemSelected(final @NonNull MenuItem item) {
        final int itemId = item.getItemId();

        /*
        For the about field
         */
        if (itemId == R.id.action_about) {
            startActivity(new Intent(this, ActivityAbout.class));
            return true;
        }
        if (itemId == R.id.action_settings) {
            startActivity(new Intent(this, ActivitySettings.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String fileContent;



    /**
     * Fires an intent to spin up the "file chooser" UI and select an file.
     */
    public final void performFileSearch(@Nullable final View view) {
        // Open a file with the system's file browser
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        // Filter to only show results that can be "opened"
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        // Start this activity in the hope of a result
        startActivityForResult(intent, READ_REQUEST_CODE);
    }


    /**
     * This is run (hopefully) when a file has been selected
     */
    @Override
    public final void onActivityResult(final int requestCode, final int resultCode,
                                       final @Nullable Intent resultData) {

        /* Check if the file was picked successfully and that this result is from the expected activity
         */
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Check that there is result data and get the URI
            if (resultData != null) {
                final Uri uri = resultData.getData();
                String uriString = "";
                // Attempt to convert the URI to a string
                if (uri != null) {
                    uriString = uri.toString();
                }
                // Attempt to read the data from the file
                try {
                    fileContent = readTextFromUri(uri);
                } catch (final Exception e) {
                    final String error = String.format(ERR_FILE_IS_INVALID, uriString);
                    reportError(error);
                }
                if (fileContent == null || fileContent.isEmpty()) {
                    final String error = String.format(ERR_FILE_IS_NULL, uriString);
                    reportError(error);
                }
                // Populate the textview with the file contents
                final TextView output = findViewById(R.id.fileContents);
                output.setText(fileContent);
            }
        }
    }

    /*
    Report and error with a toast notification, and log it to the console
     */
    private void reportError(final String error) {
        // System.out.println(error);
        Toast.makeText(getApplicationContext(), error, Toast.LENGTH_LONG).show();
    }

    /*
    Read the file text from the URI
     */
    private String readTextFromUri(final Uri uri) throws IOException {
        final InputStream inputStream = getContentResolver().openInputStream(uri);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        final StringBuilder stringBuilder = new StringBuilder();
        // Read the next line from the file and add it to the output string
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    /*
    Run the interpreter
     */
    public final void run(final @NonNull View view) {
        // Check that the file has been loaded first
        if (fileContent == null) {
            reportError(ERR_FILE_NOT_LOADED);
        } else {
            brainfuckInterpreter(syntaxCleaner(fileContent));
        }
    }

    /*
     * The purpose of this function is to strip non brainfuck syntax, this is to
     * make the program more modular. The syntax is "< > + - . , [ ]"
     */
    private String syntaxCleaner(final String fileContents) {
        // Define variables
        final int fileContentsLen = fileContents.length();
        final char[] legalChars = {'<', '>', '+', '-', '.', ',', '[', ']'};
        final StringBuilder cleanSyntax = new StringBuilder();
        // For every character in the input string...
        for (int syntaxPointer = 0; syntaxPointer < fileContentsLen;
             syntaxPointer++) {
            // ...Select the char at the position of the pointer
            final char element = fileContents.charAt(syntaxPointer);
            // ...And compare it to each char in the legalChars array
            for (final char legalChar: legalChars) {
                if (element == legalChar) {
                    // Add the char to the output if it is part of the syntax
                    cleanSyntax.append(element);
                }
            }
        }
        return cleanSyntax.toString();
    }

    /*
     * The purpose of this function is to take the cleaned syntax and execute
     * the appropriate function based on this
     */
    private void brainfuckInterpreter(final String instruction) {
        // Define variables
        final int[] array = new int[MAX_SIZE];
        int arrayPointer = 0;
        int instructionPointer = 0;
        final int instructionLen = instruction.length();
        int inputCounter = 0;
        final StringBuilder outputBuffer = new StringBuilder();

        // Get the mode
        final RadioButton modeRad = findViewById(R.id.modeAscii);
        final boolean isAsciiMode = modeRad.isChecked();

        // While still reading instructions
        while (instructionPointer < instructionLen) {
            char currentInstruction = instruction.charAt(instructionPointer);
            final int value = array[arrayPointer];

            // Define < operator
            if (currentInstruction == '<') {
                if (arrayPointer != 0) {
                    arrayPointer--;
                } else {
                    final String error = String.format(locale, ERR_POINTER_LT_ZERO,
                            instructionPointer, currentInstruction);
                    reportError(error);
                    return;
                }
            }

            // Define > operator
            if (currentInstruction == '>') {
                if (arrayPointer < MAX_SIZE) {
                    arrayPointer++;
                } else {
                    final String error = String.format(locale, ERR_POINTER_GT_MAX, MAX_SIZE,
                            instructionPointer, currentInstruction);
                    reportError(error);
                    return;
                }
            }

            // Define - operator
            if (currentInstruction == '-') {
                if (value > Integer.MIN_VALUE) {
                    array[arrayPointer]--;
                } else {
                    final String error = String.format(locale, ERR_VALUE_LT_MIN,
                            instructionPointer, currentInstruction, arrayPointer);
                    reportError(error);
                    return;
                }
            }

            // Define + operator
            if (currentInstruction == '+') {
                if (value < Integer.MAX_VALUE) {
                    array[arrayPointer]++;
                } else {
                    final String error = String.format(locale, ERR_VALUE_GT_MAX,
                            instructionPointer, currentInstruction, arrayPointer);
                    reportError(error);
                    return;
                }
            }

            // Define . operator
            if (currentInstruction == '.') {
                if (isAsciiMode) {
                    outputBuffer.append((char) value);
                } else {
                    outputBuffer.append(value);
                    outputBuffer.append(", ");
                }
            }

            // Define , operator
            if (currentInstruction == ',') {
                // Get the input
                final EditText input = findViewById(R.id.input_text_edit);
                String inputText = input.getText().toString();
                // Flag for invalid input
                boolean invalidInput = false;

                if (!inputText.isEmpty()) {
                    if (isAsciiMode) {
                        try {
                            array[arrayPointer] = inputText.charAt(inputCounter);
                        } catch (final Exception e) {
                            invalidInput = true;
                        }
                    } else {
                        inputText = inputText.replaceAll("\\s", "");
                        if (inputText.contains(",")) {
                            final String[] intParts = inputText.split(",");
                            try {
                                array[arrayPointer] = Integer.parseInt(intParts[inputCounter]);
                            } catch (final Exception e) {
                                invalidInput = true;
                            }
                        }
                        try {
                            array[arrayPointer] = Integer.parseInt(inputText);
                        } catch (final Exception e) {
                            invalidInput = true;
                        }
                    }

                    if (invalidInput) {
                        reportError(ERR_INPUT_INVALID);
                        return;
                    }
                    inputCounter++;
                } else {
                    reportError(ERR_INPUT_REQUIRED);
                    return;
                }

                // Terminate if input is called too many times
                if (inputCounter >= MAX_INPUT) {
                    final String error = String.format(locale, ERR_EXCEEDED_INPUT,
                            instructionPointer, currentInstruction, arrayPointer, MAX_INPUT);
                    reportError(error);
                    return;
                }
            }

            // Define [ operator
            // Need to find the matching closing bracket
            if (currentInstruction == '[') {
                if (value == 0) {
                    int brackets = 0;
                    while (true) {
                        // Decrement the pointer and refresh the current instruction
                        instructionPointer++;
                        currentInstruction = instruction.charAt(instructionPointer);
                        // Another opening bracket is encountered
                        if (currentInstruction == '[') {
                            brackets++;
                        }
                        // A closing bracket is encountered
                        else if (currentInstruction == ']') {
                            // If this is the matching bracket
                            if (brackets == 0) {
                                break;
                            } else {
                                brackets--;
                            }
                        }
                    }
                }
            }

            // Define ] operator
            // Need to find the matching opening bracket
            if (currentInstruction == ']') {
                if (value > 0) {
                    int brackets = 0;
                    while (true) {
                        // Decrement the pointer and refresh the current instruction
                        instructionPointer--;
                        currentInstruction = instruction.charAt(instructionPointer);
                        // Another closing bracket is encountered
                        if (currentInstruction == ']') {
                            brackets++;
                        }
                        // An opening bracket is encountered
                        else if (currentInstruction == '[') {
                            // If this is the matching bracket
                            if (brackets == 0) {
                                break;
                            } else {
                                brackets--;
                            }
                        }
                    }
                }
            }

            // Increment the instruction
            instructionPointer++;
        }

        // Inform the user that code execution is complete
        final String success = INFO_EXECUTION_COMPLETE;
        reportError(success);
        outputBuffer.append(success);

        // Populate the textview with the string
        final TextView output = findViewById(R.id.output);
        output.setText(outputBuffer.toString());
    }


}