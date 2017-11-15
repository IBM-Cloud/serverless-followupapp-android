package serverlessfollowup.app;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public abstract class DoWithProgress extends AsyncTask<Void, Void, Void> {

  private static final String TAG = "DoWithProgress";
  private Context mContext;
  private Dialog dialog;

  public DoWithProgress(Context context) {
    mContext = context;
  }


  @Override
  protected void onPreExecute() {
    super.onPreExecute();
    dialog = ProgressDialog.show(mContext, "Processing...", "Please wait...");
  }

  @Override
  protected void onPostExecute(Void aVoid) {
    super.onPostExecute(aVoid);
    dialog.dismiss();
  }
}