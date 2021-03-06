package com.stevesoltys.backup.activity.backup;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.RemoteException;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.google.android.collect.Sets;
import com.stevesoltys.backup.R;
import com.stevesoltys.backup.activity.PopupWindowUtil;
import com.stevesoltys.backup.service.PackageService;
import com.stevesoltys.backup.service.backup.BackupService;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Steve Soltys
 */
class CreateBackupActivityController {

    private static final String TAG = CreateBackupActivityController.class.getName();

    private static final Set<String> IGNORED_PACKAGES = Sets.newArraySet(
            "com.android.providers.downloads.ui", "com.android.providers.downloads", "com.android.providers.media",
            "com.android.providers.calendar", "com.android.providers.contacts", "com.stevesoltys.backup"
    );

    private final BackupService backupService = new BackupService();

    private final PackageService packageService = new PackageService();

    void populatePackageList(ListView packageListView, CreateBackupActivity parent) {
        AtomicReference<PopupWindow> popupWindow = new AtomicReference<>();

        parent.runOnUiThread(() -> {
            popupWindow.set(PopupWindowUtil.showLoadingPopupWindow(parent));
            TextView textView = popupWindow.get().getContentView().findViewById(R.id.popup_text_view);
            textView.setText(R.string.loading_packages);

            View popupWindowButton = popupWindow.get().getContentView().findViewById(R.id.popup_cancel_button);
            popupWindowButton.setOnClickListener(view -> parent.finish());
        });

        List<String> eligiblePackageList = new LinkedList<>();

        try {
            eligiblePackageList.addAll(packageService.getEligiblePackages());
            eligiblePackageList.removeAll(IGNORED_PACKAGES);

        } catch (RemoteException e) {
            Log.e(TAG, "Error while obtaining package list: ", e);
        }

        parent.runOnUiThread(() -> {
            if (popupWindow.get() != null) {
                popupWindow.get().dismiss();
            }

            packageListView.setOnItemClickListener(parent);
            packageListView.setAdapter(new ArrayAdapter<>(parent, R.layout.checked_list_item, eligiblePackageList));
            packageListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        });
    }

    void showEnterPasswordAlert(Set<String> selectedPackages, Uri contentUri, Activity parent) {
        final EditText passwordTextView = new EditText(parent);
        passwordTextView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(parent)
                .setTitle("Enter a password")
                .setMessage("You'll need this to restore your backup, so write it down!")
                .setView(passwordTextView)

                .setPositiveButton("Set password", (dialog, button) ->
                        showConfirmPasswordAlert(selectedPackages, contentUri, parent,
                                passwordTextView.getText().toString()))

                .setNegativeButton("Cancel", (dialog, button) -> dialog.cancel())
                .show();
    }

    private void showConfirmPasswordAlert(Set<String> selectedPackages, Uri contentUri, Activity parent,
                                          String originalPassword) {
        final EditText passwordTextView = new EditText(parent);
        passwordTextView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(parent)
                .setTitle("Confirm password")
                .setView(passwordTextView)

                .setPositiveButton("Confirm", (dialog, button) -> {
                    String password = passwordTextView.getText().toString();

                    if (originalPassword.equals(password)) {
                        backupService.backupPackageData(selectedPackages, contentUri, parent, password);

                    } else {
                        new AlertDialog.Builder(parent)
                                .setMessage("Passwords do not match, please try again.")
                                .setPositiveButton("Ok", (dialog2, button2) -> dialog2.dismiss())
                                .show();

                        dialog.cancel();
                    }
                })

                .setNegativeButton("Cancel", (dialog, button) -> dialog.cancel())
                .show();
    }
}
