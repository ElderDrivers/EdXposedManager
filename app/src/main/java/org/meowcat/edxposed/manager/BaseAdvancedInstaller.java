package org.meowcat.edxposed.manager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;

import org.meowcat.edxposed.manager.util.NavUtil;
import org.meowcat.edxposed.manager.util.json.XposedTab;
import org.meowcat.edxposed.manager.util.json.XposedZip;

import java.util.List;
import java.util.Objects;

import static org.meowcat.edxposed.manager.XposedApp.WRITE_EXTERNAL_PERMISSION;

public class BaseAdvancedInstaller extends Fragment {

    private View mClickedButton;

    static BaseAdvancedInstaller newInstance(XposedTab tab) {
        BaseAdvancedInstaller myFragment = new BaseAdvancedInstaller();

        Bundle args = new Bundle();
        args.putParcelable("tab", tab);
        myFragment.setArguments(args);

        return myFragment;
    }

    private List<XposedZip> installers() {
        XposedTab tab = requireArguments().getParcelable("tab");
        return Objects.requireNonNull(tab).installers;
    }

    private List<XposedZip> uninstallers() {
        XposedTab tab = requireArguments().getParcelable("tab");
        return Objects.requireNonNull(tab).uninstallers;
    }

    private String notice() {
        XposedTab tab = requireArguments().getParcelable("tab");
        return Objects.requireNonNull(tab).notice;
    }

    protected String author() {
        XposedTab tab = requireArguments().getParcelable("tab");
        return Objects.requireNonNull(tab).author;
    }

    private String supportUrl() {
        XposedTab tab = requireArguments().getParcelable("tab");
        return Objects.requireNonNull(tab).support;
    }

    private boolean isStable() {
        XposedTab tab = requireArguments().getParcelable("tab");
        return Objects.requireNonNull(tab).stable;
    }

    private boolean isOfficial() {
        XposedTab tab = requireArguments().getParcelable("tab");
        return Objects.requireNonNull(tab).official;
    }

    private String description() {
        XposedTab tab = requireArguments().getParcelable("tab");
        return Objects.requireNonNull(tab).description;
    }

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_PERMISSION);
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.single_installer_view, container, false);

        final Spinner chooserInstallers = view.findViewById(R.id.chooser_installers);
        final Spinner chooserUninstallers = view.findViewById(R.id.chooser_uninstallers);
        final Button btnInstall = view.findViewById(R.id.btn_install);
        final Button btnUninstall = view.findViewById(R.id.btn_uninstall);
        final ImageView infoInstaller = view.findViewById(R.id.info_installer);
        final ImageView infoUninstaller = view.findViewById(R.id.info_uninstaller);
        final TextView infoAuthor = view.findViewById(R.id.info_author);
        final View infoHelp = view.findViewById(R.id.info_help);
        final View infoUpdateDescription = view.findViewById(R.id.info_update_description);
        final TextView notice = view.findViewById(R.id.notice);

        try {
            chooserInstallers.setAdapter(new XposedZip.MyAdapter(getContext(), installers()));
            chooserUninstallers.setAdapter(new XposedZip.MyAdapter(getContext(), uninstallers()));
        } catch (Exception ignored) {
        }

        infoInstaller.setOnClickListener(v -> {
            final XposedZip selectedInstaller = (XposedZip) chooserInstallers.getSelectedItem();
            final String s = getString(R.string.infoInstaller,
                    selectedInstaller.name,
                    selectedInstaller.version);

            new MaterialDialog.Builder(requireContext()).title(R.string.info)
                    .content(s).positiveText(R.string.ok).show();
        });
        infoUninstaller.setOnClickListener(v -> {
            final XposedZip selectedUninstaller = (XposedZip) chooserUninstallers.getSelectedItem();
            final String s = getString(R.string.infoUninstaller,
                    selectedUninstaller.name,
                    selectedUninstaller.version);

            new MaterialDialog.Builder(requireContext()).title(R.string.info)
                    .content(s).positiveText(R.string.ok).show();
        });

        btnInstall.setOnClickListener(v -> {
            mClickedButton = v;
            if (checkPermissions()) return;

            BaseFragment.areYouSure(requireActivity(), getString(R.string.warningArchitecture), (d, w) -> {
                XposedZip selectedInstaller = (XposedZip) chooserInstallers.getSelectedItem();
                Uri uri = Uri.parse(selectedInstaller.link);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }, (d, w) -> {
            });
        });

        btnUninstall.setOnClickListener(v -> {
            mClickedButton = v;
            if (checkPermissions()) return;

            BaseFragment.areYouSure(requireActivity(), getString(R.string.warningArchitecture), (d, w) -> {
                XposedZip selectedUninstaller = (XposedZip) chooserUninstallers.getSelectedItem();
                Uri uri = Uri.parse(selectedUninstaller.link);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }, (d, w) -> {
            });
        });

        notice.setText(Html.fromHtml(notice(), Html.FROM_HTML_MODE_COMPACT));
        infoAuthor.setText(getString(R.string.download_committer, author()));

        try {
            if (uninstallers().size() == 0) {
                infoUninstaller.setVisibility(View.GONE);
                chooserUninstallers.setVisibility(View.GONE);
                btnUninstall.setVisibility(View.GONE);
            }
        } catch (Exception ignored) {
        }

        if (!isStable()) {
            view.findViewById(R.id.warning_unstable).setVisibility(View.VISIBLE);
        }

        if (!isOfficial()) {
            view.findViewById(R.id.warning_unofficial).setVisibility(View.VISIBLE);
        }

        infoHelp.setOnClickListener(v -> NavUtil.startURL(getActivity(), supportUrl()));
        infoUpdateDescription.setOnClickListener(v -> new MaterialDialog.Builder(requireContext())
                .title(R.string.changes)
                .content(Html.fromHtml(description(), Html.FROM_HTML_MODE_COMPACT))
                .positiveText(R.string.ok).show());

        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_PERMISSION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mClickedButton != null) {
                    new Handler().postDelayed(() -> mClickedButton.performClick(), 500);
                }
            } else {
                Toast.makeText(getActivity(), R.string.permissionNotGranted, Toast.LENGTH_LONG).show();
            }
        }
    }

}