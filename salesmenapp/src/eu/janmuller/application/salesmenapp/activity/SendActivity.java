package eu.janmuller.application.salesmenapp.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;
import com.google.inject.Inject;
import eu.janmuller.application.salesmenapp.Helper;
import eu.janmuller.application.salesmenapp.R;
import eu.janmuller.application.salesmenapp.adapter.DocumentAdapter;
import eu.janmuller.application.salesmenapp.model.db.*;
import eu.janmuller.application.salesmenapp.server.ConnectionException;
import eu.janmuller.application.salesmenapp.server.ServerService;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import roboguice.util.Ln;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Coder: Jan Müller
 * Date: 18.10.13
 * Time: 14:55
 */
@ContentView(R.layout.send_activity)
public class SendActivity extends BaseActivity {

    @InjectView(R.id.email)
    private EditText mEmailAddress;

    @InjectView(R.id.subject)
    private EditText mSubject;

    @InjectView(R.id.body)
    private EditText mBody;

    @InjectView(R.id.grid)
    private GridView mGridLayout;

    @InjectView(R.id.button_hide_all)
    private Button mButtonHideAll;

    @Inject
    private ServerService mServerService;

    private List<Document> mDocuments;
    private Inquiry        mInquiry;
    private DocumentAdapter mDocumentAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setHomeButtonEnabled(true);

        Intent intent = getIntent();
        mInquiry = (Inquiry) intent.getSerializableExtra(ViewActivity.INQUIRY);

        if (mInquiry == null) {

            finish();
            return;
        }
        if (mInquiry.mail != null) {
            mEmailAddress.setText(mInquiry.mail);
        }
        if (mInquiry.company != null) {
            setSubject(mInquiry);
        }
        mDocuments = mInquiry.getDocumentsByInquiry();
        mDocumentAdapter = new DocumentAdapter(this);
        mDocumentAdapter.setEditMode(true);
        mGridLayout.setAdapter(mDocumentAdapter);
        mDocumentAdapter.addAll(mDocuments);
        if (mDocuments.size() > 0) {
            setGridViewHeightBasedOnChildren(mGridLayout, 4);
        }
        lookForPostponedMessage();

        mButtonHideAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideAll();
            }
        });
        hideAll();
    }

    public void setGridViewHeightBasedOnChildren(GridView gridView, int columns) {
        DocumentAdapter listAdapter = (DocumentAdapter)gridView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        int items = listAdapter.getCount();
        int rows = 0;

        View listItem = listAdapter.getView(0, null, gridView);
        listItem.measure(0, 0);
        totalHeight = listItem.getMeasuredHeight();

        float x = 1;
        if( items > columns ){
            x = items/columns;
            rows = (int) (x + 1);
            totalHeight *= rows;
        }

        ViewGroup.LayoutParams params = gridView.getLayoutParams();
        params.height = totalHeight;
        gridView.setLayoutParams(params);
    }

    private void hideAll() {
        mDocumentAdapter.setAllObjectsVisibility(false);
    }

    private void setSubject(Inquiry inquiry) {
        mSubject.setText(String.format("Nabídka %s pro %s", getString(R.string.app_name), inquiry.company));
    }

    private void lookForPostponedMessage() {

        List<SendQueue> sendQueues = SendQueue.getByQuery(SendQueue.class, "inquiryServerId='" + mInquiry.serverId + "'");
        if (sendQueues.size() > 0) {

            final SendQueue sendQueue = sendQueues.get(0);
            AlertDialog.Builder builder = new AlertDialog.Builder(SendActivity.this);
            builder.setNegativeButton("Vytvořit novou", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    sendQueue.delete();
                }
            });
            builder.setPositiveButton("Odeslat", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    Inquiry inquiry = Inquiry.getByServerId(sendQueue.inquiryServerId);
                    send(inquiry, sendQueue.mail,
                            sendQueue.title,
                            sendQueue.text,
                            sendQueue.json, new ISendMessageCallback() {

                        @Override
                        public void onSentSuccess() {

                            sendQueue.delete();
                        }

                        @Override
                        public void onSentFail() {

                            finish();
                        }
                    });
                }
            });
            builder.setTitle("Neodeslaná zpráva");
            builder.setMessage("Při posledním odeslání nebyla zpráva odeslána. Chcete tuto zprávu odeslat nyní?");
            builder.create().show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.send_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case android.R.id.home:

                finish();
                break;
            case R.id.menu_send:

                sendMessage();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    private void sendMessage() {

        if (!validateMessage()) {

            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(SendActivity.this);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                send(mInquiry, mEmailAddress.getText().toString(),
                        mSubject.getText().toString(),
                        mBody.getText().toString(),
                        mDocuments, null);
            }
        });
        List<String> attachments = new ArrayList<String>();
        for (Document document : mDocuments) {
            if (document.show) {
                attachments.add(document.getTitle());
            }
        }
        String text = TextUtils.join(", ", attachments);

        builder.setTitle(getString(R.string.send_message_title));
        builder.setMessage(getString(R.string.send_message_body) + text);
        builder.create().show();
    }

    private void send(final Inquiry inquiry,
                      final String email,
                      final String subject,
                      final String body,
                      final Object object,
                      final ISendMessageCallback sendMessageCallback) {

        final ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.sending));
        final Handler handler = new Handler();
        new Thread() {

            @Override
            public void run() {

                try {

                    boolean failedSend = false;
                    // odeslu zpravu
                    if (object instanceof List) {

                        try {
                            // tato metoda reseni frontovani pri neuspesnem pokusu o odeslani
                            mServerService.send(inquiry, email, subject, body, (List) object);
                        } catch (ConnectionException e) {
                            failedSend = true;
                            Ln.w(String.format(getString(R.string.message_sends_failed_putting_to_queue),
                                    e.getMessage()));
                        }
                    } else if (object instanceof String) {

                        mServerService.send(inquiry, email, subject, body, (String) object);
                    }

                    // pokud nedoslo k chybe, zobrazim followup dialog
                    final boolean _failedSend = failedSend;
                    handler.post(new Runnable() {

                        @Override
                        public void run() {

                            progressDialog.dismiss();

                            if (sendMessageCallback != null) {

                                sendMessageCallback.onSentSuccess();
                            }

                            // followup zobrazime jen pokud vendor ma poptavky a zaroven se nejedna o docasnou poptavku
                            if (!mInquiry.temporary && getResources().getBoolean(R.bool.has_inquiries)) {

                                showFollowUpDialog(_failedSend);
                            } else {

                                if (_failedSend) {

                                    Toast.makeText(SendActivity.this, "Zpráva bude odeslána po připojení k internetu.", Toast.LENGTH_SHORT).show();
                                }
                                finish();
                            }
                        }
                    });
                } catch (final ConnectionException e) {

                    // v pripade chyby zobrazim chybovou hlasku
                    handler.post(new Runnable() {

                        @Override
                        public void run() {

                            progressDialog.dismiss();
                            Ln.w(String.format("Při odesílání zprávy došlo k chybě [%s]", e.getMessage()));
                            Toast.makeText(SendActivity.this, "Zpráva bude odeslána po připojení k internetu.", Toast.LENGTH_SHORT).show();
                            if (sendMessageCallback != null) {

                                sendMessageCallback.onSentFail();
                            }
                            finish();
                        }
                    });
                }
            }
        }.start();
    }

    private interface ISendMessageCallback {

        public void onSentSuccess();

        public void onSentFail();
    }

    // vztazene k followup dialogu
    private DatePicker mDatePicker;
    private EditText   mEditText;

    /**
     * Zobrazeni followup dialogu
     */
    private void showFollowUpDialog(final boolean failedSave) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View view = getLayoutInflater().inflate(R.layout.followup_dialog, null);
        builder.setView(view);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                // pokud kliknu na zrusit followup dialog pak ukoncim sendactivitu
                finish();
            }
        });
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                // nactu datum z datepickeru
                Date date = new Date(mDatePicker.getCalendarView().getDate());

                // pokud selhalo odeslani zpravy, pak zafrontujeme postpone
                if (failedSave) {

                    FollowUpQueue.push(mInquiry.serverId, mEditText.getText().toString(), date);
                    Toast.makeText(SendActivity.this, "Zpráva bude odeslána po připojení k internetu.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {

                    sendFollowUpRequest(mInquiry, date, mEditText.getText().toString());
                }
            }
        });
        builder.setTitle("Nastavení připomenutí");
        AlertDialog alertDialog = builder.create();

        mDatePicker = (DatePicker) view.findViewById(R.id.datePicker);
        mEditText = (EditText) view.findViewById(R.id.text);
        alertDialog.show();
    }

    /**
     * Poslani follow up requestu
     */
    private void sendFollowUpRequest(final Inquiry inquiry, final Date date, final String message) {

        final Handler handler = new Handler();
        new Thread() {

            @Override
            public void run() {

                String strDate = Helper.sSdf.format(date);
                try {

                    // odeslu followup na server
                    mServerService.followUp(inquiry, strDate, message);

                    // zobrazim hlasku o uspechu
                    handler.post(new Runnable() {

                        @Override
                        public void run() {

                            Toast.makeText(SendActivity.this, "Zpráva byla odeslána", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                } catch (final ConnectionException e) {

                    // v pripade chyby zobrazim chybovou hlasku
                    handler.post(new Runnable() {

                        @Override
                        public void run() {

                            Toast.makeText(SendActivity.this, "Odeslání připomenutí se nezdařilo [" + e.getMessage() + "]", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }.start();

    }

    /**
     * Validace vstupniho formulare
     * Validujeme email a predmet. V pripade problemu, zobrazime toast + requestujeme focus na konkretni edittext
     *
     * @return true = vsechno OK
     */
    private boolean validateMessage() {

        if (!Patterns.EMAIL_ADDRESS.matcher(mEmailAddress.getText().toString()).find()) {

            Toast.makeText(SendActivity.this, "Zadejte validní email", Toast.LENGTH_SHORT).show();
            mEmailAddress.requestFocus();
            return false;
        }

        if (mSubject.getText().toString().length() == 0) {

            Toast.makeText(SendActivity.this, "Zadejte předmět", Toast.LENGTH_SHORT).show();
            mSubject.requestFocus();
            return false;
        }

        return true;
    }
}
