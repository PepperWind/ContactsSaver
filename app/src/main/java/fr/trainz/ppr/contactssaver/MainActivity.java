package fr.trainz.ppr.contactssaver;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Toast;

import java.util.ArrayList;

import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity {

    private enum ReadingType {
        BEGIN, NAME, PHONE, EMAIL
    }

    private ProgressDialog pDialog;
    private Handler updateBarHandler;
    StringBuilder builder;
    EditText editText;
    Cursor cursor;
    int counter;

    private String decryptKey;
    private String cryptoSeed;

    private SecretKey key;

    private Button buttonLaunch;

    private RadioButton radioButtonLoad;
    private RadioButton radioButtonSave;

    private String memorizedLoadText;
    private String memorizedSaveText;

    private boolean cryptoOk;

    // Choose dialog

    private CheckBox checkBoxEverything;
    private Button buttonChooseOk;
    private ListView listViewChoose;

    private FArrayAdapter adapterChoose;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = (EditText) findViewById(R.id.ETX);
        updateBarHandler =new Handler();
        // Since reading contacts takes more time, let's run it on a separate thread.

        memorizedLoadText = "";
        memorizedSaveText = "";

        buttonLaunch = (Button) findViewById(R.id.BT_LAUNCH);
        buttonLaunch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cryptoOk = false;

                if (radioButtonSave.isChecked()) {
                    pDialog = new ProgressDialog(MainActivity.this);
                    pDialog.setMessage("Reading contacts...");
                    pDialog.setCancelable(false);
                    pDialog.show();

                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            getContacts();

                            while (!cryptoOk) {
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    sendEmail();
                                }
                            });
                        }
                    });
                    thread.start();

                    launchEncryptDialog();
                }

                else {
                    if(editText.getText().toString().equals("")) {
                        Toast.makeText(MainActivity.this, getString(R.string.CLEAR_DATA_EMPTY), Toast.LENGTH_LONG).show();
                    }
                    else {
                        decryptKey = "";
                        launchDecryptDialog();

                    }
                }
            }
            });

        radioButtonLoad = (RadioButton) findViewById(R.id.RBT_LOAD);
        radioButtonSave = (RadioButton) findViewById(R.id.RBT_SAVE);

        radioButtonLoad.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    editText.setHint(getString(R.string.EDT_HINT_TEXT));
                    editText.setEnabled(true);
                    memorizedSaveText = editText.getText().toString();
                    editText.setText(memorizedLoadText);
                }
                else {
                    editText.setHint("");
                    editText.setEnabled(false);
                    memorizedLoadText = editText.getText().toString();
                    editText.setText(memorizedSaveText);
                }
            }
        });

    }
    public void getContacts() {
        builder = new StringBuilder();
        String phoneNumber = null;
        String email = null;
        Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
        String _ID = ContactsContract.Contacts._ID;
        String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
        String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;
        Uri PhoneCONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String Phone_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
        String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;
        Uri EmailCONTENT_URI =  ContactsContract.CommonDataKinds.Email.CONTENT_URI;
        String EmailCONTACT_ID = ContactsContract.CommonDataKinds.Email.CONTACT_ID;
        String DATA = ContactsContract.CommonDataKinds.Email.DATA;
        ContentResolver contentResolver = getContentResolver();
        cursor = contentResolver.query(CONTENT_URI, null,null, null, null);
        // Iterate every contact in the phone
        if (cursor.getCount() > 0) {
            counter = 0;
            while (cursor.moveToNext()) {
                // Update the progress message
                updateBarHandler.post(new Runnable() {
                    public void run() {
                        pDialog.setMessage("Reading contacts : "+ counter++ +"/"+cursor.getCount());
                    }
                });
                String contact_id = cursor.getString(cursor.getColumnIndex( _ID ));
                String name = cursor.getString(cursor.getColumnIndex( DISPLAY_NAME ));
                int hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndex( HAS_PHONE_NUMBER )));
                if (hasPhoneNumber > 0) {
                    builder.append("[N").append(name).append("]");
                    //This is to read multiple phone numbers associated with the same contact
                    Cursor phoneCursor = contentResolver.query(PhoneCONTENT_URI, null, Phone_CONTACT_ID + " = ?", new String[] { contact_id }, null);
                    if (phoneCursor != null) {
                        while (phoneCursor.moveToNext()) {
                            phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(NUMBER));
                            builder.append("[P").append(phoneNumber).append("]");
                        }
                        phoneCursor.close();
                    }// Read every email id associated with the contact
                    Cursor emailCursor = contentResolver.query(EmailCONTENT_URI,    null, EmailCONTACT_ID+ " = ?", new String[] { contact_id }, null);
                    if (emailCursor != null) {
                        while (emailCursor.moveToNext()) {
                            email = emailCursor.getString(emailCursor.getColumnIndex(DATA));
                            builder.append("[E").append(email).append("]");
                        }
                        emailCursor.close();
                    }
                }
            }
            updateBarHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        pDialog.cancel();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 500);
        }
    }

    private void launchEncryptDialog() {
        final AlertDialog cryptoDialog = new AlertDialog.Builder(MainActivity.this).create();
        cryptoDialog.setTitle(getString(R.string.ENCRYPT_DIALOG_TITLE));
        cryptoDialog.setMessage(getString(R.string.ENCRYPT_DIALOG_TEXT));

        final EditText input = new EditText(MainActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        cryptoDialog.setView(input);
        cryptoDialog.setCancelable(false);

        cryptoDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ENCRYPT_DIALOG_OK),
                new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, int which) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                cryptoSeed = input.getText().toString();
                                cryptoDialog.dismiss();
                                cryptoOk = true;
                            }
                        });
                    }
                });

        cryptoDialog.show();
    }

    private void launchDecryptDialog() {
        final AlertDialog cryptoDialog = new AlertDialog.Builder(MainActivity.this).create();
        cryptoDialog.setTitle(getString(R.string.DECRYPT_DIALOG_TITLE));
        cryptoDialog.setMessage(getString(R.string.DECRYPT_DIALOG_TEXT));

        final EditText input = new EditText(MainActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        cryptoDialog.setView(input);
        cryptoDialog.setCancelable(false);

        cryptoDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.DECRYPT_DIALOG_OK),
                new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, int which) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                decryptKey = input.getText().toString();
                                cryptoDialog.dismiss();
                                launchContactChoiceDialog();
                            }
                        });
                    }
                });

        cryptoDialog.show();
    }

    private void launchContactChoiceDialog() {
        ArrayList<Contact> contactsCandidats = new ArrayList<>();

        try {
            if(decryptKey.equals(""))
                contactsCandidats = decodeContacts(editText.getText().toString());
            else {
                key = AESHelper.generateKey(decryptKey);
                contactsCandidats = decodeContacts(AESHelper.decryptMsg(AESHelper.hexStringToByteArray(editText.getText().toString()), key));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(contactsCandidats.size()==0) {
            Toast.makeText(MainActivity.this, getString(R.string.CLEAR_DATA_EMPTY), Toast.LENGTH_LONG).show();
            return;
        }

        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.dialog_contact_select);
        dialog.setTitle(getString(R.string.CHOOSE_DIALOG_TITLE));
        dialog.setCancelable(false);

        listViewChoose = (ListView) dialog.findViewById(R.id.LVW_SELECT_CONTACTS);

        adapterChoose = new FArrayAdapter(MainActivity.this, contactNames(contactsCandidats));
        listViewChoose.setAdapter(adapterChoose);

        checkBoxEverything = (CheckBox) dialog.findViewById(R.id.CBX_SELECT_ALL_CONTACTS);
        checkBoxEverything.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                adapterChoose.checkEverything(isChecked);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapterChoose.notifyDataSetChanged();
                    }
                });
            }
        });

        buttonChooseOk = (Button) dialog.findViewById(R.id.BT_CHOOSE_OK);
        final ArrayList<Contact> finalContactsCandidats = contactsCandidats;
        buttonChooseOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();

                if(finalContactsCandidats.size()==0)
                    return;

                boolean[] checks = adapterChoose.getChecks();

                for(int i = 0; i < finalContactsCandidats.size(); i++) {
                    if(checks[i]) {
                        Intent intent = new Intent(Intent.ACTION_INSERT);
                        intent.setType(ContactsContract.Contacts.CONTENT_TYPE);

                        intent.putExtra(ContactsContract.Intents.Insert.NAME, finalContactsCandidats.get(i).getName());

                        if(finalContactsCandidats.get(i).getNumbers().size() == 1)
                            intent.putExtra(ContactsContract.Intents.Insert.PHONE, finalContactsCandidats.get(i).getNumbers().get(0));
                        if(finalContactsCandidats.get(i).getNumbers().size() == 2) {
                            intent.putExtra(ContactsContract.Intents.Insert.PHONE, finalContactsCandidats.get(i).getNumbers().get(0));
                            intent.putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, finalContactsCandidats.get(i).getNumbers().get(1));
                        }
                        if(finalContactsCandidats.get(i).getNumbers().size() == 3) {
                            intent.putExtra(ContactsContract.Intents.Insert.PHONE, finalContactsCandidats.get(i).getNumbers().get(0));
                            intent.putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, finalContactsCandidats.get(i).getNumbers().get(1));
                            intent.putExtra(ContactsContract.Intents.Insert.TERTIARY_PHONE, finalContactsCandidats.get(i).getNumbers().get(2));
                        }


                        if(finalContactsCandidats.get(i).getEmails().size() == 1)
                            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, finalContactsCandidats.get(i).getEmails().get(0));
                        if(finalContactsCandidats.get(i).getEmails().size() == 2) {
                            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, finalContactsCandidats.get(i).getEmails().get(0));
                            intent.putExtra(ContactsContract.Intents.Insert.SECONDARY_EMAIL, finalContactsCandidats.get(i).getEmails().get(1));
                        }
                        if(finalContactsCandidats.get(i).getEmails().size() == 3) {
                            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, finalContactsCandidats.get(i).getEmails().get(0));
                            intent.putExtra(ContactsContract.Intents.Insert.SECONDARY_EMAIL, finalContactsCandidats.get(i).getEmails().get(1));
                            intent.putExtra(ContactsContract.Intents.Insert.TERTIARY_EMAIL, finalContactsCandidats.get(i).getEmails().get(2));
                        }

                        int PICK_CONTACT = 100+i;
                        MainActivity.this.startActivityForResult(intent, PICK_CONTACT);
                    }
                }
            }
        });

        dialog.show();
    }

    public void sendEmail() {
        radioButtonSave.setChecked(true);

        try {
            if (cryptoSeed.equals(""))
                editText.setText(builder.toString());
            else {
                key = AESHelper.generateKey(cryptoSeed);
                editText.setText(AESHelper.bytesToHex(AESHelper.encryptMsg(builder.toString(), key)));
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, getString(R.string.CRYPTO_ERROR_TOAST), Toast.LENGTH_LONG).show();
            editText.setText(builder.toString());
            e.printStackTrace();
        }

        editText.setText(editText.getText().toString().replaceAll("[|]", "|"));

        final Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_TEXT, editText.getText().toString());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            final android.content.ClipboardManager clipboardManager = (android.content.ClipboardManager) MainActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
            final android.content.ClipData clipData = android.content.ClipData
                    .newPlainText("contacts", editText.getText().toString());
            clipboardManager.setPrimaryClip(clipData);
        } else {
            final android.text.ClipboardManager clipboardManager = (android.text.ClipboardManager)MainActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboardManager.setText(editText.getText().toString());
        }

        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle(getString(R.string.READY_TO_SEND_TITLE));
        alertDialog.setMessage(getString(R.string.READY_TO_SEND_TEXT));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            startActivity(Intent.createChooser(i, "Send mail..."));
                        } catch (android.content.ActivityNotFoundException ex) {
                            Toast.makeText(MainActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                        }
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    public ArrayList<Contact> decodeContacts(String clearData) {
        if(clearData.equals("")) {
            Toast.makeText(MainActivity.this, getString(R.string.CLEAR_DATA_EMPTY), Toast.LENGTH_LONG).show();
            return new ArrayList<>();
        }

        ArrayList<Contact> contacts = new ArrayList<>();

        ReadingType rType = ReadingType.BEGIN;
        StringBuilder decodeBuilder = new StringBuilder();

        try {

            for (Character c : clearData.toCharArray()) {
                switch (rType) {
                    case BEGIN:
                        if(decodeBuilder.length()==0) {
                            if (c != '[') {
                                throw new InconformException();
                            }
                            decodeBuilder.append('[');
                        }
                        else {
                            switch (c) {
                                case 'N':
                                    contacts.add(new Contact());
                                    rType = ReadingType.NAME;
                                    decodeBuilder = new StringBuilder();
                                    break;
                                case 'P':
                                    rType = ReadingType.PHONE;
                                    decodeBuilder = new StringBuilder();
                                    break;
                                case 'E':
                                    rType = ReadingType.EMAIL;
                                    decodeBuilder = new StringBuilder();
                                    break;
                                default:
                                    Toast.makeText(MainActivity.this, "Ni N, ni P, ni E", Toast.LENGTH_LONG).show();

                                    throw new InconformException();
                            }
                        }
                        break;
                    default:
                        if(c == ']') {
                            switch (rType) {
                                case NAME:
                                    contacts.get(contacts.size()-1).setName(decodeBuilder.toString());
                                    break;
                                case PHONE:
                                    contacts.get(contacts.size()-1).addNumber(decodeBuilder.toString());
                                    break;
                                case EMAIL:
                                    contacts.get(contacts.size()-1).addEmail(decodeBuilder.toString());
                                    break;
                                default:
                                    throw new InconformException();
                            }
                            decodeBuilder = new StringBuilder();
                            rType = ReadingType.BEGIN;
                        }
                        else
                            decodeBuilder.append(c);

                }
            }

            return contacts;
        } catch (InconformException e) {
            Toast.makeText(MainActivity.this, getString(R.string.CLEAR_DATA_INCONFORM), Toast.LENGTH_LONG).show();
            return new ArrayList<>();
        }
    }

    public String[] contactNames(ArrayList<Contact> contacts) {
        String[] names = new String[contacts.size()];

        if(contacts.size()==0)
            return names;

        for(int i = 0; i < contacts.size(); i++) {
            names[i] = contacts.get(i).getName();
        }

        return names;
    }
}