package io.lerk.lrkFM.util;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import io.lerk.lrkFM.R;
import io.lerk.lrkFM.activities.FileActivity;
import io.lerk.lrkFM.entities.FMFile;

import static android.widget.Toast.LENGTH_SHORT;
import static io.lerk.lrkFM.activities.FileActivity.PREF_FILENAME_LENGTH;

public class FileArrayAdapter extends ArrayAdapter<FMFile> {

    private static final int ID_COPY = 0;
    private static final int ID_MOVE = 1;
    private static final int ID_RENAME = 2;
    private static final int ID_DELETE = 3;
    private static final String TAG = FileArrayAdapter.class.getCanonicalName();
    private static final int ID_SHARE = 4;

    private FileActivity activity;

    public FileArrayAdapter(Context context, int resource, List<FMFile> items) {
        super(context, resource, items);
        if (context instanceof FileActivity) {
            this.activity = (FileActivity) context;
        } else {
            Log.d(TAG, "Context is no FileActivity: " + context.getClass().getName());
        }
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return initUI(getItem(position));
    }


    private View initUI(FMFile f) {
        assert activity != null;

        @SuppressLint("InflateParams") View v = LayoutInflater.from(activity).inflate(R.layout.layout_file, null); // works.

        if (f != null) {
            TextView fileNameView = (TextView) v.findViewById(R.id.fileTitle);
            TextView filePermissions = (TextView) v.findViewById(R.id.filePermissions);
            TextView fileDate = (TextView) v.findViewById(R.id.fileDate);
            TextView fileSize = (TextView) v.findViewById(R.id.fileSize);
            ImageView fileImage = (ImageView) v.findViewById(R.id.fileIcon);

            final String fileName = f.getName();
            if (fileNameView != null) {
                int maxLength = Integer.parseInt(activity.getDefaultPreferences().getString(PREF_FILENAME_LENGTH, "27"));
                if (fileName.length() >= maxLength) {
                    @SuppressLint("SetTextI18n") String output = fileName.substring(0, maxLength - 3) + "...";
                    fileNameView.setText(output); //shorten long names
                } else {
                    fileNameView.setText(fileName);
                }
            }
            if (filePermissions != null) {
                filePermissions.setText(f.getPermissions());
            }
            if (fileDate != null) {
                fileDate.setText(f.getLastModified().toString());
            }
            if (fileSize != null) {
                if (f.getDirectory()) {
                    fileSize.setVisibility(View.GONE);
                } else {
                    fileSize.setText(getSizeFormatted(f));
                }
            }
            if (fileImage != null) {
                if (!f.getDirectory()) {
                    fileImage.setImageDrawable(getContext().getDrawable(R.drawable.ic_insert_drive_file_black_24dp));
                }
            }
            if (f.getDirectory()) {
                v.setOnClickListener(v1 -> activity.loadDirectory(f.getFile().getAbsolutePath()));
            } else {
                v.setOnClickListener(v1 -> {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT, Uri.fromFile(f.getFile()));
                    i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    i.setType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(f.getFile().getAbsolutePath())));
                    try {
                        getContext().startActivity(i);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(getContext(), R.string.no_app_to_handle_file, LENGTH_SHORT).show();
                    }
                });
            }
            v.setOnCreateContextMenuListener((menu, view, info) -> {
                initializeContextMenu(f, fileName, menu);
            });

            ImageButton contextButton = (ImageButton) v.findViewById(R.id.contextMenuButton);
            contextButton.setOnClickListener(v1 -> {
                activity.getFileListView().showContextMenuForChild(v);
                Log.d(TAG, "Opening context menu!");
            });

        }
        return v;
    }

    private void initializeContextMenu(FMFile f, String fileName, ContextMenu menu) {
        menu.setHeaderTitle(fileName);
        menu.add(0, ID_COPY, 0, activity.getString(R.string.copy)).setOnMenuItemClickListener(item -> {
            AlertDialog alertDialog = getGenericFileOpDialog(
                    R.string.copy,
                    R.string.op_destination,
                    R.drawable.ic_content_copy_black_24dp,
                    R.layout.layout_path_prompt,
                    (d) -> {
                        Log.d(TAG, "Dismiss called!");
                        EditText editText = (EditText) d.findViewById(R.id.destinationPath);
                        String pathname = editText.getText().toString();
                        if (pathname.isEmpty()) {
                            Toast.makeText(activity, R.string.err_empty_input, LENGTH_SHORT).show();
                        } else {
                            FileUtil.copy(f, activity, new File(pathname));
                        }
                    },
                    (d) -> Log.d(TAG, "Cancelled."));
            alertDialog.setOnShowListener(d -> {
                EditText editText = (EditText) alertDialog.findViewById(R.id.destinationPath);
                if (editText != null) {
                    editText.setText(f.getFile().getAbsolutePath());
                } else {
                    Log.w(TAG, "Unable to find view, can not set file title.");
                }
            });
            alertDialog.show();
            activity.reloadCurrentDirectory();
            return true;
        });
        menu.add(0, ID_MOVE, 0, activity.getString(R.string.move)).setOnMenuItemClickListener(item -> {
            AlertDialog alertDialog = getGenericFileOpDialog(
                    R.string.move,
                    R.string.op_destination,
                    R.drawable.ic_content_cut_black_24dp,
                    R.layout.layout_path_prompt,
                    (d) -> {
                        Log.d(TAG, "Dismiss called!");
                        EditText editText = (EditText) d.findViewById(R.id.destinationPath);
                        String pathname = editText.getText().toString();
                        if (pathname.isEmpty()) {
                            Toast.makeText(activity, R.string.err_empty_input, LENGTH_SHORT).show();
                        } else {
                            FileUtil.move(f, activity, new File(pathname));
                        }
                    },
                    (d) -> Log.d(TAG, "Cancelled."));
            alertDialog.setOnShowListener(d -> {
                EditText editText = (EditText) alertDialog.findViewById(R.id.destinationPath);
                if (editText != null) {
                    editText.setText(f.getFile().getAbsolutePath());
                } else {
                    Log.w(TAG, "Unable to find view, can not set file title.");
                }
            });
            alertDialog.show();
            activity.reloadCurrentDirectory();
            return true;
        });
        menu.add(0, ID_RENAME, 0, activity.getString(R.string.rename)).setOnMenuItemClickListener(item -> {
            AlertDialog alertDialog = getGenericFileOpDialog(
                    R.string.rename,
                    R.string.rename,
                    R.drawable.ic_mode_edit_black_24dp,
                    R.layout.layout_name_prompt,
                    (d) -> {
                        Log.d(TAG, "Dismiss called!");
                        EditText editText = (EditText) d.findViewById(R.id.destinationName);
                        String newName = editText.getText().toString();
                        if (newName.isEmpty()) {
                            Toast.makeText(activity, R.string.err_empty_input, LENGTH_SHORT).show();
                        } else {
                            FileUtil.rename(f, activity, newName);
                        }
                    },
                    (d) -> Log.d(TAG, "Cancelled."));
            alertDialog.setOnShowListener(d -> {
                EditText editText = (EditText) alertDialog.findViewById(R.id.destinationName);
                if (editText != null) {
                    editText.setText(f.getName());
                } else {
                    Log.w(TAG, "Unable to find view, can not set file title.");
                }
            });
            alertDialog.show();
            activity.reloadCurrentDirectory();
            return true;
        });
        menu.add(0, ID_SHARE, 0, activity.getString(R.string.share)).setOnMenuItemClickListener(i->{
            Intent intent = new Intent(Intent.ACTION_SEND);
            // MIME of .apk is "application/vnd.android.package-archive".
            // but Bluetooth does not accept this. Let's use "*/*" instead.
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f.getFile()));
            activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.share_app)));
            return true;
        });
        menu.add(0, ID_DELETE, 0, activity.getString(R.string.delete)).setOnMenuItemClickListener(item -> {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.delete)
                    .setMessage(activity.getString(R.string.warn_delete_msg) + f.getName() + "?")
                    .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel())
                    .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                        if (!FileUtil.deleteNoValidation(f)) {
                            Toast.makeText(activity, R.string.err_deleting_element, LENGTH_SHORT).show();
                        }
                        activity.reloadCurrentDirectory();
                        dialogInterface.dismiss();
                    })
                    .show();
            return true;
        });
    }

    private String getSizeFormatted(FMFile f) {
        String[] units = new String[]{"B", "KiB", "MiB", "GiB", "TiB", "PiB"};
        Long length = f.getFile().length();
        Double number = Math.floor(Math.log(length) / Math.log(1024));
        Double pow = Math.pow(1024, Math.floor(number));
        Double d = length / pow;
        String formattedString = d.toString().substring(0, d.toString().indexOf(".") + 2);
        return formattedString + ' ' + units[number.intValue()];
    }

    private AlertDialog getGenericFileOpDialog(@StringRes int positiveBtnText,
                                               @StringRes int title,
                                               @DrawableRes int icon,
                                               @LayoutRes int view,
                                               ButtonCallBackInterface positiveCallBack,
                                               ButtonCallBackInterface negativeCallBack) {
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setTitle(title)
                .setIcon(icon)
                .setCancelable(true).create();

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, activity.getString(positiveBtnText), (d, i) -> positiveCallBack.handle(dialog));
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, activity.getString(R.string.cancel), (d, i) -> negativeCallBack.handle(dialog));
        dialog.setOnShowListener(dialog1 -> {
            EditText inputField = null;
            if (view == R.layout.layout_name_prompt) {
                inputField = (EditText) dialog.findViewById(R.id.destinationName);
                if (inputField != null) {
                    String name = activity.getTitleFromPath(activity.getCurrentDirectory());
                    inputField.setText(name);
                    Log.d(TAG, "Destination set to: " + name);
                } else {
                    Log.w(TAG, "Unable to preset current name, text field is null!");
                }
            } else if (view == R.layout.layout_path_prompt) {
                inputField = (EditText) dialog.findViewById(R.id.destinationPath);
                if (inputField != null) {
                    String directory = activity.getCurrentDirectory();
                    inputField.setText(directory);
                    Log.d(TAG, "Destination set to: " + directory);
                } else {
                    Log.w(TAG, "Unable to preset current path, text field is null!");
                }
            }
        });
        return dialog;
    }
}
