package info.shibafu528.shindanaccelerator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.widget.EditText;

import java.io.IOException;

import info.shibafu528.shindan4j.ShindanMaker;
import info.shibafu528.shindan4j.ShindanResult;

public class ShindanActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            Intent intent = getIntent();
            int id = Integer.valueOf(intent.getData().getLastPathSegment());
            if (intent.getBooleanExtra("grant_name", false)) {
                String name = intent.getStringExtra("user_name");
                ShindanProgressDialogFragment fragment = ShindanProgressDialogFragment.newInstance(id, name);
                fragment.show(getSupportFragmentManager(), "shindan");
            } else {
                NameInputDialogFragment fragment = NameInputDialogFragment.newInstance(id);
                fragment.show(getSupportFragmentManager(), "input");
            }
        }
    }

    public static class NameInputDialogFragment extends DialogFragment {
        private SharedPreferences sp;

        public static NameInputDialogFragment newInstance(int id) {
            NameInputDialogFragment fragment = new NameInputDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final EditText v = new EditText(getActivity());
            v.setText(sp.getString("latest", ""));
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle("Input Name")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                            int id = getArguments().getInt("id");
                            ShindanProgressDialogFragment fragment = ShindanProgressDialogFragment.newInstance(id, v.getText().toString());
                            fragment.show(getFragmentManager(), "shindan");
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                            getActivity().finish();
                        }
                    })
                    .setView(v);
            return builder.create();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            getActivity().finish();
        }
    }

    public static class ShindanProgressDialogFragment extends DialogFragment {
        private AsyncTask<Void, Void, ShindanResult> task;

        public static ShindanProgressDialogFragment newInstance(int id, String name) {
            ShindanProgressDialogFragment fragment = new ShindanProgressDialogFragment();
            Bundle args = new Bundle();
            args.putString("name", name);
            args.putInt("id", id);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onResume() {
            super.onResume();
            if (task == null) {
                task = new AsyncTask<Void, Void, ShindanResult>() {
                    @Override
                    protected ShindanResult doInBackground(Void... params) {
                        Bundle args = getArguments();
                        try {
                            return ShindanMaker.getShindan(args.getInt("id")).shindan(args.getString("name"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(ShindanResult shindanResult) {
                        super.onPostExecute(shindanResult);
                        task = null;
                        if (shindanResult != null && !isCancelled()) {
                            Uri.Builder builder = Uri.parse("https://twitter.com/intent/tweet").buildUpon();
                            builder.appendQueryParameter("text", shindanResult.getShareResult());
                            Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
                            intent.addCategory(Intent.CATEGORY_DEFAULT);
                            intent.setPackage("shibafu.yukari");
                            try {
                                startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                intent = new Intent(Intent.ACTION_VIEW, builder.build());
                                startActivity(intent);
                            }
                        }
                        dismiss();
                        getActivity().finish();
                    }
                };
                task.execute();
            }
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return ProgressDialog.show(
                    getActivity(),
                    null,
                    "Wait a moment...",
                    true);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            if (task != null) {
                task.cancel(true);
            }
            getActivity().finish();
        }
    }
}
