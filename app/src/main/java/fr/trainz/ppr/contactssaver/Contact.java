package fr.trainz.ppr.contactssaver;

import java.util.ArrayList;

/**
 * Created by PPR on 11/02/2017.
 */

public class Contact {
    private String name;
    private ArrayList<String> numbers;
    private ArrayList<String> emails;
    public Contact() {
        name = new String();
        numbers = new ArrayList<>();
        emails = new ArrayList<>();
    }

    public void setName(String pName){
        name = pName;
    }

    public String getName() {
        return name;
    }

    public void addEmail(String pEmail) {
        emails.add(pEmail);
    }

    public void addNumber(String pPhone) {
        numbers.add(pPhone);
    }

    public ArrayList<String> getEmails() {
        return emails;
    }

    public ArrayList<String> getNumbers() {
        return numbers;
    }
}
