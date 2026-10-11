package io.github.cynagenautes.pipepipe.mspgothic;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

/**
 * Renders every PipePipe bullet comment with MS PGothic, whatever font is selected in the app.
 *
 * <p>PipePipe creates one {@link TextView} per comment and adds it to the
 * {@code bulletCommentsContainer} inside {@code BulletCommentsView}. The module hooks the
 * framework's {@link ViewGroup#addView(View, int, ViewGroup.LayoutParams)} and swaps the typeface
 * of those views, so it does not depend on PipePipe's private methods.</p>
 */
public class MSPGothicModule extends XposedModule {
    private static final String TAG = "PipePipeMSPGothic";
    private static final String TARGET_PACKAGE = "InfinityLoop1309.NewPipeEnhanced";
    private static final String BULLET_COMMENTS_VIEW = "org.schabi.newpipe.views.BulletCommentsView";
    private static final String FONT_ASSET = "ms_pgothic.ttf";
    /** TextView -> bulletCommentsContainer -> layout root -> BulletCommentsView. */
    private static final int MAX_ANCESTOR_DEPTH = 3;

    private volatile Typeface font;
    private volatile boolean fontLoadFailed;

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        log(Log.INFO, TAG, "Loaded in " + param.getProcessName() + " on " + getFrameworkName()
                + " " + getFrameworkVersion() + " (API " + getApiVersion() + ")");
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!TARGET_PACKAGE.equals(param.getPackageName()) || !param.isFirstPackage()) {
            return;
        }
        try {
            final Method addView = ViewGroup.class.getDeclaredMethod("addView",
                    View.class, int.class, ViewGroup.LayoutParams.class);
            hook(addView).intercept(this::onAddView);
        } catch (NoSuchMethodException e) {
            log(Log.ERROR, TAG, "ViewGroup.addView not found", e);
        }
    }

    private Object onAddView(XposedInterface.Chain chain) throws Throwable {
        if (chain.getArg(0) instanceof TextView textView
                && chain.getThisObject() instanceof ViewGroup parent
                && isInsideBulletCommentsView(parent)) {
            final Typeface typeface = getFont(parent.getContext());
            if (typeface != null) {
                // Keep the style PipePipe applied (italic for super chats). On Android 9+ PipePipe
                // passes Typeface.BOLD (1) as a weight, so its comments end up non-bold there.
                final Typeface current = textView.getTypeface();
                textView.setTypeface(typeface, current != null ? current.getStyle() : Typeface.NORMAL);
            }
        }
        return chain.proceed();
    }

    private static boolean isInsideBulletCommentsView(ViewGroup parent) {
        ViewParent view = parent;
        for (int i = 0; i < MAX_ANCESTOR_DEPTH && view != null; i++) {
            if (BULLET_COMMENTS_VIEW.equals(view.getClass().getName())) {
                return true;
            }
            view = view.getParent();
        }
        return false;
    }

    private Typeface getFont(Context context) {
        if (font != null || fontLoadFailed) {
            return font;
        }
        synchronized (this) {
            if (font == null && !fontLoadFailed) {
                try {
                    font = loadFont(context);
                    log(Log.INFO, TAG, "MS PGothic loaded");
                } catch (Exception e) {
                    fontLoadFailed = true;
                    log(Log.ERROR, TAG, "Failed to load MS PGothic", e);
                }
            }
        }
        return font;
    }

    private Typeface loadFont(Context context) throws IOException {
        final ApplicationInfo moduleInfo = getModuleApplicationInfo();
        try {
            // Installed APKs are world-readable, so the module's assets can be opened directly.
            final Resources resources =
                    context.getPackageManager().getResourcesForApplication(moduleInfo);
            return Typeface.createFromAsset(resources.getAssets(), FONT_ASSET);
        } catch (Exception e) {
            log(Log.WARN, TAG, "Cannot open module assets, extracting the font instead", e);
            return extractFont(context, moduleInfo);
        }
    }

    private static Typeface extractFont(Context context, ApplicationInfo moduleInfo)
            throws IOException {
        final File apk = new File(moduleInfo.publicSourceDir);
        // Name the copy after the APK timestamp so a module update replaces it.
        final File out = new File(context.getCacheDir(),
                "mspgothic-" + apk.lastModified() + ".ttf");
        if (!out.exists()) {
            final File[] stale = context.getCacheDir().listFiles(
                    (dir, name) -> name.startsWith("mspgothic-"));
            if (stale != null) {
                for (final File file : stale) {
                    file.delete();
                }
            }
            final File tmp = new File(out.getPath() + ".tmp");
            try (ZipFile zip = new ZipFile(apk)) {
                final ZipEntry entry = zip.getEntry("assets/" + FONT_ASSET);
                if (entry == null) {
                    throw new IOException(FONT_ASSET + " not found in " + apk);
                }
                try (InputStream in = zip.getInputStream(entry)) {
                    Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            Files.move(tmp.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return Typeface.createFromFile(out);
    }
}
