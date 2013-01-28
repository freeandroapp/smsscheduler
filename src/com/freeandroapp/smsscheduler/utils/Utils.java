package com.freeandroapp.smsscheduler.utils;

import java.util.ArrayList;

public class Utils {
	
	// ------------------- For displaying appropriate number of recipients in
    // sms listing---------------------
    /**
     * @details creates a fixed length string to display in the recipients
     *          section of each child in the ExpandableList. It rounds off the
     *          string to contain only an exact number of recipients.
     * @param number
     *            : String to rectify the length of.
     * @return validLengthNumber.
     */
    public static String numbersLengthRectify(String number) {
        if (number.length() <= 30) {
            return number;
        }
        int validLength = 0;
        for (int i = 0; i < number.length(); i++) {
            if (number.charAt(i) == ' ' && number.charAt(i - 1) == ',') {
                if (i <= 30) {
                    validLength = i;
                }
            }
        }
        String validLengthNumber = number.substring(0, validLength);

        return validLengthNumber;
    }

    /**
     * @details refines a number string. Only allows the characters 0-9.
     * @param number
     *            : string to refine
     * @return refined number.
     */
    public static String refineNumber(String number) {
        if (number.matches("[0-9]+")) {
            return number;
        }
        ArrayList<Character> chars = new ArrayList<Character>();
        for (int i = 0; i < number.length(); i++) {
            chars.add(number.charAt(i));
        }
        for (int i = 0; i < chars.size(); i++) {
        	int a = (chars.get(i) -'0');
        	//Replaced the logic
        	//if (!(chars.get(i) == '0' || chars.get(i) == '1' || chars.get(i) == '2' || chars.get(i) == '3' || chars.get(i) == '4' || chars.get(i) == '5' || chars.get(i) == '6' || chars.get(i) == '7' || chars.get(i) == '8' || chars.get(i) == '9' || chars.get(i) == '+')) {
        	if(!((a <= 9) && (a >= 0))){
                chars.remove(i);
                i--;
            }
        }

        StringBuffer number1 = new StringBuffer();
        for (int i = 0; i < chars.size(); i++) {
            number1.append(chars.get(i));
        }
        return number1.toString();
    }

}
